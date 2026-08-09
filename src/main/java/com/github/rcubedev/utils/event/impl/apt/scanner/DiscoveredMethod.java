package com.github.rcubedev.utils.event.impl.apt.scanner;

import com.github.rcubedev.utils.event.api.annotation.Weak;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

public record DiscoveredMethod(TypeElement listenerClass, ExecutableElement method, boolean isWeak) {
    public DiscoveredMethod(TypeElement listenerClass, ExecutableElement method) {
        this(listenerClass, method,
                method.getAnnotation(Weak.class) != null || listenerClass.getAnnotation(Weak.class) != null);
    }
}
