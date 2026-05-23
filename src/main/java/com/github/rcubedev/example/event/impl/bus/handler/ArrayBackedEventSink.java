package com.github.rcubedev.example.event.impl.bus.handler;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.spi.Subscription;

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
// fixme add pub api
// todo maybe add the invokerFactory stuff again which gives a EventProcessor<E>[] and returns EventProcessor<E>; merges them
public final class ArrayBackedEventSink<E extends Event> {

    private final Class<E> eventType;
    private final Priority priority;
    private final Object lock = new Object();

    private record RegisteredProcessor<E extends Event>(EventProcessor<E> processor, Subscription sub) {}

    @SuppressWarnings("unchecked")
    private RegisteredProcessor<E>[] listeners = (RegisteredProcessor<E>[]) new RegisteredProcessor<?>[0];

    private volatile EventProcessor<E> invoker = event -> {};

    public ArrayBackedEventSink(Class<E> eventType, Priority priority) {
        this.eventType = eventType;
        this.priority = priority;
    }

    /**
     * Add a listener and rebuild the merged invoker.
     */
    public void addListener(EventProcessor<E> listener, Subscription subscription) {
        synchronized (lock) {
            @SuppressWarnings("unchecked")
            RegisteredProcessor<E>[] newArray = (RegisteredProcessor<E>[]) new RegisteredProcessor<?>[listeners.length + 1];
            System.arraycopy(listeners, 0, newArray, 0, listeners.length);

            newArray[listeners.length] = new RegisteredProcessor<>(listener, subscription);
            listeners = newArray;
            rebuildInvoker();
        }
    }

    public boolean removeListener(Subscription subscription) {
        synchronized (lock) {
            int index = -1;
            for (int i = 0; i < listeners.length; i++) {
                if (listeners[i].sub() == subscription) {
                    index = i;
                    break;
                }
            }
            if (index == -1) return false;

            @SuppressWarnings("unchecked")
            RegisteredProcessor<E>[] newArray = (RegisteredProcessor<E>[]) new RegisteredProcessor[listeners.length - 1];
            System.arraycopy(listeners, 0, newArray, 0, index);
            if (index < listeners.length - 1) {
                System.arraycopy(listeners, index + 1, newArray, index, listeners.length - index - 1);
            }
            listeners = newArray;
            rebuildInvoker();
            return true;
        }
    }

    /**
     * Rebuild the merged invoker from all registered listeners.
     * <p>
     * Called after every {@link #addListener}.
     */
    private void rebuildInvoker() { // use invoker factory later
        RegisteredProcessor<E>[] builder = listeners;
        invoker = switch (builder.length) {
            case 0 -> event -> {};
            case 1 -> builder[0].processor();
            default -> {
                @SuppressWarnings("unchecked")
                EventProcessor<E>[] snapshot = new EventProcessor[builder.length];
                for (int i = 0; i < builder.length; i++) {
                    snapshot[i] = builder[i].processor();
                }

                yield event -> {
                    for (EventProcessor<E> l : snapshot) l.process(event);
                };
            }
        };
    }

    /**
     * Clear all listeners and reset the invoker.
     */
    @SuppressWarnings("unchecked")
    public void clear() {
        synchronized (lock) {
            listeners = (RegisteredProcessor<E>[]) new RegisteredProcessor[0];
            rebuildInvoker();
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

    public EventSinkSnapshot<E> snapshot() {
        return new EventSinkSnapshot<>(
                eventType,
                priority,
                invoker
        );
    }
}