package com.github.rcubedev.utils.services.impl.layer;

import com.github.rcubedev.utils.registry.api.mutable.MutableKeylessRegistry;
import com.github.rcubedev.utils.registry.api.mutable.MutableRegistry;
import com.github.rcubedev.utils.services.api.exception.ServiceSignatureException;
import com.github.rcubedev.utils.services.api.spi.MutableServiceLayer;
import com.github.rcubedev.utils.services.api.spi.Service;
import com.github.rcubedev.utils.services.impl.DirectServiceImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public final class ManualServiceLayer implements MutableServiceLayer {

    private final String name;
    private final int priority;

    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();
    private final Map<Class<?>, MutableKeylessRegistry<Service<?>>> registryMap = new ConcurrentHashMap<>();

    private volatile boolean frozen = false;

    public ManualServiceLayer(@NotNull String name, int priority) {
        this.name = name;
        this.priority = priority;
    }

    public <S, T extends S> ManualServiceLayer register(@NotNull Class<S> contract, @NotNull Supplier<T> factory, @NotNull Class<T> implType) {
        if (frozen) throw new IllegalStateException("Service layer is frozen");
        validateProvidedType(contract, implType);

        lifecycleLock.readLock().lock();
        try {
            if (frozen) throw new IllegalStateException("Service layer is frozen");

            MutableKeylessRegistry<Service<?>> contractRegistry = this.registryMap.computeIfAbsent(contract,
                    k -> MutableKeylessRegistry.create(this.name + "/" + k.getName()));

            contractRegistry.register(new DirectServiceImpl<>(implType, factory));
            return this;
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    public void freeze() {
        if (frozen) throw new IllegalStateException("Service layer is already frozen");

        lifecycleLock.writeLock().lock();
        try {
            if (frozen) throw new IllegalStateException("Service layer is already frozen");
            this.registryMap.values().forEach(MutableRegistry::freeze);
            frozen = true;
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public <S> @NotNull Optional<Service<S>> find(@NotNull Class<S> contract) {
        if (!frozen) throw new IllegalStateException("Service layer not frozen");

        MutableKeylessRegistry<Service<?>> contractRegistry = this.registryMap.get(contract);
        if (contractRegistry == null) return Optional.empty();

        List<Service<?>> entries = contractRegistry.entries();
        if (entries.isEmpty()) return Optional.empty();

        @SuppressWarnings("unchecked")
        Service<S> service = (Service<S>) entries.getFirst();

        return Optional.of(service);
    }

    @Override
    public <S> @NotNull @Unmodifiable List<Service<S>> findAll(@NotNull Class<S> contract) {
        if (!frozen) throw new IllegalStateException("Service layer not frozen");

        MutableKeylessRegistry<Service<?>> contractRegistry = this.registryMap.get(contract);
        if (contractRegistry == null) return Collections.emptyList();

        @SuppressWarnings("unchecked")
        List<Service<S>> services = (List<Service<S>>) (List<?>) contractRegistry.entries();
        return services;
    }

    private <S> void validateProvidedType(Class<S> contract, Class<? extends S> provided) {
        if (provided == contract) throw new ServiceSignatureException(String.format(
                "ServiceLayer '%s' provider for '%s' must return its concrete type, not the contract.",
                this.name, contract.getSimpleName()
        ));
    }

    @Override
    public int priority() {
        return this.priority;
    }
}