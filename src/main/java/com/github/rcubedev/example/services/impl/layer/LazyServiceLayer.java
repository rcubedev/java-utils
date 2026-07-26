package com.github.rcubedev.example.services.impl.layer;

import com.github.rcubedev.example.services.api.exception.ServiceLayerException;
import com.github.rcubedev.example.services.api.spi.Service;
import com.github.rcubedev.example.services.api.spi.ServiceLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A {@link ServiceLayer} that defers evaluation of its backing supplier until
 * the first call to {@link #find} or {@link #findAll}, then caches the result.
 */
public final class LazyServiceLayer implements ServiceLayer {

    private final Supplier<ServiceLayer> supplier;
    private final Object lock = new Object();
    private volatile ServiceLayer delegate = null;

    public LazyServiceLayer(@NotNull Supplier<ServiceLayer> supplier) {
        this.supplier = supplier;
    }

    private ServiceLayer delegate() {
        ServiceLayer d = this.delegate;
        if (d != null) return d;

        synchronized (lock) {
            d = this.delegate;
            if (d != null) return d;
            ServiceLayer supplied = this.supplier.get();
            if (supplied == null) throw new ServiceLayerException("ServiceLayer supplier returned null");
            return this.delegate = supplied;
        }
    }

    @Override
    public @NotNull String name() {
        return delegate().name();
    }

    @Override
    public <S> @NotNull Optional<Service<S>> find(@NotNull Class<S> contract) {
        return delegate().find(contract);
    }

    @Override
    public <S> @NotNull @Unmodifiable List<Service<S>> findAll(@NotNull Class<S> contract) {
        return delegate().findAll(contract);
    }

    @Override
    public int priority() {
        return delegate().priority();
    }
}