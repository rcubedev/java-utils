package com.github.rcubedev.example.event.api;

import com.github.rcubedev.example.event.impl.EventHandlerFactoryImpl;

import java.util.function.Function;

public final class EventHandlerFactory {

    private EventHandlerFactory() {}


    /**
     * Create an "array-backed" {@link EventHandler}.
     * <p>
     * If your factory simply delegates to the listeners without adding custom behavior,
     * consider using {@linkplain #createArrayBacked(Class, EventProcessor, Function) the other overload}
     * if performance of this event is critical.
     *
     * @param type           The listener class type.
     * @param invokerFactory The invoker factory, combining multiple listeners into one instance.
     * @param <E>            The event type.
     * @return The {@link EventHandler}.
     */
    public static <E extends Event> EventHandler<E> createArrayBacked(Class<E> type, Function<EventProcessor<E>[], EventProcessor<E>> invokerFactory) {
        return EventHandlerFactoryImpl.createArrayBacked(type, invokerFactory);
    }

    public static <E extends Event> EventHandler<E> createArrayBacked(Class<E> type) {
        return EventHandlerFactory.createArrayBacked(type, e -> {}, listeners -> event -> {
            for (EventProcessor<E> eventProcessor : listeners) {
                eventProcessor.process(event);
            }
        });
    }

    /**
     * Create an "array-backed" {@link EventHandler} with a custom empty invoker,
     * for an event whose {@code invokerFactory} only delegates to the listeners.
     * <ul>
     *   <li>If there is no listener, the custom empty invoker will be used.</li>
     *   <li><b>If there is only one listener, that one will be used as the invoker
     *   and the factory will not be called.</b></li>
     *   <li>Only when there are at least two listeners will the factory be used.</li>
     * </ul>
     *
     * <p>Having a custom empty invoker (of type (...) -&gt; {}) increases performance
     * relative to iterating over an empty array; however, it only really matters
     * if the event is executed thousands of times a second.
     *
     * @param type           The listener class type.
     * @param emptyInvoker   The custom empty invoker.
     * @param invokerFactory The invoker factory, combining multiple listeners into one instance.
     * @param <E>            The event type.
     * @return The {@link EventHandler} instance.
     */
    public static <E extends Event> EventHandler<E> createArrayBacked(Class<E> type, EventProcessor<E> emptyInvoker, Function<EventProcessor<E>[], EventProcessor<E>> invokerFactory) {
        return createArrayBacked(type, listeners -> {
            if (listeners.length == 0) {
                return emptyInvoker;
            } else if (listeners.length == 1) {
                return listeners[0];
            } else {
                return invokerFactory.apply(listeners);
            }
        });
    }
}