package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * Holds all registered listeners for a single event type at a single priority,
 * merged into one pre-computed {@link EventProcessor} invoker.
 * <p>
 * The bus reads {@link #eventType()}, {@link #priority()}, and {@link #invoker()}
 * when rebuilding the flat dispatch array. The internal {@code listeners} array
 * is never accessed by the bus directly.
 *
 * @param <E> The event type
 */
public final class ArrayBackedEventHandler<E extends Event> {

    private final Class<E> eventType;
    private final Priority priority;
    private final Object lock = new Object();

    @SuppressWarnings("unchecked")
    private EventProcessor<E>[] listeners = (EventProcessor<E>[]) new EventProcessor[0];

    private volatile EventProcessor<E> invoker = event -> {};

    public ArrayBackedEventHandler(Class<E> eventType, Priority priority) {
        this.eventType = eventType;
        this.priority = priority;
    }

    /**
     * Add a listener and rebuild the merged invoker.
     */
    public void addListener(EventProcessor<E> listener) {
        synchronized (lock) {
            @SuppressWarnings("unchecked")
            EventProcessor<E>[] newArray = (EventProcessor<E>[]) new EventProcessor[listeners.length + 1];
            System.arraycopy(listeners, 0, newArray, 0, listeners.length);
            newArray[listeners.length] = listener;
            listeners = newArray;
            rebuildInvoker();
        }
    }

    /**
     * Rebuild the merged invoker from all registered listeners.
     * Called after every {@link #addListener}.
     */
    private void rebuildInvoker() { // use invoker factory later
        EventProcessor<E>[] snapshot = listeners;
        invoker = switch (snapshot.length) {
            case 0 -> event -> {};
            case 1 -> snapshot[0];
            default -> event -> {
                for (EventProcessor<E> l : snapshot) l.process(event);
            };
        };
    }

    /**
     * Clear all listeners and reset the invoker.
     */
    @SuppressWarnings("unchecked")
    public void clear() {
        synchronized (lock) {
            listeners = (EventProcessor<E>[]) new EventProcessor[0];
            invoker = event -> {};
        }
    }

    /**
     * The single merged invoker. Calls all registered listeners in registration order.
     */
    public EventProcessor<E> invoker() {
        return invoker;
    }

    public Class<E> eventType() {
        return eventType;
    }

    public Priority priority() {
        return priority;
    }
}