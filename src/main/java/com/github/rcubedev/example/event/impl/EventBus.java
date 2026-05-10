package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.spi.RecursionBypass;
import com.github.rcubedev.example.event.api.SubscribeEvent;
import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.api.spi.IEventBus;
import com.github.rcubedev.example.event.api.spi.Linkable;
import com.github.rcubedev.example.event.api.spi.Registrar;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Concrete implementation of {@link IEventBus}.
 * <p>
 * Stores one {@link ArrayBackedEventHandler} per {@code (eventType, priority)} pair.
 * Each handler holds its own listeners merged into a single invoker.
 * <p>
 * At registration time, {@link #rebuild()} reads all handlers' {@code eventType},
 * {@code priority}, and {@code invoker} to produce an immutable flat {@link DispatchTable}.
 * Dispatch performs one {@code isInstance} check per processor in the flat array. When it
 * returns {@code false}, the rest of the family is skipped.
 * <p>
 * Extend to create a named singleton bus:
 * <pre>
 * {@code
 * public abstract class CustomEvent extends Event {}
 *
 * public final class CustomEventBus extends EventBus<CustomEvent> {
 *     public static final IEventBus<CustomEvent> INSTANCE = new CustomEventBus().register();
 *     private CustomEventBus() { super(CustomEvent.class); }
 * }
 * }
 * </pre>
 *
 * @param <B> The base event type this bus accepts
 */
// todo optimize posting by merging all handlers for event type into one EventProcessor to remove inst checks
// todo make type safe with GenericEvent<T> for example or document unsafety (likely latter).
public final class EventBus<B extends Event> implements IEventBus<B> {

    private final Class<B> busType;

    // One ArrayBackedEventHandler[] per eventType, indexed by Priority.ordinal()
    // Only accessed inside rebuildLock. Generic types should match; safe to cast ArrayBackedEventHandler<T>
    // if getting from Class<T>
    private final Map<Class<? extends B>, ArrayBackedEventHandler<? extends B>[]> handlers = new HashMap<>();

    // Children per parent. Array grown on each new child, only written/read inside rebuildLock
    private final Map<Class<? extends B>, Class<? extends B>[]> childrenMap = new HashMap<>();

    // Flat dispatch array and family metadata. Rebuilt at registration, read-only at dispatch
    private volatile DispatchTable dispatchTable = DispatchTable.EMPTY;

    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);
    private final int maxStackDepth;

    private final Object rebuildLock = new Object();
    private volatile boolean registered = false;

    public EventBus(Class<B> busType, int maxStackDepth) {
        this.busType = busType;
        this.maxStackDepth = maxStackDepth;
    }

    @Override
    public @NotNull Class<B> getBusType() {
        return busType;
    }

    public @NotNull IEventBus<B> register() {
        Supplier<RuntimeException> exSupplier = () -> new IllegalStateException("Attempted to register the bus when it is already registered!");
        if (registered) throw exSupplier.get();
        synchronized (rebuildLock) { // todo should likely use a different lock
            if (registered) throw exSupplier.get();
            registered = true;
        }
        EventBusRegistry.register(this);
        return this;
    }

    @Override
    public <E extends B> void post(E event) {
        int currentDepth = depth.get();

        if (currentDepth > maxStackDepth) throw new StackOverflowGuardExcepption(
                "Stack Overflow Guard: Event recursion too deep. Check for circular posts (e.g. A posts B, B posts A).");
        try {
            depth.set(currentDepth + 1);
            dispatchTable.dispatch(event);
        } finally {
            depth.set(currentDepth);
        }
    }

    @Override
    public @NotNull RecursionBypass openBypassTo(int extraBudget) {
        if (extraBudget < 0) throw new IllegalArgumentException("extraBudget must be positive");
        int previousDepth = depth.get();
        depth.set(-extraBudget);

        return () -> depth.set(previousDepth);
    }

    @Override
    public <E extends B> @NotNull Subscription register(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
        Subscription sub = createSubscription(eventType, priority, listener);

        synchronized (rebuildLock) {
            getOrCreateHandler(eventType, priority).addListener(listener);
            rebuild();
        }
        return sub;
    }

    @Override
    public @NotNull Subscription register(Object target) {
        List<BatchedSubscription> subscriptions = new ArrayList<>();
        Registrar<B> register = (type, priority, processor) -> { // fixme not type safe as Registrar registers any Event type
            BatchedSubscription sub = createBatchedSubscription(type, priority, processor);
            // still use registerDirect because we are inside a batch
            this.registerDirect(type, priority, processor);
            subscriptions.add(sub);
            return sub;
        };

        synchronized (rebuildLock) {
            EventSubscriberHandler.register(this, target, register);
            rebuild();
        }
        return new MasterSubscription(subscriptions.toArray(BatchedSubscription[]::new), () -> {
            synchronized (rebuildLock) { rebuild(); }
        });
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
     * Register a processor directly without triggering a rebuild.
     * <p>
     * Used by {@link EventSubscriberHandler} to batch multiple
     * {@link SubscribeEvent @SubscribeEvent} registrations before a single rebuild.
     * <p>
     * Must be called inside {@code rebuildLock}.
     */
    private <E extends B> void registerDirect(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
        getOrCreateHandler(eventType, priority).addListener(listener);
    }

    private <E extends B> boolean removeListener(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
        @SuppressWarnings("unchecked")
        ArrayBackedEventHandler<E>[] priorityHandlers = (ArrayBackedEventHandler<E>[]) handlers.get(eventType);
        boolean removed = false;
        if (priorityHandlers != null) {
            ArrayBackedEventHandler<E> handler = priorityHandlers[priority.ordinal()];
            if (handler != null) removed = handler.removeListener(listener);
        }
        return removed;
    }

    private <E extends B> Subscription createSubscription(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
        Runnable unregister = () -> {
            synchronized (rebuildLock) {
                if (removeListener(eventType, priority, listener)) rebuild();
            }
        };

        Subscription sub = new BasicSubscription(unregister);
        if (listener instanceof Linkable linkable) linkable.setSubscription(sub);
        return sub;
    }

    private <E extends B> BatchedSubscription createBatchedSubscription(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
        BooleanSupplier unregister = () -> removeListener(eventType, priority, listener);
        Runnable unsubscribe = () -> {
            synchronized (rebuildLock) {
                if (unregister.getAsBoolean()) rebuild();
            }
        };

        BatchedSubscription sub = new BatchedSubscription(unregister, unsubscribe);
        if (listener instanceof Linkable linkable) linkable.setSubscription(sub);
        return sub;
    }

    @SuppressWarnings("unchecked")
    private <E extends B> ArrayBackedEventHandler<E> getOrCreateHandler(Class<E> eventType, Priority priority) {
        ArrayBackedEventHandler<E>[] priorityHandlers = (ArrayBackedEventHandler<E>[]) handlers.get(eventType);
        if (priorityHandlers == null) {
            priorityHandlers = new ArrayBackedEventHandler[Priority.values().length];
            handlers.put(eventType, priorityHandlers);
            trackType(eventType);
        }
        int ordinal = priority.ordinal();
        if (priorityHandlers[ordinal] == null) {
            priorityHandlers[ordinal] = new ArrayBackedEventHandler<>(eventType, priority);
        }
        return priorityHandlers[ordinal];
    }

    /**
     * Record parent -> child relationship for family building.
     * <p>
     * Called the first time an event type is seen. Must be called inside {@code rebuildLock}.
     */
    @SuppressWarnings("unchecked")
    private <E extends B> void trackType(Class<E> eventType) {
        Class<? extends B> parent = (Class<? extends B>) getRegisteredParent(eventType);
        if (parent != null) {
            Class<? extends B>[] current = childrenMap.get(parent);
            Class<? extends B>[] next;
            if (current != null) {
                next = (Class<? extends B>[]) new Class<?>[current.length + 1];
                System.arraycopy(current, 0, next, 0, current.length);
            }
            else next = (Class<? extends B>[]) new Class<?>[1];
            next[next.length - 1] = eventType;
            childrenMap.put(parent, next);
        }
    }

    /**
     * Find the nearest registered ancestor of {@code type} on this bus.
     * <p>
     * The return generic {@code <? super E>} is a subtype of {@link B}.
     */
    private <E extends B> Class<? super E> getRegisteredParent(Class<E> type) {
        Class<? super E>[] hierarchy = EventHandlerInheritanceRegistry.getEventHierarchy(type);
        for (int i = 1; i < hierarchy.length; i++) {
            Class<? super E> ancestor = hierarchy[i];
            if (busType.isAssignableFrom(ancestor) && handlers.containsKey(ancestor)) {
                return ancestor;
            }
        }
        return null;
    }

    /**
     * Rebuild the flat dispatch array from all stored {@link ArrayBackedEventHandler}s.
     * <p>
     * Re-adds each handler's {@link ArrayBackedEventHandler#eventType()},
     * {@link ArrayBackedEventHandler#priority()}, and {@link ArrayBackedEventHandler#invoker()}.
     * <p>
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

        // Build families. Linear chains split at branch points
        List<List<Class<? extends B>>> families = buildFamilies(allTypes);

        // Flatten into 1D dispatch array. Priority-first, superclass -> subclass within family
        // Parallel array of event types for per-element isInstance check at dispatch
        List<EventProcessor<?>> flatProcessors = new ArrayList<>();

        record Pair<A, B>(A a, B b) {}
        List<Pair<Class<?>, Priority>> flatChecking = new ArrayList<>();

        List<Class<?>> flatTypes = new ArrayList<>();
        Priority[] priorities = Priority.values();
        int numPriorities = priorities.length;
        int numFamilies = families.size();
        // Segment (p, f) stored at p*numFamilies+f. Keeps break valid per (priority, family) chunk
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
     * <p>
     * A family is a linear chain; a new family starts at a branch point.
     */
    @SuppressWarnings("unchecked")
    private List<List<Class<? extends B>>> buildFamilies(List<Class<? extends B>> sortedTypes) {
        List<List<Class<? extends B>>> families = new ArrayList<>();
        Map<Class<?>, Integer> typeToFamily = new HashMap<>();

        for (Class<? extends B> type : sortedTypes) {
            Class<? extends B> parent = (Class<? extends B>) getRegisteredParent(type);

            if (parent == null) {
                List<Class<? extends B>> family = new ArrayList<>();
                family.add(type);
                typeToFamily.put(type, families.size());
                families.add(family);
            } else {
                int parentFamilyIdx = typeToFamily.get(parent);
                List<Class<? extends B>> parentFamily = families.get(parentFamilyIdx);

                // Branch if parent already has another child in this family
                Class<? extends B>[] siblings = childrenMap.getOrDefault(parent, (Class<? extends B>[]) new Class<?>[0]);
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

    /**
     * Hierarchy depth; used to sort types shallowest (superclass) first.
     * <ol>{@code [PlayerLoginEvent, PlayerEvent, Event]}
     * <li>Event</li>
     * <li>2nd least specific (PlayerEvent)</li>
     * <li>Least specific (PlayerLoginEvent)</li>
     * </ol>
     */
    private <E extends B> int hierarchyDepth(Class<E> type) {
        Class<? super @NotNull E>[] hierarchy = EventHandlerInheritanceRegistry.getEventHierarchy(type);
        return hierarchy.length;
    }

    /**
     * Immutable snapshot of the flat dispatch array and family metadata.
     * <p>
     * Built at registration time, read locklessly at dispatch.
     */
    private static final class DispatchTable {

        static final DispatchTable EMPTY =
                new DispatchTable(new EventProcessor[0], new Class[0], new int[0], new int[0]);

        /**
         * Flat array of merged invokers. One per (eventType, priority) handler.
         * <p>
         * Sorted: priority-first, superclass -> subclass within each priority block per family.
         */
        private final EventProcessor<?>[] flat;

        /**
         * Parallel to {@link #flat}; the event type each processor belongs to.
         * <p>
         * Used for per-element {@code isInstance} check at dispatch.<br>
         * When {@code isInstance} returns {@code false}, skip to the next family.
         */
        private final Class<?>[] flatTypes;

        /**
         * Start index for segment (p, f), stored at p*numFamilies+f.
         */
        private final int[] segmentOffsets;

        /**
         * Entry count for segment (p, f), stored at p*numFamilies+f.
         */
        private final int[] segmentLengths;

        DispatchTable(EventProcessor<?>[] flat, Class<?>[] flatTypes,
                      int[] segmentOffsets, int[] segmentLengths) {
            this.flat = flat;
            this.flatTypes = flatTypes;
            this.segmentOffsets = segmentOffsets;
            this.segmentLengths = segmentLengths;
        }

        @SuppressWarnings("unchecked")
        public <E extends Event> void dispatch(E event) {
            for (int s = 0; s < segmentOffsets.length; s++) {
                int start = segmentOffsets[s];
                int end = start + segmentLengths[s];
                for (int i = start; i < end; i++) {
                    if (!flatTypes[i].isInstance(event)) break; // not an instance; skip rest of family
                    ((EventProcessor<E>) flat[i]).process(event);
                }
            }
        }
    }
}