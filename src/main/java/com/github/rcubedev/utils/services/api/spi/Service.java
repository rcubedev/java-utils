package com.github.rcubedev.utils.services.api.spi;

import java.util.function.Supplier;

public interface Service<S> extends Supplier<S> {

    /**
     * Returns the provider type. The {@link #get()} method should be used
     * to obtain the provider instance.
     * <p>
     * For lazy loaded services, when a module declares that the provider class is created by a
     * provider factory then this method returns the return type of its
     * public static {@code provider()} method.
     * <p>
     * For {@link Eager} services, the provider has already been instantiated so this
     * method returns the actual runtime class of the provider instance.
     *
     * @return The provider type
     */
    Class<? extends S> type();

    /**
     * Returns an instance of the provider.
     *
     * @return An instance of the provider.
     */
    @Override
    S get();
}
