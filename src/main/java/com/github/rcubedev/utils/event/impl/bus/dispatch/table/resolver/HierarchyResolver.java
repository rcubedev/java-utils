package com.github.rcubedev.utils.event.impl.bus.dispatch.table.resolver;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.RegisteredParentResolver;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Resolves missing event mappings by climbing the registered type hierarchy.
 */
public final class HierarchyResolver<B extends Event> implements Resolver<B> {

    private final RegisteredParentResolver<B> resolver;
    private final Map<Class<? extends B>, List<EventProcessor<? super B>>> preComputedPool;

    public HierarchyResolver(
            RegisteredParentResolver<B> resolver,
            Map<Class<? extends B>, List<EventProcessor<? super B>>> preComputedPool) {
        this.resolver = resolver;
        this.preComputedPool = preComputedPool;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull List<EventProcessor<? super B>> resolve(@NotNull Class<?> unregisteredType) {
        Class<? extends B> targetType = (Class<? extends B>) unregisteredType;
        Class<? extends B> parent = resolver.getRegisteredParentAsExtendsBus(targetType);

        if (parent != null) return preComputedPool.get(parent);
        return List.of();
    }

    public interface Factory<B extends Event> {

        HierarchyResolver<B> create(RegisteredParentResolver<B> resolver,
                                    Map<Class<? extends B>, List<EventProcessor<? super B>>> preComputedPool);
    }
}
