package com.github.rcubedev.example.services.impl.registry;

import com.github.rcubedev.example.services.api.spi.Service;
import com.github.rcubedev.example.services.api.spi.ServiceLayer;
import com.github.rcubedev.example.services.api.ServiceRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;

/**
 * {@link ServiceRegistry} that queries an ordered list of {@link ServiceLayer layers},
 * highest priority first.
 */
public final class CompositeServiceRegistry implements ServiceRegistry {

    private final List<ServiceLayer> layers;

    public CompositeServiceRegistry(@NotNull List<? extends ServiceLayer> layers) {
        this.layers = layers.stream()
                .sorted(Comparator.comparingInt(ServiceLayer::priority).reversed())
                .map(s -> (ServiceLayer) s)
                .toList();
    }

    @Override
    public @NotNull <S> Optional<Service<S>> find(@NotNull Class<S> contract) {
        for (ServiceLayer layer : layers) {
            Optional<Service<S>> result = layer.find(contract);
            if (result.isPresent()) return result;
        }
        return Optional.empty();
    }

    @Override
    public <S> @NotNull @Unmodifiable List<Service<S>> findAll(@NotNull Class<S> contract) {
        Set<Class<?>> seenImpls = new HashSet<>();

        return layers.stream()
                .flatMap(layer -> layer.findAll(contract).stream())
                .filter(s -> seenImpls.add(s.type()))
                .toList();
    }
}
