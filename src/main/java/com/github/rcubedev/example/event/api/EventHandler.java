package com.github.rcubedev.example.event.api;

import com.github.rcubedev.example.event.impl.EventHandlerFactoryImpl;

/**
 * Base class for event handlers.
 */
public abstract class EventHandler<E extends Event> {

    // /**
    //  * Register a global listener to the event ({@link E}), for the specified priority
    //  *
    //  * @param eventType the event {@linkplain Class class}
    //  * @param priority the priority of the {@code listener}
    //  * @param listener the desired listener.
    //  * @param <E> the event type to globally register to
    //  */
    // public static <E extends Event> void registerGlobal(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
    //     EventHandlerFactoryImpl.registerGlobal(eventType, priority, listener);
    // }
    //
    // /**
    //  * Register a global listener to the event ({@link E}), in the normal {@link Priority}.
    //  *
    //  * @param eventType the event {@linkplain Class class}
    //  * @param listener the desired listener.
    //  * @param <E> the event type to globally register to
    //  */
    // public static <E extends Event> void registerGlobal(Class<E> eventType, EventProcessor<E> listener) {
    //     registerGlobal(eventType, Priority.NORMAL, listener);
    // }

    /**
     * Register a global listener class or instance with {@link SubscribeEvent @SubscribeEvent} methods.
     * If the class has static {@link SubscribeEvent @SubscribeEvent} methods, pass the {@link Class}.
     * If the instance has instance {@link SubscribeEvent @SubscribeEvent} methods, pass the instance.
     * Can also pass a {@link java.lang.reflect.Method Method} if it's static and annotated with {@link SubscribeEvent @SubscribeEvent}.
     *
     * @param target A {@link Class}, {@link Object instance}, or {@link java.lang.reflect.Method Method}
     *               with {@link SubscribeEvent @SubscribeEvent} methods
     * @throws IllegalArgumentException if no {@link SubscribeEvent @SubscribeEvent} methods found or invalid signature
     */
    public static void registerGlobal(Object target) {
        EventHandlerFactoryImpl.registerGlobal(target);
    }

    /**
     * Register a listener to the event ({@link E}), for the specified priority
     *
     * @param priority the priority of the {@code listener}
     * @param listener the desired listener.
     */
    public abstract void register(Priority priority, EventProcessor<E> listener);

    /**
     * Register a listener to the event ({@link E}), in the normal {@link Priority}.
     *
     * @param listener the desired listener.
     */
    public abstract void register(EventProcessor<E> listener);

    /**
     * Register a listener class or instance with {@link SubscribeEvent @SubscribeEvent} methods.
     * If the class has static {@link SubscribeEvent @SubscribeEvent} methods, pass the {@link Class}.
     * If the instance has instance {@link SubscribeEvent @SubscribeEvent} methods, pass the instance.
     * Can also pass a {@link java.lang.reflect.Method Method} if it's static and annotated with {@link SubscribeEvent @SubscribeEvent}.
     *
     * @param target A {@link Class}, {@link Object instance}, or {@link java.lang.reflect.Method Method}
     *               with {@link SubscribeEvent @SubscribeEvent} methods
     * @throws IllegalArgumentException if no {@link SubscribeEvent @SubscribeEvent} methods found or invalid signature
     */
    public abstract void register(Object target);

    /**
     * Get the invoker for this event handler.
     *
     * @return the invoker processor
     */
    public abstract EventProcessor<E> invoker();

    /**
     * Get the event type this handler manages.
     */
    public abstract Class<E> getEventType();
}