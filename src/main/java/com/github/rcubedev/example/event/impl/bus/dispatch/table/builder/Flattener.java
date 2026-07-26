package com.github.rcubedev.example.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.DispatchTable;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.RegisteredParentResolver;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.resolver.*;
import com.github.rcubedev.example.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import com.github.rcubedev.example.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Flattener<B extends Event> {

    private final RegistrySnapshot<B> snapshot;
    private final RegisteredParentResolver<B> resolver;
    private final CompositeResolver.Factory<B> compositeFallbackFactory;
    private final DirectPoolResolver.Factory<B> directPoolFallbackFactory;
    private final HierarchyResolver.Factory<B> hierarchyFallbackFactory;
    private final DeadEventResolver.Factory<B> deadEventFallbackFactory;
    private final DispatchTableFactory<B> tableFactory;

    @UnitTestIgnored
    public Flattener(RegistrySnapshot<B> snapshot, RegisteredParentResolver<B> resolver) {
        this(snapshot, resolver, CompositeResolver::new, DirectPoolResolver::new,
                HierarchyResolver::new, DeadEventResolver::new, DispatchTable::create);
    }

    Flattener(RegistrySnapshot<B> snapshot, RegisteredParentResolver<B> resolver,
              CompositeResolver.Factory<B> compositeFallbackFactory,
              DirectPoolResolver.Factory<B> directPoolFallbackFactory,
              HierarchyResolver.Factory<B> hierarchyFallbackFactory,
              DeadEventResolver.Factory<B> deadEventFallbackFactory, DispatchTableFactory<B> tableFactory) {
        this.snapshot = snapshot;
        this.resolver = resolver;
        this.compositeFallbackFactory = compositeFallbackFactory;
        this.directPoolFallbackFactory = directPoolFallbackFactory;
        this.hierarchyFallbackFactory = hierarchyFallbackFactory;
        this.deadEventFallbackFactory = deadEventFallbackFactory;
        this.tableFactory = tableFactory;
    }

    @SuppressWarnings("unchecked")
    public @NotNull DispatchTable<B> flatten(List<List<Class<? extends B>>> families) {
        Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> handlers = snapshot.getHandlers();
        Priority[] priorities = Priority.values();

        Map<Class<? extends B>, List<EventProcessor<? super B>>> preComputedPool = new HashMap<>();

        for (List<Class<? extends B>> family : families) {
            if (family.isEmpty()) continue;

            Class<? extends B> type = family.getLast();
            List<EventProcessor<? super B>> processorsForType = new ArrayList<>();

            for (Priority priority : priorities) {
                for (Class<? extends B> eventClass : family) {
                    Map<Priority, EventSinkSnapshot<? extends B>> priorityHandlers = handlers.get(eventClass);
                    if (priorityHandlers != null) {
                        EventSinkSnapshot<? extends B> handler = priorityHandlers.get(priority);
                        if (handler != null) processorsForType.add((EventProcessor<? super B>) handler.invoker());
                    }
                }
            }
            preComputedPool.put(type, List.copyOf(processorsForType));
        }
        Map<Class<? extends B>, List<EventProcessor<? super B>>> immutablePool = Map.copyOf(preComputedPool);

        Resolver<B> directPoolLayer = directPoolFallbackFactory.create(immutablePool);
        Resolver<B> hierarchyLayer = hierarchyFallbackFactory.create(resolver, immutablePool);
        Resolver<B> deadEventLayer = deadEventFallbackFactory.create(immutablePool);
        Resolver<B> fallback = compositeFallbackFactory.create(List.of(directPoolLayer, hierarchyLayer, deadEventLayer));
        return tableFactory.create(fallback, immutablePool.keySet());
    }
}
