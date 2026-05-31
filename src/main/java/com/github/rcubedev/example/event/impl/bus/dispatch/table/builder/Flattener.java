package com.github.rcubedev.example.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.DispatchTable;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.RegisteredParentResolver;
import com.github.rcubedev.example.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import com.github.rcubedev.example.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Flattener<B extends Event> {

    private final RegistrySnapshot<B> snapshot;
    private final RegisteredParentResolver<B> resolver;
    private final HierarchyFallbackResolverFactory<B> hierarchyFallbackFactory;
    private final DispatchTableFactory<B> tableFactory;

    @UnitTestIgnored
    public Flattener(RegistrySnapshot<B> snapshot, RegisteredParentResolver<B> resolver) {
        this(snapshot, resolver, HierarchyFallbackResolver::new, DispatchTable::new);
    }

    Flattener(RegistrySnapshot<B> snapshot, RegisteredParentResolver<B> resolver,
              HierarchyFallbackResolverFactory<B> hierarchyFallbackFactory, DispatchTableFactory<B> tableFactory) {
        this.snapshot = snapshot;
        this.resolver = resolver;
        this.hierarchyFallbackFactory = hierarchyFallbackFactory;
        this.tableFactory = tableFactory;
    }

    @SuppressWarnings("unchecked")
    public @NotNull DispatchTable<B> flatten(List<List<Class<? extends B>>> families) {
        Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> handlers = snapshot.getHandlers();
        Priority[] priorities = Priority.values();

        Map<Class<? extends B>, EventProcessor<? super B>[]> preComputedPool = new HashMap<>();

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
            preComputedPool.put(type, processorsForType.toArray(EventProcessor[]::new));
        }
        return tableFactory.create(preComputedPool, hierarchyFallbackFactory.create(resolver, preComputedPool));
    }
}
