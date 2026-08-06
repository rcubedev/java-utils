package com.github.rcubedev.example.event.api;

import com.github.rcubedev.example.event.api.hooks.AfterDispatchHook;
import com.github.rcubedev.example.event.api.hooks.BeforeDispatchHook;
import com.github.rcubedev.example.event.api.hooks.ErrorHandler;
import com.github.rcubedev.example.event.api.spi.IEventBus;
import com.github.rcubedev.example.event.impl.HookedEventBus;
import com.github.rcubedev.example.event.impl.bus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A builder for creating {@link IEventBus} instances.
 * <p>
 * This builder allows for configuration of an event bus, including
 * lifecycle hooks, error handling, and recursion safety limits.
 * <p>
 * <b>Example Usage:</b>
 * <pre>{@code
 * IEventBus<MyBaseEvent> bus = EventBusBuilder.builder(MyBaseEvent.class)
 *         .maxDepth(64)
 *         .beforeDispatch(event -> System.out.println("Dispatching: " + event))
 *         .errorHandler((event, error) -> logger.error("Failed to post " + event.getClass().getName(), error))
 *         .build();
 * }</pre>
 *
 * @param <B> The base event type for the bus. All events posted to the resulting
 * bus must be a subclass of this type.
 * @see IEventBus
 */
public final class EventBusBuilder<B extends Event> {

    private final @NotNull Class<B> busType;
    private @Nullable BeforeDispatchHook<B> beforeDispatch;
    private @Nullable AfterDispatchHook<B> afterDispatch;
    private @Nullable ErrorHandler<B> errorHandler;
    private boolean global = true;
    private int maxDepth = 128;

    private EventBusBuilder(@NotNull Class<B> busType) {
        this.busType = busType;
    }

    /**
     * Creates a new builder for an event bus handling the specified base type.
     *
     * @param busType the class of the base event
     * @param <B> the base event type
     * @return a new builder instance
     */
    public static <B extends Event> @NotNull EventBusBuilder<B> builder(@NotNull Class<B> busType) {
        return new EventBusBuilder<>(busType);
    }

    /**
     * Create a standard event bus with default settings.
     *
     * @param busType the class of the base event
     * @param <B> the base event type
     * @return a default IEventBus instance
     */
    public static <B extends Event> @NotNull IEventBus<B> create(@NotNull Class<B> busType) {
        return builder(busType).build();
    }

    /**
     * Sets a hook to be executed immediately before an event is dispatched to listeners.
     *
     * @param hook the pre-dispatch hook
     * @return this builder
     */
    public @NotNull EventBusBuilder<B> beforeDispatch(@NotNull BeforeDispatchHook<B> hook) {
        this.beforeDispatch = hook;
        return this;
    }

    /**
     * Sets a hook to be executed immediately after an event has been dispatched.
     * <p>
     * This hook runs even if the dispatch threw an exception (inside a finally block).
     *
     * @param hook the post-dispatch consumer
     * @return this builder
     */
    public @NotNull EventBusBuilder<B> afterDispatch(@NotNull AfterDispatchHook<B> hook) {
        this.afterDispatch = hook;
        return this;
    }

    /**
     * Sets a custom error handler to manage exceptions thrown during event dispatch.
     * <p>
     * If no handler is set, exceptions will propagate up the stack normally.
     *
     * @param handler the error consumer, receiving the event and the thrown exception
     * @return this builder
     */
    public @NotNull EventBusBuilder<B> errorHandler(@NotNull ErrorHandler<B> handler) {
        this.errorHandler = handler;
        return this;
    }

    /**
     * Whether the bus should be registered to {@link EventBusRegistry}
     * <p>
     * Default: {@code true}
     *
     * @param global if the bus should be registered
     * @return this builder
     */
    public @NotNull EventBusBuilder<B> global(boolean global) {
        this.global = global;
        return this;
    }

    /**
     * Sets the maximum recursion depth for event posting to prevent {@link StackOverflowError}
     * from circular event logic.
     * <p>
     * Default: {@code 128}
     *
     * @param depth the maximum allowed depth
     * @return this builder
     * @throws IllegalArgumentException if depth is less than or equal to 0
     */
    public @NotNull EventBusBuilder<B> maxDepth(int depth) {
        if (depth <= 0) throw new IllegalArgumentException("Max depth must be positive");
        this.maxDepth = depth;
        return this;
    }

    /**
     * Constructs the event bus based on the current builder configuration.
     *
     * @return the configured {@link IEventBus} instance
     */
    public @NotNull IEventBus<B> build() {
        EventBusConfig<B> config = new EventBusConfig<>(beforeDispatch, afterDispatch, errorHandler, global, maxDepth);

        EventBus<B> impl = new EventBus<>(busType, maxDepth);
        if (!config.hasHooks()) return register(impl);

        // Wrap the implementation with the hooks
        return register(new HookedEventBus<>(impl, config));
    }

    private <E extends Event, T extends IEventBus<E>> T register(T bus) {
        if (global) EventBusRegistry.getInstance().register(bus);
        return bus;
    }

    /**
     * An immutable snapshot of the event bus configuration.
     *
     * @param before the pre-dispatch hook, or null
     * @param after the post-dispatch hook, or null
     * @param error the error handler, or null
     * @param maxDepth the maximum recursion depth
     * @param <B> the base event type
     */
    public record EventBusConfig<B extends Event>(@Nullable BeforeDispatchHook<B> before, @Nullable AfterDispatchHook<B> after,
                                                  @Nullable ErrorHandler<B> error, boolean global, int maxDepth) {
        public boolean hasHooks() {
            return before != null || after != null || error != null;
        }
    }
}