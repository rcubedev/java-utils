package com.github.rcubedev.example.event.impl.bus.dispatch.table.resolver;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A composite resolver that chains {@link Resolver}s.
 */
public final class CompositeResolver<B extends Event> implements Resolver<B> {

    private final List<Resolver<B>> resolvers;

    public CompositeResolver(List<Resolver<B>> resolvers) {
        this.resolvers = List.copyOf(resolvers);
    }

    @Override
    public @NotNull List<EventProcessor<? super B>> resolve(@NotNull Class<?> unregisteredType) {
        for (Resolver<B> resolver : resolvers) {
            List<EventProcessor<? super B>> processors = resolver.resolve(unregisteredType);
            if (!processors.isEmpty()) return processors;
        }
        return List.of();
    }

    public interface Factory<B extends Event> {
        CompositeResolver<B> create(List<Resolver<B>> resolvers);
    }
}
