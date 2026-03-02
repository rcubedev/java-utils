package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventHandler;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.SubscribeEvent;
import com.google.common.collect.MapMaker;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Internal implementation for array-backed events.
 */
public final class EventHandlerFactoryImpl {

    private static final Set<ArrayBackedEventHandler<?>> EVENT_HANDLERS = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
    private static final Map<Class<? extends Event>, ArrayBackedEventHandler<?>> HANDLER_REGISTRY = Collections.synchronizedMap(new MapMaker().weakKeys().makeMap());
    private static final Set<Consumer<ArrayBackedEventHandler<?>>> GLOBAL_LISTENERS = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
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
        GLOBAL_LISTENERS.forEach(entry -> entry.accept(handler));
        return handler;
    }

    public static void registerGlobal(Object target) {
        Consumer<ArrayBackedEventHandler<?>> entry;
        Class<?> clazz = target.getClass();
        if (clazz == Method.class) {
            Method method = (Method) target;
            entry = handler -> {
                if (method.getParameterCount() != 1) {
                    handler.register(method); // let it fail
                    return;
                }
                Class<?> paramType = method.getParameterTypes()[0];
                if (paramType == handler.getEventType()) {
                    handler.register(method);
                } else if (!Event.class.isAssignableFrom(paramType)) {
                    handler.register(method); // intentionally try register to throw appropriate ex
                }
            };
        } else {
            // Force-load event classes from @SubscribeEvent methods to trigger their static initializers
            for (Method m : clazz.getDeclaredMethods()) {
                if (!m.isAnnotationPresent(SubscribeEvent.class)) continue;
                if (m.getParameterCount() == 1 && Event.class.isAssignableFrom(m.getParameterTypes()[0])) {
                    try {
                        Class.forName(m.getParameterTypes()[0].getName());
                    } catch (ClassNotFoundException ignored) {}
                }
            }

            boolean hasAnySubscribeEvent = Arrays.stream(clazz.getDeclaredMethods())
                    .anyMatch(m -> m.isAnnotationPresent(SubscribeEvent.class));
            entry = handler -> {
                if (!hasAnySubscribeEvent) {
                    handler.register(target); // no @SubscribeEvent at all, let it throw
                    return;
                }
                Method[] methods = clazz.getDeclaredMethods();
                boolean match = false;
                for (Method m : methods) {
                    if (!m.isAnnotationPresent(SubscribeEvent.class)) continue;
                    if (m.getParameterCount() != 1) {
                        match = true;
                        break; // let it throw
                    }
                    Class<?> paramType = m.getParameterTypes()[0];
                    if (paramType == handler.getEventType()) {
                        match = true;
                        break;
                    } else if (!Event.class.isAssignableFrom(paramType)) {
                        match = true; // let it throw
                        break;
                    }
                }
                if (match) {
                    handler.register(target); // let EventSubscriberHandler handle validation and matching
                }
            };
        }
        GLOBAL_LISTENERS.add(entry);
        EVENT_HANDLERS.forEach(entry);
    }

    public static void resetListeners() {
        GLOBAL_LISTENERS.clear();
        EVENT_HANDLERS.forEach(ArrayBackedEventHandler::clearListeners);
    }
}