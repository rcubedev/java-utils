package com.github.rcubedev.utils.event.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as an event listener.
 * The method must be public, take exactly one {@link Event} subtype parameter, and return void.
 * <pre>
 * {@code
 * class MyListener {
 *     @SubscribeEvent(priority = Priority.HIGH)
 *     public void onLogin(PlayerLoginEvent event) {
 *         // handle event
 *     }
 * }
 *
 * MainEventBus.INSTANCE.register(new MyListener());
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SubscribeEvent {

    /**
     * The {@link Priority} of this listener.
     */
    Priority priority() default Priority.NORMAL;

    /**
     * If true, this listener will not be called for cancelled events.
     */
    boolean ignoreCancelled() default false;
}