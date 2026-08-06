package com.github.rcubedev.utils.services.impl.layer;

import com.github.rcubedev.utils.registry.impl.SimpleRegistry;
import com.github.rcubedev.utils.services.api.spi.Service;
import com.github.rcubedev.utils.services.api.spi.ServiceLayer;
import com.github.rcubedev.utils.services.impl.DirectServiceImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

// fixme this is quite broken in its current form.
public final class ManualServiceLayer implements ServiceLayer {

    private final String name;
    private final int priority;

    private final Map<Class<?>, SimpleRegistry<Service<?>>> registryMap = new ConcurrentHashMap<>();

    public ManualServiceLayer(@NotNull String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public <S> ManualServiceLayer register(@NotNull Class<S> contract, @NotNull Supplier<? extends S> factory, @NotNull Class<? extends S> implType) {
        SimpleRegistry<Service<?>> contractRegistry = this.registryMap.computeIfAbsent(
                contract,
                k -> new SimpleRegistry<>(this.name + "/" + contract.getSimpleName())
        );

        contractRegistry.register(new DirectServiceImpl<>(implType, factory));
        return this;
    }

    /**
     * Call this when mod initialization wraps up to seal the services from further changes.
     */
    public void freeze() {
        this.registryMap.values().forEach(SimpleRegistry::freeze);
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> @NotNull Optional<Service<S>> find(@NotNull Class<S> contract) {
        SimpleRegistry<Service<?>> contractRegistry = this.registryMap.get(contract);
        if (contractRegistry == null) return Optional.empty();

        try {
            List<Service<?>> entries = contractRegistry.entries();
            if (entries.isEmpty()) return Optional.empty();
            return Optional.of((Service<S>) entries.getFirst());
        } catch (Exception e) {
            // Catches RegistryFrozenException if lookup is attempted too early
            return Optional.empty();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> @NotNull @Unmodifiable List<Service<S>> findAll(@NotNull Class<S> contract) {
        SimpleRegistry<Service<?>> contractRegistry = this.registryMap.get(contract);
        if (contractRegistry == null) return Collections.emptyList();

        try {
            // SimpleRegistry already provides an unmodifiable view safely under its internal states
            return contractRegistry.entries().stream()
                    .map(s -> (Service<S>) s)
                    .toList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public int priority() {
        return this.priority;
    }
}