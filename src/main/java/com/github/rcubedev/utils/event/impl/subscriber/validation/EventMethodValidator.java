package com.github.rcubedev.utils.event.impl.subscriber.validation;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.annotation.Weak;
import com.github.rcubedev.utils.event.api.subscriber.validation.MethodValidator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class EventMethodValidator<E extends Event> implements MethodValidator<E> {

    private final Class<E> eventType;

    public EventMethodValidator(Class<E> eventType) {
        this.eventType = eventType;
    }

    @Override
    public Class<? extends E> validate(Method method) throws IllegalArgumentException {
        if (!Modifier.isPublic(method.getModifiers())) {
            throw new IllegalArgumentException("@SubscribeEvent method must be public: " + method);
        }
        boolean isWeak = method.isAnnotationPresent(Weak.class) || method.getDeclaringClass().isAnnotationPresent(Weak.class);
        if (Modifier.isStatic(method.getModifiers()) && isWeak) {
            throw new IllegalArgumentException("@Weak cannot be applied to static event handler methods or its declaring class: " + method);
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

        // this wasn't here before it just no-opd
        if (!eventType.isAssignableFrom(paramType)) {
            throw new IllegalArgumentException(
                    "Method " + method + "'s parameter is incompatible with type: " + eventType.getName());
        }

        if (method.getReturnType() != void.class) {
            throw new IllegalArgumentException("@SubscribeEvent method must return void: " + method); // fixme what if i add something where event can decide w/o using event.setsomething & instead return type
        }

        @SuppressWarnings("unchecked") // safe as checked if paramType is a subtype of eventType
        Class<? extends E> eventType = (Class<? extends E>) paramType;
        return eventType;
    }

    // decoupled so other has method in cause
    @Override
    public Class<? extends E> validateParameter(Class<?> paramType) {
        if (!Event.class.isAssignableFrom(paramType)) {
            throw new IllegalArgumentException(
                    "Method has @SubscribeEvent but parameter " + paramType.getName() + " is not an Event subtype: " + paramType);
        }

        if (!eventType.isAssignableFrom(paramType)) {
            throw new IllegalArgumentException(
                    "Parameter " + paramType.getName() + " is incompatible with type: " + eventType.getName());
        }

        @SuppressWarnings("unchecked") // safe as checked if paramType is a subtype of eventType
        Class<? extends E> eventType = (Class<? extends E>) paramType;
        return eventType;
    }

    @Override
    public boolean isCompatible(Method method) {
        if (method.getParameterCount() != 1) return false;
        Class<?> paramType = method.getParameterTypes()[0];
        return eventType.isAssignableFrom(paramType);
    }
}
