package com.github.rcubedev.utils.event.api.annotation;

import com.github.rcubedev.utils.event.generated.EventSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.SubscriberInvokerFactory;

import java.lang.annotation.*;

/**
 * Opt-in annotation marking an event listener class for compile-time code generation.
 * <p>
 * When present on a class, the annotation processor generates {@link EventSubscriberInvoker}s for
 * and a corresponding {@link SubscriberInvokerFactory} at compile time for all
 * {@link SubscribeEvent @SubscribeEvent} methods.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface CompiledEventHandlers {
    //fixme lets move it into api pkg somewhere but not directly sm sub pkg
}
