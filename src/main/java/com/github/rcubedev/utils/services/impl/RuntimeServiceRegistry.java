package com.github.rcubedev.utils.services.impl;

import com.github.rcubedev.utils.services.api.ServiceLayerRegistry;
import com.github.rcubedev.utils.services.api.ServiceRegistry;
import com.github.rcubedev.utils.services.api.spi.Service;
import com.github.rcubedev.utils.services.api.spi.ServiceLayer;
import com.github.rcubedev.utils.services.impl.layer.LazyServiceLayer;
import com.github.rcubedev.utils.services.impl.registry.CompositeServiceRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

public final class RuntimeServiceRegistry implements ServiceRegistry {

    private final ServiceRegistry delegate;

    public static RuntimeServiceRegistry of(@NotNull ServiceLayerRegistry layerRegistry) {
        List<LazyServiceLayer> layers = layerRegistry.layers().stream()
                .map(LazyServiceLayer::new)
                .toList();
        return new RuntimeServiceRegistry(layers);
    }

    RuntimeServiceRegistry(@NotNull List<? extends ServiceLayer> layers) {
        this.delegate = new CompositeServiceRegistry(layers);
    }

    @Override
    public @NotNull <S> Optional<Service<S>> find(@NotNull Class<S> contract) {
        return delegate.find(contract);
    }

    @Override
    public @NotNull @Unmodifiable <S> List<Service<S>> findAll(@NotNull Class<S> contract) {
        return delegate.findAll(contract);
    }
}