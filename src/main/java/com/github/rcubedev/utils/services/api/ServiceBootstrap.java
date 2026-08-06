package com.github.rcubedev.utils.services.api;

import com.github.rcubedev.utils.services.api.spi.ServiceLayer;
import com.github.rcubedev.utils.services.impl.RuntimeServiceRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Builds a {@link ServiceRegistry} from a set of known {@link ServiceLayer layers}.
 */
// todo add ModuleLayer acceptor etc to create the internal stuff
public final class ServiceBootstrap {

    private ServiceBootstrap() {}

    /**
     * Builds a {@link ServiceRegistry} from the provided layers.
     */
    @SafeVarargs
    public static @NotNull ServiceRegistry bootstrap(@NotNull Supplier<ServiceLayer>... layers) {
        ServiceLayerRegistry registry = new ServiceLayerRegistry();
        return bootstrap(registry, layers);
    }

    /**
     * Builds a {@link ServiceRegistry} from the provided layers.
     */
    public static @NotNull ServiceRegistry bootstrap(@NotNull ServiceLayer... layers) {
        @SuppressWarnings("unchecked")
        Supplier<ServiceLayer>[] result = Arrays.stream(layers)
                .map(v -> (Supplier<ServiceLayer>) () -> v)
                .toArray(Supplier[]::new);
        return bootstrap(result);
    }

    /**
     * Builds a {@link ServiceRegistry} from a pre-populated {@link ServiceLayerRegistry}
     * plus any additional known layers.
     * <p>
     * The registry must not yet be frozen.
     */
    @SafeVarargs
    public static @NotNull ServiceRegistry bootstrap(@NotNull ServiceLayerRegistry layerRegistry,
                                                     @NotNull Supplier<ServiceLayer>... layers) {
        for (Supplier<ServiceLayer> layer : layers) {
            layerRegistry.register(layer);
        }
        layerRegistry.freeze();
        return RuntimeServiceRegistry.of(layerRegistry);
    }

    /**
     * Builds a {@link ServiceRegistry} from a pre-populated {@link ServiceLayerRegistry}
     * plus any additional known layers.
     * <p>
     * The registry must not yet be frozen.
     */
    public static @NotNull ServiceRegistry bootstrap(@NotNull ServiceLayerRegistry layerRegistry,
                                                     @NotNull ServiceLayer... layers) {
        @SuppressWarnings("unchecked")
        Supplier<ServiceLayer>[] result = Arrays.stream(layers)
                .map(v -> (Supplier<ServiceLayer>) () -> v)
                .toArray(Supplier[]::new);
        return bootstrap(layerRegistry, result);
    }
}