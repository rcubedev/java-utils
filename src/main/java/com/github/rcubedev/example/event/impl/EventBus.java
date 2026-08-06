package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.*;
import com.github.rcubedev.example.event.api.exceptions.EventStackOverflowException;
import com.github.rcubedev.example.event.api.spi.*;
import com.github.rcubedev.example.event.impl.bus.handler.ArrayBackedEventSink;
import com.github.rcubedev.example.event.impl.subscriber.EventSubscriberCompiler;
import com.github.rcubedev.example.event.impl.subscription.BasicSubscription;
import com.github.rcubedev.example.event.impl.subscription.BatchedSubscription;
import com.github.rcubedev.example.event.impl.subscription.MasterSubscription;
import com.github.rcubedev.example.event.test.TestableDispatchTable;
import com.github.rcubedev.example.event.test.TestableEventBus;
import com.github.rcubedev.example.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Concrete implementation of {@link IEventBus}.
 * <p>
 * Stores one {@link ArrayBackedEventSink} per {@code (eventType, priority)} pair.
 * Each handler holds its own listeners merged into a single invoker.
 * <p>
 * At registration time, {@link #rebuild()} reads all handlers' {@code eventType},
 * {@code priority}, and {@code invoker} to produce an immutable flat {@link DispatchTable}.
 * Dispatch performs one {@code isInstance} check per processor in the flat array. When it
 * returns {@code false}, the rest of the family is skipped.
 *
 * @param <B> The base event type this bus accepts
 */
// todo optimize posting by merging all handlers for event type into one EventProcessor to remove inst checks
// todo make type safe with GenericEvent<T> for example or document unsafety (likely latter).
// todo this violates single responsibility principle badly making it annoying to unit test etc.
//  example cleanup:
//class EventBus<B extends Event> {
//    private final ListenerRegistry<B> registry = new ListenerRegistry<>();
//    private final Dispatcher<B> dispatcher = new Dispatcher<>();
//
//    public <E extends B> void register(Class<E> type, Priority priority, EventProcessor<E> listener) {
//        registry.add(type, priority, listener);
//        dispatcher.rebuild(registry.snapshot());
//    }
//
//    public void post(B event) {
//        dispatcher.dispatch(event);
//    }
//}
//class ListenerRegistry<B extends Event> {
//    // Map: event type -> priority -> list of listeners
//    private final Map<Class<? extends B>, ArrayBackedEventHandler<? extends B>[]> handlers = new HashMap<>();
//
//    public <E extends B> void add(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
//        getOrCreateHandler(eventType, priority).addListener(listener);
//    }
//
//    public <E extends B> boolean remove(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
//        // similar to your removeListener()
//    }
//
//    public RegistrySnapshot<B> snapshot() {
//        // Returns immutable view of handlers
//        return new RegistrySnapshot<>(handlers);
//    }
//
//    private <E extends B> ArrayBackedEventHandler<E> getOrCreateHandler(Class<E> eventType, Priority priority) {
//        // Same logic you have now
//    }
//}
//class Dispatcher<B extends Event> {
//    private EventBus.DispatchTable dispatchTable = EventBus.DispatchTable.EMPTY;
//    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);
//    private final int maxStackDepth;
//
//    public Dispatcher(int maxStackDepth) {
//        this.maxStackDepth = maxStackDepth;
//    }
//
//    public void rebuild(RegistrySnapshot<B> snapshot) {
//        // Flatten handlers from snapshot into DispatchTable
//        this.dispatchTable = DispatchTableBuilder.build(snapshot);
//    }
//
//    public <E extends B> void dispatch(E event) {
//        int currentDepth = depth.get();
//        int newDepth = currentDepth + 1;
//        if (newDepth > maxStackDepth) throw new EventStackOverflowException(...);
//
//        try {
//            depth.set(newDepth);
//            dispatchTable.dispatch(event); // same as your current DispatchTable.dispatch()
//        } finally {
//            depth.set(currentDepth);
//        }
//    }
//}
//class RegistrySnapshot<B extends Event> {
//
//    // Map: event type -> array of handlers by priority
//    private final Map<Class<? extends B>, ArrayBackedEventHandler<? extends B>[]> handlers;
//
//    public RegistrySnapshot(Map<Class<? extends B>, ArrayBackedEventHandler<? extends B>[]> handlers) {
//        // Copy to make immutable (shallow copy is fine if handlers themselves are immutable for rebuild)
//        this.handlers = Map.copyOf(handlers);
//    }
//
//    public Map<Class<? extends B>, ArrayBackedEventHandler<? extends B>[]> getHandlers() {
//        return handlers;
//    }
//}
@UnitTestIgnored
@Deprecated
public final class EventBus<B extends Event> implements IEventBus<B>, TestableEventBus<B> {

    private static final ThreadLocal<int[]> depth = ThreadLocal.withInitial(() -> new int[]{0});

    private final Class<B> busType;
    private final Object rebuildLock = new Object();

    // One ArrayBackedEventHandler[] per eventType, indexed by Priority.ordinal()
    // Only accessed inside rebuildLock. Generic types should match; safe to cast ArrayBackedEventHandler<T>
    // if getting from Class<T>
    private final Map<Class<? extends B>, ArrayBackedEventSink<? extends B>[]> handlers = new HashMap<>();
    private final int maxStackDepth;

    // Flat dispatch array and family metadata. Rebuilt at registration, read-only at dispatch
    private volatile DispatchTable dispatchTable = DispatchTable.EMPTY;

    public EventBus(Class<B> busType, int maxStackDepth) {
        this.busType = busType;
        this.maxStackDepth = maxStackDepth;
    }

    @Override
    public @NotNull Class<B> getBusType() {
        return busType;
    }

    public @NotNull IEventBus<B> register() {
        EventBusRegistry.getInstance().register(this);
        return this;
    }

    @Override
    public <E extends B> void post(E event) {
        int[] depthArr = depth.get();
        int currentDepth = depthArr[0];
        int newDepth = currentDepth + 1;

        if (newDepth > maxStackDepth) {
            throw new EventStackOverflowException(
                    "Event recursion too deep (currentDepth: " + newDepth + ", maxStackDepth: " + maxStackDepth + "). " +
                            "Check for circular posts (e.g. A posts B, B posts A).", newDepth, maxStackDepth);
        }
        try {
            depthArr[0] = newDepth;
            dispatchTable.dispatch(event);
        } finally {
            depthArr[0] = currentDepth;
            if (currentDepth == 0) depth.remove();
        }
    }

    @Override
    public @NotNull RecursionBypass openBypassTo(int extraBudget) {
        if (extraBudget < 0) throw new IllegalArgumentException("extraBudget must be positive");
        int[] depthArr = depth.get();
        int previousDepth = depthArr[0];
        int newDepth = previousDepth - extraBudget;
        newDepth = newDepth > previousDepth ? Integer.MIN_VALUE : newDepth;
        // depth.set(newDepth);
        depthArr[0] = newDepth;

        // return () -> depth.set(previousDepth);
        return () -> depthArr[0] = previousDepth;
    }

    @Override
    public <E extends B> @NotNull Subscription register(Class<E> eventType, Priority priority, EventProcessor<E> listener, Identity identity) {
        Subscription sub = createSubscription(eventType, priority, listener);

        synchronized (rebuildLock) {
            getOrCreateHandler(eventType, priority).addListener(listener, sub);
            rebuild();
        }
        return sub;
    }

    @Override
    public @NotNull Subscription register(Object target, Identity identity) {
        List<BatchedSubscription> subscriptions = new ArrayList<>();
        // use anon to make compiler happy. swapped to named to annt with UnitTestIgnored.
        @UnitTestIgnored
        @Deprecated
        class RegistrarImpl implements Registrar<B> {
            @Override
            public <E extends B> @NotNull Subscription register(Class<E> type, Priority priority, EventProcessor<E> processor) {
                BatchedSubscription sub = createBatchedSubscription(type, priority, processor);
                // still use registerDirect because we are inside a batch
                EventBus.this.registerDirect(type, priority, processor, sub);
                subscriptions.add(sub);
                return sub;
            }
        }
        Registrar<B> register = new RegistrarImpl();

        synchronized (rebuildLock) {
            new EventSubscriberCompiler<>(busType).build(target, identity, register);
            rebuild();
        }
        return new MasterSubscription(subscriptions.toArray(BatchedSubscription[]::new), () -> {
            synchronized (rebuildLock) { rebuild(); }
        });
    }

    public void resetListeners() {
        synchronized (rebuildLock) {
            for (ArrayBackedEventSink<?>[] priorityHandlers : handlers.values()) {
                for (ArrayBackedEventSink<?> handler : priorityHandlers) {
                    if (handler != null) handler.clear();
                }
            }
            dispatchTable = DispatchTable.EMPTY;
        }
    }

    /**
     * Register a processor directly without triggering a rebuild.
     * <p>
     * Used by {@link EventSubscriberCompiler} to batch multiple
     * {@link SubscribeEvent @SubscribeEvent} registrations before a single rebuild.
     * <p>
     * Must be called inside {@code rebuildLock}.
     */
    private <E extends B> void registerDirect(Class<E> eventType, Priority priority, EventProcessor<E> listener, Subscription subscription) {
        getOrCreateHandler(eventType, priority).addListener(listener, subscription);
    }

    // todo if there are two of the same listeners it will remove both.
    private <E extends B> boolean removeListener(Class<E> eventType, Priority priority, Subscription subscription) {
        @SuppressWarnings("unchecked")
        ArrayBackedEventSink<E>[] priorityHandlers = (ArrayBackedEventSink<E>[]) handlers.get(eventType);
        boolean removed = false;
        if (priorityHandlers != null) {
            ArrayBackedEventSink<E> handler = priorityHandlers[priority.ordinal()];
            if (handler != null) removed = handler.removeListener(subscription);
        }
        return removed;
    }

    private <E extends B> Subscription createSubscription(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
        Consumer<Subscription> unregister = sub -> {
            synchronized (rebuildLock) {
                if (removeListener(eventType, priority, sub)) rebuild();
            }
        };

        Subscription sub = new BasicSubscription(unregister);
        if (listener instanceof SubscriptionAware linkable) linkable.acceptSubscription(sub);
        return sub;
    }

    private <E extends B> BatchedSubscription createBatchedSubscription(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
        Predicate<Subscription> unregister = sub -> removeListener(eventType, priority, sub);
        Consumer<Subscription> unsubscribe = sub -> {
            synchronized (rebuildLock) {
                if (unregister.test(sub)) rebuild();
            }
        };

        BatchedSubscription sub = new BatchedSubscription(unregister, unsubscribe);
        if (listener instanceof SubscriptionAware linkable) linkable.acceptSubscription(sub);
        return sub;
    }

    @SuppressWarnings("unchecked")
    private <E extends B> ArrayBackedEventSink<E> getOrCreateHandler(Class<E> eventType, Priority priority) {
        ArrayBackedEventSink<E>[] priorityHandlers = (ArrayBackedEventSink<E>[]) handlers.get(eventType);
        if (priorityHandlers == null) {
            priorityHandlers = new ArrayBackedEventSink[Priority.values().length];
            handlers.put(eventType, priorityHandlers);
        }
        int ordinal = priority.ordinal();
        if (priorityHandlers[ordinal] == null) {
            priorityHandlers[ordinal] = new ArrayBackedEventSink<>(eventType, priority);
        }
        return priorityHandlers[ordinal];
    }

    /**
     * Find the nearest registered ancestor of {@code type} on this bus.
     * <p>
     * The return generic {@code <? super E>} is a subtype of {@link B}.
     */
    private <E extends B> @Nullable Class<? super E> getRegisteredParent(Class<E> type) {
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
     * Rebuild the flat dispatch array from all stored {@link ArrayBackedEventSink}s.
     * <p>
     * Re-adds each handler's {@link ArrayBackedEventSink#eventType()},
     * {@link ArrayBackedEventSink#priority()}, and {@link ArrayBackedEventSink#invoker()}.
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
        // siblings will be in order of hashmap; JVM dependent.
        allTypes.sort(Comparator.comparingInt(this::hierarchyDepth));

        // Map every unique type to a bit index
        Map<Class<? extends B>, Integer> typeToBitIndex = new HashMap<>();
        int allTypesLen = allTypes.size();
        for (int i = 0; i < allTypesLen; i++) {
            typeToBitIndex.put(allTypes.get(i), i);
        }
        // System.out.println("Type to bit index: " + typeToBitIndex);

        // Build families. Linear chains split at branch points
        List<List<Class<? extends B>>> families = buildFamilies(allTypes);

        // Flatten into 1D dispatch array. Priority-first, superclass -> subclass within family
        List<EventProcessor<?>> flatProcessors = new ArrayList<>();
        List<Class<?>> flatTypes = new ArrayList<>();
        List<Integer> parentBitIndicesList = new ArrayList<>();
        List<Integer> selfBitIndicesList = new ArrayList<>();

        // record Pair<A, B>(A a, B b) {}
        // List<Pair<Class<?>, Priority>> flatChecking = new ArrayList<>();

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
                    ArrayBackedEventSink<?>[] priorityHandlers = handlers.get(type);
                    if (priorityHandlers == null) continue;

                    ArrayBackedEventSink<?> handler = priorityHandlers[priority.ordinal()];
                    if (handler == null) continue;

                    // Data for the dispatch table
                    flatProcessors.add(handler.invoker());
                    flatTypes.add(handler.eventType());

                    // Map self and parent bits
                    selfBitIndicesList.add(typeToBitIndex.get(type));

                    @SuppressWarnings("unchecked") // safe as #getRegisteredParent ret is always a subtype of B
                    Class<? extends B> parent = (Class<? extends B>) getRegisteredParent(type);
                    parentBitIndicesList.add(parent == null ? -1 : typeToBitIndex.get(parent));

                    // flatChecking.add(new Pair<>(handler.eventType(), priority));
                }
                segmentLengths[idx] = flatProcessors.size() - segmentOffsets[idx];
            }
        }

        // System.out.println("Flat checking: " + flatChecking);
        dispatchTable = new DispatchTable(
                flatProcessors.toArray(EventProcessor[]::new),
                flatTypes.toArray(Class[]::new),
                segmentOffsets,
                segmentLengths,
                parentBitIndicesList.stream().mapToInt(i -> i).toArray(),
                selfBitIndicesList.stream().mapToInt(i -> i).toArray(),
                (allTypes.size() + 63) / 64
        );
    }

    /**
     * Build families from event types sorted by hierarchy depth.
     * <p>
     * A family is a linear chain; a new family starts at a branch point.
     */
    private List<List<Class<? extends B>>> buildFamilies(List<Class<? extends B>> sortedTypes) {
        // We go back to the SIMPLEST version.
        // Families are just linear chains where each child's parent is the element before it.
        List<List<Class<? extends B>>> families = new ArrayList<>();
        Map<Class<? extends B>, Integer> typeToFamilyIdx = new HashMap<>();

        for (Class<? extends B> type : sortedTypes) {
            @SuppressWarnings("unchecked") // safe because getRegisteredParent always returns a subtype of B.
            Class<? extends B> parent = (Class<? extends B>) getRegisteredParent(type);

            if (parent == null || !typeToFamilyIdx.containsKey(parent)) {
                List<Class<? extends B>> family = new ArrayList<>();
                family.add(type);
                typeToFamilyIdx.put(type, families.size());
                families.add(family);
            } else {
                int familyIdx = typeToFamilyIdx.get(parent);
                List<Class<? extends B>> family = families.get(familyIdx);

                if (family.getLast().equals(parent)) {
                    family.add(type);
                    typeToFamilyIdx.put(type, familyIdx);
                } else {
                    // Sibling branch: Starts a NEW family.
                    // IMPORTANT: This family starts at the sibling, NOT the parent.
                    List<Class<? extends B>> newBranch = new ArrayList<>();
                    newBranch.add(type);
                    typeToFamilyIdx.put(type, families.size());
                    families.add(newBranch);
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
    @UnitTestIgnored
    @Deprecated
    public static final class DispatchTable implements TestableDispatchTable {

        static final DispatchTable EMPTY =
                new DispatchTable(new EventProcessor[0], new Class[0], new int[0], new int[0], new int[0], new int[0], 0);

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
         * When {@link Class#isInstance} returns {@code false}, skip to the next family.
         */
        private final Class<?>[] flatTypes;

        /**
         * Start index for segment (priority, family), stored at priority * numFamilies + family.
         */
        private final int[] segmentOffsets;

        /**
         * Entry count for segment (priority, family), stored at priority * numFamilies + family.
         */
        private final int[] segmentLengths;

        // BitSet metadata
        /**
         * Bit index of the registered parent for the type at this index.
         * <p>
         * Used to perform skips across different families.
         */
        private final int[] parentBitIndices;

        /**
         * Unique bit index for the type at this index.
         * <p>
         * Used to mark success in the {@code passBits} bitset.
         */
        private final int[] selfBitIndices;

        /**
         * Number of long slots required to represent all unique registered types.
         */
        private final int bitSetSize; // (numUniqueTypes + 63) / 64 <-- int ceil

        /**
         * Thread-local scratchpad to ensure thread-safe dispatch without per-post allocations.
         */
        private static final ThreadLocal<long[]> BIT_STRIP_CACHE = new ThreadLocal<>();

        DispatchTable(EventProcessor<?>[] flat, Class<?>[] flatTypes,
                      int[] segmentOffsets, int[] segmentLengths,
                      int[] parentBitIndices, int[] selfBitIndices, int bitSetSize) {
            this.flat = flat;
            this.flatTypes = flatTypes;
            this.segmentOffsets = segmentOffsets;
            this.segmentLengths = segmentLengths;
            this.parentBitIndices = parentBitIndices;
            this.selfBitIndices = selfBitIndices;
            this.bitSetSize = bitSetSize;
        }

        /**
         * Dispatches the given event to all compatible processors in the table.
         * <p>
         * This method uses a dual-skip strategy to optimise event processing.
         * <ul>
         *   <li><b>Horizontal Skip:</b> Uses a bitset to skip entire branches when a common parent has failed elsewhere.</li>
         *   <li><b>Vertical Skip:</b> Uses the linear family structure to skip children if a parent fails in-place.</li>
         * </ul>
         *
         * @param event The event to post.
         * @param <E>   The event type.
         */
        @SuppressWarnings("unchecked")
        public <E extends Event> void dispatch(E event) {
            if (flat.length == 0) return;

            long[] passBits = BIT_STRIP_CACHE.get();
            if (passBits == null || passBits.length < bitSetSize) {
                passBits = new long[bitSetSize];
                BIT_STRIP_CACHE.set(passBits);
            }
            Arrays.fill(passBits, 0L);

            for (int s = 0; s < segmentOffsets.length; s++) {
                int start = segmentOffsets[s];
                int end = start + segmentLengths[s];

                for (int i = start; i < end; i++) {
                    // if parent didn't pass isInstance check (in any family), skip this one.
                    final int pIdx = parentBitIndices[i];
                    if (pIdx != -1 && (passBits[pIdx >> 6] & (1L << (pIdx & 63))) == 0) break;

                    if (!flatTypes[i].isInstance(event)) break; // not an instance; skip rest of family

                    // mark success
                    final int selfIdx = selfBitIndices[i];
                    passBits[selfIdx >> 6] |= (1L << (selfIdx & 63));

                    // fire listeners
                    ((EventProcessor<E>) flat[i]).process(event);
                }
            }
        }

        @Override
        public @NotNull EventProcessor<?>[] getFlatEventProcessors() {
            return this.flat;
        }

        @Override
        public @NotNull Class<?>[] getFlatTypes() {
            return this.flatTypes;
        }

        @Override
        public int @NotNull [] getSegmentOffsets() {
            return this.segmentOffsets;
        }

        @Override
        public int @NotNull [] getSegmentLengths() {
            return this.segmentLengths;
        }

        @Override
        public int @NotNull [] getParentBitIndices() {
            return this.parentBitIndices;
        }

        @Override
        public int @NotNull [] getSelfBitIndices() {
            return this.selfBitIndices;
        }

        @Override
        public int getBitSetSize() {
            return this.bitSetSize;
        }
    }

    @Override
    public @NotNull EventBus.DispatchTable getDispatchTable() {
        return this.dispatchTable;
    }

    @Override
    public void setDispatchTable(@NotNull EventBus.DispatchTable table) {
        synchronized (rebuildLock) {
            this.dispatchTable = table;
        }
    }

    @Override
    public int getCurrentRecursionDepth() {
        return depth.get()[0];
    }

    @Override
    public void setRecursionDepth(int newDepth) {
        depth.get()[0] = newDepth;
    }
}
