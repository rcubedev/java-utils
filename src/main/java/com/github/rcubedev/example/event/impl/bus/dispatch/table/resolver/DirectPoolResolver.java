package com.github.rcubedev.example.event.impl.bus.dispatch.table.resolver;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Routes events to their exact type.
 */
public final class DirectPoolResolver<E extends Event> implements Resolver<E> {

    private final Map<Class<? extends E>, List<EventProcessor<? super E>>> preComputedPool;

    public DirectPoolResolver(Map<Class<? extends E>, List<EventProcessor<? super E>>> preComputedPool) {
        this.preComputedPool = preComputedPool;
    }

    @Override
    public @NotNull List<EventProcessor<? super E>> resolve(@NotNull Class<?> unregisteredType) {
        List<EventProcessor<? super E>> processors = preComputedPool.get(unregisteredType);
        if (processors == null) return List.of();
        return processors;
    }

    public interface Factory<B extends Event> {
        DirectPoolResolver<B> create(Map<Class<? extends B>, List<EventProcessor<? super B>>> preComputedPool);
    }
}
