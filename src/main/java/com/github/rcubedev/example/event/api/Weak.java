package com.github.rcubedev.example.event.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the listener should be stored using a WeakReference.
 * <p>
 * The listener will be automatically unregistered when the target object
 * is garbage collected.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Weak {
}
