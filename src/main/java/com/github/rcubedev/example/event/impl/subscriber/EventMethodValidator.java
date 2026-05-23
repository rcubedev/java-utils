package com.github.rcubedev.example.event.impl.subscriber;

import com.github.rcubedev.example.event.api.Event;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class EventMethodValidator<B extends Event> implements MethodValidator<B> {

    private final Class<B> eventType;

    public EventMethodValidator(Class<B> eventType) {
        this.eventType = eventType;
    }

    public <E extends B> Class<E> validate(Method method) throws IllegalArgumentException {
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException("@SubscribeEvent method must be public: " + method);
        }
        if (method.getParameterCount() != 1) {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent annotation. " +
                            "It has " + method.getParameterCount() + " arguments, " +
                            "but event handler methods require a single argument only.");
        }

        Class<?> paramType = method.getParameterTypes()[0];

        if (!Event.class.isAssignableFrom(paramType)) {
            throw new IllegalArgumentException(
                    "Method " + method + " has @SubscribeEvent but parameter is not an Event subtype: " + paramType);
        }

        // todo: this wasn't here before it just no-opd
        if (!eventType.isAssignableFrom(paramType)) {
            throw new IllegalArgumentException(
                    "Method " + method + "'s parameter is incompatible with type: " + eventType.getName());
        }

        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("@SubscribeEvent method must return void: " + method); // fixme what if i add something where event can decide w/o using event.setsomething & instead return type
        }

        @SuppressWarnings("unchecked") // safe as checked if paramType is a subtype of eventType
        Class<E> eventType = (Class<E>) paramType;
        return eventType;
    }

    @Override
    public boolean isCompatible(Method method) {
        if (method.getParameterCount() != 1) return false;
        Class<?> paramType = method.getParameterTypes()[0];
        return eventType.isAssignableFrom(paramType);
    }
}
