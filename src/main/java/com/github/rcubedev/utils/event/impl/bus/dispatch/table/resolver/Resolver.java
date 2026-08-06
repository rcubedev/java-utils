package com.github.rcubedev.utils.event.impl.bus.dispatch.table.resolver;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Strategy for resolving an execution chain when an unregistered
 * or unmapped event type is dispatched.
 */
@FunctionalInterface
public interface Resolver<B extends Event> {

    @NotNull List<EventProcessor<? super B>> resolve(@NotNull Class<?> unregisteredType);
}
