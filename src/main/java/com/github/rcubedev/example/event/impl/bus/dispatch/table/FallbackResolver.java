package com.github.rcubedev.example.event.impl.bus.dispatch.table;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import org.jetbrains.annotations.NotNull;

/**
 * Strategy for resolving an execution chain when an unregistered
 * or unmapped event type is dispatched.
 */
@FunctionalInterface
public interface FallbackResolver<B extends Event> {

    @NotNull EventProcessor<? super B>[] resolve(@NotNull Class<?> unregisteredType);
}