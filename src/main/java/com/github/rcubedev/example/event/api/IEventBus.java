package com.github.rcubedev.example.event.api;

/**
 * Interface for event buses typed to a specific {@link Event} subclass {@code B}.
 * <p>
 * Create a named bus by extending {@link EventBus}:
 * <pre>
 * {@code
 * // The bus marker — all events on this bus extend this
 * public abstract class MainBusEvent extends Event {}
 *
 * // The singleton bus
 * public final class MainEventBus extends EventBus<MainBusEvent> {
 *     public static final MainEventBus INSTANCE = new MainEventBus();
 *     private MainEventBus() { super(MainBusEvent.class); }
 * }
 *
 * // An event on this bus
 * public class PlayerLoginEvent extends MainBusEvent {}
 * }
 * </pre>
 *
 * @param <B> The base event type this bus accepts
 */
public interface IEventBus<B extends Event> {

    /**
     * Post an event to this bus.
     * Compile-time safe — only subtypes of {@code B} are accepted.
     */
    <E extends B> void post(E event);

    /**
     * Register a direct {@link EventProcessor} for the given event type at {@link Priority#NORMAL}.
     */
    <E extends B> void register(Class<E> eventType, EventProcessor<E> listener);

    /**
     * Register a direct {@link EventProcessor} for the given event type at a specific priority.
     */
    <E extends B> void register(Class<E> eventType, Priority priority, EventProcessor<E> listener);

    /**
     * Register a listener instance or {@link Class} with {@link SubscribeEvent @SubscribeEvent} methods.
     * Only methods whose parameter type is a subtype of {@code B} will be registered.
     *
     * @param target Listener instance or {@link Class} for static methods
     * @throws IllegalArgumentException if no valid {@link SubscribeEvent @SubscribeEvent} methods found
     */
    void register(Object target);

    /**
     * Get the base event type this bus accepts.
     */
    Class<B> getBusType();

    /**
     * Reset all listeners on this bus. Useful for testing.
     */
    void resetListeners();
}