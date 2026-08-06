package com.github.rcubedev.utils.services.impl.layer;

import com.github.rcubedev.utils.services.api.exception.ServiceSignatureException;
import com.github.rcubedev.utils.services.api.spi.Eager;
import com.github.rcubedev.utils.services.api.spi.Service;
import com.github.rcubedev.utils.services.api.spi.ServiceLayer;
import com.github.rcubedev.utils.services.impl.EagerServiceImpl;
import com.github.rcubedev.utils.services.impl.ProviderServiceImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * {@link ServiceLayer} backed by a {@link ClassLoader}.
 * <p>
 * Uses {@link ServiceLoader#load(Class, ClassLoader)}.
 */
public final class ClassLoaderServiceLayer implements ServiceLayer {

    private final String name;
    private final ClassLoader classLoader;
    private final int priority;

    public ClassLoaderServiceLayer(@NotNull String name, @NotNull ClassLoader classLoader, int priority) {
        this.name = name;
        this.classLoader = classLoader;
        this.priority = priority;
    }

    @Override
    public @NotNull String name() {
        return this.name;
    }

    @Override
    public int priority() {
        return this.priority;
    }

    @Override
    public <S> @NotNull Optional<Service<S>> find(@NotNull Class<S> contract) {
        boolean eager = Eager.class.isAssignableFrom(contract);
//        Stream<Service<S>> stream = ServiceLoader.load(contract).stream()
//                .map(ProviderServiceImpl::new);

        Stream<Service<S>> stream = ServiceLoader.load(contract, this.classLoader).stream()
                .map(ProviderServiceImpl::new);

        Optional<Service<S>> ret = eager ? stream.filter(p -> ((Eager) p.get()).isAvailable()).findFirst()
                     : stream.findFirst();

        ret.ifPresent(s -> validateProvidedType(contract, s.type()));
        return ret;
    }

    @Override
    public <S> @NotNull @Unmodifiable List<Service<S>> findAll(@NotNull Class<S> contract) {
        boolean eager = Eager.class.isAssignableFrom(contract);
//        Stream<ServiceLoader.Provider<S>> stream = ServiceLoader.load(contract).stream();

        List<ServiceLoader.Provider<S>> providers = ServiceLoader.load(contract, this.classLoader)
                .stream()
                .toList();

        for (ServiceLoader.Provider<S> provider : providers) validateProvidedType(contract, provider.type());

        if (!eager) return providers.stream().map(s -> (Service<S>) new ProviderServiceImpl<>(s)).toList();
        return providers.stream().map(ServiceLoader.Provider::get)
                .filter(s -> ((Eager) s).isAvailable())
                .map(instance -> (Service<S>) new EagerServiceImpl<>(contract, instance))
                .toList();
    }

    private <S> void validateProvidedType(Class<S> contract, Class<? extends S> provided) {
        if (!(provided == contract)) return;
        throw new ServiceSignatureException(String.format(
                "ServiceLayer '%s' provider for '%s' must return its concrete type, not the interface.",
                this.name, contract.getSimpleName()
        ));
    }

    @Override
    public String toString() {
        return "ClassLoaderServiceLayer[name=" + this.name + ", priority=" + this.priority + "]";
    }
}