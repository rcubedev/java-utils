package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventBusBuilder.EventBusConfig;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Identity;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.hooks.AfterDispatchHook;
import com.github.rcubedev.example.event.api.hooks.BeforeDispatchHook;
import com.github.rcubedev.example.event.api.hooks.ErrorHandler;
import com.github.rcubedev.example.event.api.spi.RecursionBypass;
import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.api.spi.IEventBus;
import com.github.rcubedev.example.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;

/**
 * A wrapper for {@link IEventBus} that applies hooks and
 * error handling around event dispatch.
 *
 * @param <B> The base event type this bus accepts
 */
@UnitTestIgnored
public final class HookedEventBus<B extends Event> implements IEventBus<B> {

    private final IEventBus<B> delegate;
    private final EventBusConfig<B> config;

    public HookedEventBus(@NotNull IEventBus<B> delegate, @NotNull EventBusConfig<B> config) {
        this.delegate = delegate;
        this.config = config;
    }

    @Override
    public <E extends B> void post(@NotNull E event) {
        BeforeDispatchHook<B> before = config.before();
        if (before != null) before.beforeDispatch(event);

        try {
            delegate.post(event);
        } catch (Throwable t) {
            ErrorHandler<B> errorHandler = config.error();
            if (errorHandler != null) errorHandler.handle(event, t);
            else throw t;
        } finally {
            AfterDispatchHook<B> after = config.after();
            if (after != null) after.afterDispatch(event);
        }
    }

    @Override
    public @NotNull RecursionBypass openBypassTo(int extraBudget) {
        return delegate.openBypassTo(extraBudget);
    }

    @Override
    public @NotNull <E extends B> Subscription register(Class<E> eventType, Priority priority, EventProcessor<E> listener, Identity identity) {
        return delegate.register(eventType, priority, listener, identity);
    }

    @Override
    public @NotNull Subscription register(Object target, Identity identity) {
        return delegate.register(target, identity);
    }

    @Override
    public @NotNull Class<B> getBusType() {
        return delegate.getBusType();
    }
}
