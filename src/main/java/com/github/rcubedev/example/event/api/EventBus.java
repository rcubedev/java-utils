package com.github.rcubedev.example.event.api;

import com.github.rcubedev.example.event.impl.ArrayBackedEventHandler;
import com.github.rcubedev.example.event.impl.EventBusRegistry;
import com.github.rcubedev.example.event.impl.EventHandlerInheritanceRegistry;
import com.github.rcubedev.example.event.impl.EventSubscriberHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Concrete implementation of {@link IEventBus}.
 * <p>
 * Stores one {@link ArrayBackedEventHandler} per {@code (eventType, priority)} pair.
 * Each handler holds its own listeners merged into a single invoker.
 * <p>
 * At registration time, {@link #rebuild()} reads all handlers' {@code eventType},
 * {@code priority}, and {@code invoker} to produce an immutable flat {@link DispatchTable}.
 * Dispatch performs one {@code isInstance} check per processor in the flat array —
 * when it returns {@code false}, the rest of the family is skipped.
 * <p>
 * Extend to create a named singleton bus:
 * <pre>
 * {@code
 * public abstract class CustomEvent extends Event {}
 *
 * public final class CustomEventBus extends EventBus<CustomEvent> {
 *     public static final CustomEventBus INSTANCE = new CustomEventBus();
 *     private CustomEventBus() { super(CustomEvent.class); }
 * }
 * }
 * </pre>
 *
 * @param <B> The base event type this bus accepts
 */
// todo optimize posting by merging all handlers for event type into one EventProcessor to remove inst checks
public abstract class EventBus<B extends Event> implements IEventBus<B> {

    private final Class<B> busType;

    // One ArrayBackedEventHandler[] per eventType, indexed by Priority.ordinal()
    // Only accessed inside rebuildLock
    private final Map<Class<? extends B>, ArrayBackedEventHandler<?>[]> handlers = new HashMap<>();

    // Children per parent — array grown on each new child, only written/read inside rebuildLock
    private final Map<Class<? extends B>, Class<? extends B>[]> childrenMap = new HashMap<>();

    // Flat dispatch array and family metadata; rebuilt at registration, read-only at dispatch
    private volatile DispatchTable dispatchTable = DispatchTable.EMPTY;

    private final Object rebuildLock = new Object();

    protected EventBus(Class<B> busType) {
        this.busType = busType;
        EventBusRegistry.register(this);
    }

    @Override
    public Class<B> getBusType() {
        return busType;
    }

    @Override
    public <E extends B> void post(E event) {
        dispatchTable.dispatch(event);
    }

    @Override
    public <E extends B> void register(Class<E> eventType, EventProcessor<E> listener) {
        register(eventType, Priority.NORMAL, listener);
    }

    @Override
    public <E extends B> void register(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
        synchronized (rebuildLock) {
            getOrCreateHandler(eventType, priority).addListener(listener);
            rebuild();
        }
    }

    @Override
    public void register(Object target) {
        synchronized (rebuildLock) {
            EventSubscriberHandler.register(this, target);
            rebuild();
        }
    }

    @Override
    public void resetListeners() {
        synchronized (rebuildLock) {
            for (ArrayBackedEventHandler<?>[] priorityHandlers : handlers.values()) {
                for (ArrayBackedEventHandler<?> handler : priorityHandlers) {
                    if (handler != null) handler.clear();
                }
            }
            dispatchTable = DispatchTable.EMPTY;
        }
    }

    /**
     * Internal. Post without compile-time check. Used by {@link EventBusRegistry#dispatch(Event)}.
     * Only fires if the event is an instance of this bus's base type.
     */
    @ApiStatus.Internal
    public final void postUnchecked(Event event) {
        if (!busType.isInstance(event)) return;
        dispatchTable.dispatch(event);
    }

    /**
     * Register a processor directly without triggering a rebuild.
     * Used by {@link EventSubscriberHandler} to batch multiple
     * {@link SubscribeEvent @SubscribeEvent} registrations before a single rebuild.
     * Must be called inside {@code rebuildLock}.
     */
    @ApiStatus.Internal
    public <E extends B> void registerDirect(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
        getOrCreateHandler(eventType, priority).addListener(listener);
    }

    @SuppressWarnings("unchecked")
    private <E extends B> ArrayBackedEventHandler<E> getOrCreateHandler(Class<E> eventType, Priority priority) {
        ArrayBackedEventHandler<?>[] priorityHandlers = handlers.get(eventType);
        if (priorityHandlers == null) {
            priorityHandlers = new ArrayBackedEventHandler[Priority.values().length];
            handlers.put(eventType, priorityHandlers);
            trackType(eventType);
        }
        int ordinal = priority.ordinal();
        if (priorityHandlers[ordinal] == null) {
            priorityHandlers[ordinal] = new ArrayBackedEventHandler<>(eventType, priority);
        }
        return (ArrayBackedEventHandler<E>) priorityHandlers[ordinal];
    }

    /**
     * Record parent→child relationship for family building.
     * Called the first time an event type is seen. Must be called inside {@code rebuildLock}.
     */
    @SuppressWarnings("unchecked")
    private void trackType(Class<? extends B> eventType) {
        Class<? extends B> parent = getRegisteredParent(eventType);
        if (parent != null) {
            Class<? extends B>[] current = childrenMap.get(parent);
            Class<? extends B>[] next;
            if (current != null) next = Arrays.copyOf(current, current.length + 1);
            else next = new Class[1];
            next[next.length - 1] = eventType;
            childrenMap.put(parent, next);
        }
    }

    /** Find the nearest registered ancestor of {@code type} on this bus. */
    @SuppressWarnings("unchecked")
    private Class<? extends B> getRegisteredParent(Class<? extends B> type) {
        Class<? extends Event>[] hierarchy = EventHandlerInheritanceRegistry.getEventHierarchy(type);
        for (int i = 1; i < hierarchy.length; i++) {
            Class<? extends Event> ancestor = hierarchy[i];
            if (busType.isAssignableFrom(ancestor) && handlers.containsKey(ancestor)) {
                return (Class<? extends B>) ancestor;
            }
        }
        return null;
    }

    /**
     * Rebuild the flat dispatch array from all stored {@link ArrayBackedEventHandler}s.
     * Re adds each handler's {@link ArrayBackedEventHandler#eventType()},
     * {@link ArrayBackedEventHandler#priority()}, and {@link ArrayBackedEventHandler#invoker()}.
     * Called after every registration. Must be called inside {@code rebuildLock}.
     */
    private void rebuild() {
        if (handlers.isEmpty()) {
            dispatchTable = DispatchTable.EMPTY;
            return;
        }

        // Sort all registered event types shallowest first (superclass before subclass)
        List<Class<? extends B>> allTypes = new ArrayList<>(handlers.keySet());
        allTypes.sort(Comparator.comparingInt(this::hierarchyDepth));
        // System.out.println("bbb" +  allTypes + "\n\n");

        // Build families — linear chains split at branch points
        List<List<Class<? extends B>>> families = buildFamilies(allTypes);

        // Flatten into 1D dispatch array — priority-first, superclass -> subclass within family
        // Parallel array of event types for per-element isInstance check at dispatch
        List<EventProcessor<?>> flatProcessors = new ArrayList<>();

        record Pair<A, B>(A a, B b) {}
        List<Pair<Class<?>, Priority>> flatChecking = new ArrayList<>();

        // List<Class<?>> flatTypes = new ArrayList<>();
        // int[] familyOffsets = new int[families.size()];
        // int[] familyLengths = new int[families.size()];
        //
        // Priority[] priorities = Priority.values();
        //
        // for (int f = 0; f < families.size(); f++) {
        //     familyOffsets[f] = flatProcessors.size();
        //     List<Class<? extends B>> family = families.get(f);
        //
        //     // Interleave by priority: for each priority, emit handlers in family (super→sub) order
        //     // for (Class<? extends B> type : family) {
        //     // for (Priority priority : priorities) {
        //     for (Priority priority : priorities) {
        //         for (Class<? extends B> type : family) {
        //             ArrayBackedEventHandler<?>[] priorityHandlers = handlers.get(type);
        //             if (priorityHandlers == null) continue;
        //             ArrayBackedEventHandler<?> handler = priorityHandlers[priority.ordinal()];
        //             if (handler == null) continue;
        //             flatProcessors.add(handler.invoker());
        //             flatTypes.add(handler.eventType());
        //
        //             flatChecking.add(new Pair<>(handler.eventType(), priority));
        //         }
        //     }
        //
        //     familyLengths[f] = flatProcessors.size() - familyOffsets[f];
        // }
        //
        // System.out.println("Flat checking: " + flatChecking);
        // dispatchTable = new DispatchTable(
        //         flatProcessors.toArray(EventProcessor[]::new),
        //         flatTypes.toArray(Class[]::new),
        //         familyOffsets,
        //         familyLengths
        // );
        List<Class<?>> flatTypes = new ArrayList<>();
        Priority[] priorities = Priority.values();
        int numPriorities = priorities.length;
        int numFamilies = families.size();
        // Segment (p, f) stored at p*numFamilies+f — keeps break valid per (priority, family) chunk
        int[] segmentOffsets = new int[numPriorities * numFamilies];
        int[] segmentLengths = new int[numPriorities * numFamilies];

        for (int p = 0; p < numPriorities; p++) {
            Priority priority = priorities[p];
            for (int f = 0; f < numFamilies; f++) {
                int idx = p * numFamilies + f;
                segmentOffsets[idx] = flatProcessors.size();
                for (Class<? extends B> type : families.get(f)) {
                    ArrayBackedEventHandler<?>[] priorityHandlers = handlers.get(type);
                    if (priorityHandlers == null) continue;
                    ArrayBackedEventHandler<?> handler = priorityHandlers[priority.ordinal()];
                    if (handler == null) continue;
                    flatProcessors.add(handler.invoker());
                    flatTypes.add(handler.eventType());

                    flatChecking.add(new Pair<>(handler.eventType(), priority));
                }
                segmentLengths[idx] = flatProcessors.size() - segmentOffsets[idx];
            }
        }

        System.out.println("Flat checking: " + flatChecking);
        dispatchTable = new DispatchTable(
                flatProcessors.toArray(EventProcessor[]::new),
                flatTypes.toArray(Class[]::new),
                segmentOffsets,
                segmentLengths);
    }

    /**
     * Build families from event types sorted by hierarchy depth.
     * A family is a linear chain — a new family starts at a branch point.
     */
    @SuppressWarnings("unchecked")
    private List<List<Class<? extends B>>> buildFamilies(List<Class<? extends B>> sortedTypes) {
        List<List<Class<? extends B>>> families = new ArrayList<>();
        Map<Class<?>, Integer> typeToFamily = new HashMap<>();

        for (Class<? extends B> type : sortedTypes) {
            Class<? extends B> parent = getRegisteredParent(type);

            if (parent == null) {
                List<Class<? extends B>> family = new ArrayList<>();
                family.add(type);
                typeToFamily.put(type, families.size());
                families.add(family);
            } else {
                int parentFamilyIdx = typeToFamily.get(parent);
                List<Class<? extends B>> parentFamily = families.get(parentFamilyIdx);

                // Branch if parent already has another child in this family
                Class<? extends B>[] siblings = childrenMap.getOrDefault(parent, new Class[0]);
                boolean branched = false;
                for (Class<? extends B> sibling : siblings) {
                    if (!sibling.equals(type)
                            && typeToFamily.containsKey(sibling)
                            && typeToFamily.get(sibling).equals(parentFamilyIdx)) {
                        branched = true;
                        break;
                    }
                }

                if (!branched && parentFamily.getLast().equals(parent)) {
                    parentFamily.add(type);
                    typeToFamily.put(type, parentFamilyIdx);
                } else {
                    List<Class<? extends B>> newFamily = new ArrayList<>();
                    newFamily.add(type);
                    typeToFamily.put(type, families.size());
                    families.add(newFamily);
                }
            }
        }

        return families;
    }

    /** Hierarchy depth — used to sort types shallowest (superclass) first.
     * {@code [PlayerLoginEvent, PlayerEvent, Event]}
     * 1 -> Event
     * 2 -> 2nd least specific (PlayerEvent)
     * 3 -> least specific (PlayerLoginEvent)
     */
    private int hierarchyDepth(Class<? extends Event> type) {
        Class<? extends @NotNull Event>[] hierarchy = EventHandlerInheritanceRegistry.getEventHierarchy(type);
        // System.out.println("hierarchyDepth: " + hierarchy.length + " event type: " + type + " pos: " + List.of(hierarchy).indexOf(type) + " list: " + Arrays.toString(hierarchy));
        return hierarchy.length;
    }


    /**
     * Immutable snapshot of the flat dispatch array and family metadata.
     * Built at registration time, read locklessly at dispatch.
     */
    private static final class DispatchTable {

        static final DispatchTable EMPTY =
                new DispatchTable(new EventProcessor[0], new Class[0], new int[0], new int[0]);

        /**
         * Flat array of merged invokers — one per (eventType, priority) handler.
         * Sorted: priority-first, superclass→subclass within each priority block per family.
         */
        private final EventProcessor<?>[] flat;

        /**
         * Parallel to {@link #flat} — the event type each processor belongs to.
         * Used for per-element {@code isInstance} check at dispatch.
         * When {@code isInstance} returns {@code false}, skip to the next family.
         */
        private final Class<?>[] flatTypes;

        // /** Start index of each family in {@link #flat}. */
        // private final int[] familyOffsets;
        //
        // /** Number of processors in each family. */
        // private final int[] familyLengths;
        /** Start index for segment (p, f), stored at p*numFamilies+f. */
        private final int[] segmentOffsets;

        /** Entry count for segment (p, f), stored at p*numFamilies+f. */
        private final int[] segmentLengths;

        // DispatchTable(EventProcessor<?>[] flat, Class<?>[] flatTypes,
        //               int[] familyOffsets, int[] familyLengths) {
        //     this.flat = flat;
        //     this.flatTypes = flatTypes;
        //     this.familyOffsets = familyOffsets;
        //     this.familyLengths = familyLengths;
        // }
        DispatchTable(EventProcessor<?>[] flat, Class<?>[] flatTypes,
                      int[] segmentOffsets, int[] segmentLengths) {
            this.flat = flat;
            this.flatTypes = flatTypes;
            this.segmentOffsets = segmentOffsets;
            this.segmentLengths = segmentLengths;
        }

        @SuppressWarnings("unchecked")
        <E extends Event> void dispatch(E event) {
            for (int s = 0; s < segmentOffsets.length; s++) {
                int start = segmentOffsets[s];
                int end = start + segmentLengths[s];
                for (int i = start; i < end; i++) {
                    if (!flatTypes[i].isInstance(event)) break; // not an instance; skip rest of family
                    ((EventProcessor<E>) flat[i]).process(event);
                }
            }
        }
        // @SuppressWarnings("unchecked")
        // <E extends Event> void dispatch(E event) {
        //     for (int f = 0; f < familyOffsets.length; f++) {
        //         int start = familyOffsets[f];
        //         int end = start + familyLengths[f];
        //         for (int i = start; i < end; i++) {
        //             if (!flatTypes[i].isInstance(event)) break; // not an instance; skip rest of family
        //             ((EventProcessor<E>) flat[i]).process(event);
        //         }
        //     }
        // }
    }
}