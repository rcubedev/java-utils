package com.github.rcubedev.example.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.FallbackResolver;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.RegisteredParentResolver;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * Resolves missing event mappings by climbing the registered type hierarchy.
 */
public final class HierarchyFallbackResolver<B extends Event> implements FallbackResolver<B> {

    private final RegisteredParentResolver<B> resolver;
    private final Map<Class<? extends B>, EventProcessor<? super B>[]> preComputedPool;
    private final EventProcessor<? super B>[] emptyArray;

    @SuppressWarnings("unchecked")
    public HierarchyFallbackResolver(
            RegisteredParentResolver<B> resolver,
            Map<Class<? extends B>, EventProcessor<? super B>[]> preComputedPool) {
        this.resolver = resolver;
        this.preComputedPool = preComputedPool;
        this.emptyArray = (EventProcessor<? super B>[]) new EventProcessor<?>[0];
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull EventProcessor<? super B>[] resolve(@NotNull Class<?> unregisteredType) {
        Class<? extends B> targetType = (Class<? extends B>) unregisteredType;
        Class<? extends B> parent = resolver.getRegisteredParentAsExtendsBus(targetType);
        if (parent == null) return emptyArray;

        EventProcessor<? super B>[] result = preComputedPool.get(parent);
        return result != null ? result : emptyArray;
    }
}
