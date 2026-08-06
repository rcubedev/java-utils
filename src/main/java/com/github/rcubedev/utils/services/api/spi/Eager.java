package com.github.rcubedev.utils.services.api.spi;

public interface Eager {
    /**
     * @return true if the required condition is met.
     */
    boolean isAvailable();
}