package com.github.rcubedev.utils.event.api.annotation;

import java.lang.annotation.*;

/**
 * Indicates that the listener should be stored using a {@link java.lang.ref.WeakReference WeakReference}.
 * <p>
 * The listener may stop receiving events once it has been garbage collected.<br>
 * The timing of this depends on the JVM garbage collector and when the reference is cleared.
 * <p>
 * {@link com.github.rcubedev.utils.event.api.spi.IEventBus IEventBus}
 * implementations may remove cleared weak references when they are detected.<br>
 * Removal of a listener may occur during event dispatch when a cleared reference is encountered.
 * <p>
 * This helps reduce memory leaks by allowing listeners to be collected when no
 * longer strongly referenced elsewhere.
 */
//todo disallow on static event handler methods/static listener classes on runtime reflection side
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Weak {
}
