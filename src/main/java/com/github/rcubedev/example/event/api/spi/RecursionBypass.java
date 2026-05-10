package com.github.rcubedev.example.event.api.spi;


/**
 * A handle used to temporarily disable the recursion guard.
 * <p>
 * Must be used within a try-with-resources block.
 */
@FunctionalInterface
public interface RecursionBypass extends AutoCloseable {

    @Override
    void close();
}