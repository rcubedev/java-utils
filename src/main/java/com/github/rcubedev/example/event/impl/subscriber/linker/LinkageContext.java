package com.github.rcubedev.example.event.impl.subscriber.linker;

import com.github.rcubedev.example.event.api.Event;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public record LinkageContext<E extends Event>(MethodHandles.Lookup lookup, Method method, Class<E> paramType) {
    public Class<?> targetClass() {
        return method.getDeclaringClass();
    }
}