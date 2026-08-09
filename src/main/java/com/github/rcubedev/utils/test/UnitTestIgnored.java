package com.github.rcubedev.utils.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a program element as intentionally ignored by unit test coverage.
 * <p>
 * Elements annotated with {@link UnitTestIgnored @UnitTestIgnored} will have
 * {@code @Generated} applied to their bytecode at compile time for unit tests,
 * causing JaCoCo to exclude them from coverage reports.
 * <p>
 * This annotation is intended for elements that are impractical to unit test in
 * isolation.
 *
 * <p><b>Retention:</b> {@link RetentionPolicy#CLASS}. Present in bytecode
 * but not available at runtime via reflection.
 *
 * <p><b>Usage:</b>
 * <pre>{@code
 * @UnitTestIgnored
 * public MyService(RealDependencyA a, RealDependencyB b) {
 *     this.a = a;
 *     this.b = b;
 * }
 * }</pre>
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.PACKAGE, ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR, ElementType.FIELD,
        ElementType.PARAMETER})
public @interface UnitTestIgnored {
    // todo add reason
}
