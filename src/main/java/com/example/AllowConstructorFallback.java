package com.example;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a configuration value type as intentionally allowing constructor
 * fallback during reflective instantiation.
 * <p>
 * When present, the configuration system will suppress warnings that would
 * normally be logged if a preferred constructor signature is missing and a
 * fallback constructor is used instead.
 * <p>
 * This annotation does not change behavior on its own.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AllowConstructorFallback {
}
