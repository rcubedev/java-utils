package com.github.rcubedev.utils.services.api.spi;

import com.github.rcubedev.utils.services.api.ServiceRegistry;
import com.github.rcubedev.utils.services.api.exception.ServiceSignatureException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.Optional;

/**
 * A single logical slice of service providers.
 *
 * <p>Layers are ordered by {@link #priority} within a {@link ServiceRegistry};
 * higher priority layers are queried first.
 */
public interface ServiceLayer {

    /**
     * Human-readable name for diagnostics (e.g. {@code "boot"}, {@code "mod:sodium"}).
     */
    @NotNull String name();

    /**
     * Higher value = queried first within the same {@link ServiceRegistry}.
     */
    int priority();

    /**
     * Finds the highest priority service matching the given contract.
     *
     * @param contract the service interface token
     * @param <S> the service contract type
     * @return an {@link Optional} containing the service, or empty if not found
     * @throws ServiceSignatureException if the discovered service provider masks
     *                                   its {@link Service#type()} behind the
     *                                   {@code contract} interface
     */
    <S> @NotNull Optional<Service<S>> find(@NotNull Class<S> contract);

    /**
     * Retrieves all services matching the given contract within this layer.
     *
     * @param contract the service interface token
     * @param <S> the service contract type
     * @return an unmodifiable list of matching services
     * @throws ServiceSignatureException if a discovered service provider masks
     *                                   its {@link Service#type()} behind the
     *                                   {@code contract} interface
     */
    <S> @NotNull @Unmodifiable List<Service<S>> findAll(@NotNull Class<S> contract);
}