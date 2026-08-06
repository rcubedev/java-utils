package com.github.rcubedev.utils.services.api;

import com.github.rcubedev.utils.services.api.exception.ServiceNotFoundException;
import com.github.rcubedev.utils.services.api.exception.ServiceSignatureException;
import com.github.rcubedev.utils.services.api.spi.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

// todo how to handle duplicate services from the same ServiceLayer??
public interface ServiceRegistry {

    /**
     * Resolves a service matching the specified contract interface.
     *
     * @param contract the service interface token
     * @param <S> the service contract type
     * @return an {@link Optional} containing the resolved service, or empty
     * @throws ServiceSignatureException if an underlying service provider masks
     *                                   its {@link Service#type()} behind the
     *                                   {@code contract} interface.
     */
    <S> @NotNull Optional<Service<S>> find(@NotNull Class<S> contract);

    /**
     * Resolves all services matching the specified contract interface.
     *
     * @param contract the service interface token
     * @param <S> the service contract type
     * @return an unmodifiable list of all matching services
     * @throws ServiceSignatureException if an underlying service provider masks
     *                                   its {@link Service#type()} behind the
     *                                   {@code contract} interface.
     */
    <S> @NotNull @Unmodifiable List<Service<S>> findAll(@NotNull Class<S> contract);

    /**
     * Returns the first provider for {@code contract}, throwing if absent.
     *
     * @throws ServiceNotFoundException if no provider is registered
     * @throws ServiceSignatureException todo
     */
    default <S> @NotNull Service<S> require(@NotNull Class<S> contract) {
        return find(contract).orElseThrow(() -> new ServiceNotFoundException(contract));
    }
}