package com.github.rcubedev.example.event.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that the listener should be stored using a {@link java.lang.ref.WeakReference WeakReference}.
 * <p>
 * The listener may stop receiving events once it has been garbage collected.
 * The timing of this depends on the JVM garbage collector and when the reference is cleared.
 * <p>
 * {@link com.github.rcubedev.example.event.api.spi.IEventBus IEventBus}
 * implementations may remove cleared weak references when they are detected.
 * Removal of a listener may occur during event dispatch when a cleared reference is encountered.
 * <p>
 * This helps reduce memory leaks by allowing listeners to be collected when no
 * longer strongly referenced elsewhere.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Weak {
}
