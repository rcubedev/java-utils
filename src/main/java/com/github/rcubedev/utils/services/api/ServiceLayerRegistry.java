package com.github.rcubedev.utils.services.api;

import com.github.rcubedev.utils.registry.api.Registry;
import com.github.rcubedev.utils.registry.impl.SimpleRegistry;
import com.github.rcubedev.utils.services.api.spi.ServiceLayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.function.Supplier;

public final class ServiceLayerRegistry {

    private final Registry<Supplier<ServiceLayer>> delegate = new SimpleRegistry<>("service-layers");

    public void register(@NotNull Supplier<ServiceLayer> factory) {
        this.delegate.register(factory);
    }

    public void freeze() {
        this.delegate.freeze();
    }

    /**
     * Evaluates all registered factories and returns the flat list of layers.
     */
    public @NotNull @Unmodifiable List<Supplier<ServiceLayer>> layers() {
        return this.delegate.entries();
    }
}
