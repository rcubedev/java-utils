package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventHandler;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.google.common.collect.MapMaker;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Internal implementation for array-backed events.
 */
public final class EventHandlerFactoryImpl {

    private static final Set<ArrayBackedEventHandler<?>> EVENT_HANDLERS = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
    private static final Map<Class<? extends Event>, ArrayBackedEventHandler<?>> HANDLER_REGISTRY = Collections.synchronizedMap(new MapMaker().weakKeys().makeMap());
    private static final Object lock = new Object();

    private EventHandlerFactoryImpl() {}

    public static void invalidate() {
        EVENT_HANDLERS.forEach(ArrayBackedEventHandler::update);
    }

    /**
     * Create an array-backed event handler instance.
     */
    public static <E extends Event> EventHandler<E> createArrayBacked(Class<E> eventType, Function<EventProcessor<E>[], EventProcessor<E>> invokerFactory) {
        @SuppressWarnings("unchecked")
        ArrayBackedEventHandler<E> handler = new ArrayBackedEventHandler<>(eventType, (Class<EventProcessor<E>>) (Class<?>) EventProcessor.class, invokerFactory);

        synchronized (lock) {
            // Find parent handlers and register this handler with them
            List<Class<? extends Event>> hierarchy = EventHandlerInheritanceRegistry.getEventHierarchy(eventType);

            // Skip first element (the event type itself), look for parents
            for (int i = 1; i < hierarchy.size(); i++) {
                Class<? extends Event> parentType = hierarchy.get(i);
                @SuppressWarnings("unchecked")
                ArrayBackedEventHandler<? super E> parentHandler = (ArrayBackedEventHandler<? super E>) HANDLER_REGISTRY.get(parentType);

                if (parentHandler != null) {
                    handler.addParentHandler(parentHandler);
                }
            }

            HANDLER_REGISTRY.put(eventType, handler);
        }

        EVENT_HANDLERS.add(handler);
        return handler;
    }
}