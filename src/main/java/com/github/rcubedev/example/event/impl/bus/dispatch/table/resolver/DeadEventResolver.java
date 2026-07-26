package com.github.rcubedev.example.event.impl.bus.dispatch.table.resolver;

import com.github.rcubedev.example.event.api.DeadEvent;
import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

/**
 * Routes unhandled events to {@link DeadEvent} listeners.
 */
// todo only hold list of DeadEvent procs
public final class DeadEventResolver<B extends Event> implements Resolver<B> {

    private final Map<Class<? extends B>, List<EventProcessor<? super B>>> preComputedPool;

    public DeadEventResolver(Map<Class<? extends B>, List<EventProcessor<? super B>>> preComputedPool) {
        this.preComputedPool = preComputedPool;
    }

    @SuppressWarnings("unchecked")
    @Override
    public @NotNull List<EventProcessor<? super B>> resolve(@NotNull Class<?> unregisteredType) {
        if (DeadEvent.class.isAssignableFrom(unregisteredType)) return List.of();

        List<EventProcessor<DeadEvent>> deadEventListeners = (List<EventProcessor<DeadEvent>>) (List<?>) preComputedPool.get(DeadEvent.class);
        if (deadEventListeners == null || deadEventListeners.isEmpty()) return List.of();

        return List.of(
                event -> {
                    DeadEvent deadEvent = new DeadEvent(event);
                    for (EventProcessor<DeadEvent> target : deadEventListeners) target.process(deadEvent);
                }
        );
    }

    public interface Factory<B extends Event> {
        DeadEventResolver<B> create(Map<Class<? extends B>, List<EventProcessor<? super B>>> preComputedPool);
    }
}
