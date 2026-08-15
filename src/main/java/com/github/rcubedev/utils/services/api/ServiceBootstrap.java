package com.github.rcubedev.utils.services.api;

import com.github.rcubedev.utils.services.api.spi.MutableServiceLayer;
import com.github.rcubedev.utils.services.api.spi.ServiceLayer;
import com.github.rcubedev.utils.services.impl.RuntimeServiceRegistry;
import com.github.rcubedev.utils.services.impl.layer.ClassLoaderServiceLayer;
import com.github.rcubedev.utils.services.impl.layer.ManualServiceLayer;
import com.github.rcubedev.utils.services.impl.layer.ModuleLayerServiceLayer;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builds a {@link ServiceRegistry} from the provided {@link ServiceLayer}s.
 */
public final class ServiceBootstrap {

    private ServiceBootstrap() {}

    /**
     * Creates a {@link ServiceLayer} backed by the provided {@link ClassLoader}.
     */
    public static @NotNull ServiceLayer classLoader(@NotNull String name, @NotNull ClassLoader classLoader, int priority) {
        return new ClassLoaderServiceLayer(name, classLoader, priority);
    }

    /**
     * Creates a {@link ServiceLayer} backed by the thread context class loader.
     */
    public static @NotNull ServiceLayer classLoader(@NotNull String name, int priority) {
        return classLoader(name, Thread.currentThread().getContextClassLoader(), priority);
    }

    /**
     * Creates a manually configured {@link ServiceLayer}.
     */
    public static @NotNull ServiceLayer manual(@NotNull String name, int priority, @NotNull Consumer<MutableServiceLayer> configurer) {
        ManualServiceLayer layer = new ManualServiceLayer(name, priority);
        configurer.accept(layer);
        layer.freeze();
        return layer;
    }

    /**
     * Creates a {@link ServiceLayer} backed by the provided {@link ModuleLayer}.
     */
    public static @NotNull ServiceLayer moduleLayer(@NotNull String name, @NotNull ModuleLayer moduleLayer, int priority) {
        return new ModuleLayerServiceLayer(name, moduleLayer, priority);
    }

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