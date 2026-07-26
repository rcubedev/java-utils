package com.github.rcubedev.example.services.api.spi;

public interface Eager {
    /**
     * @return true if the required condition is met.
     */
    boolean isAvailable();
}