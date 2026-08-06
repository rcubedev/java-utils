package com.github.rcubedev.utils.services.api;

import com.github.rcubedev.utils.services.api.spi.ServiceLayer;
import com.github.rcubedev.utils.services.impl.RuntimeServiceRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Static accessor for the active {@link ServiceRegistry}.
 *
 * <p>The registry must be initialised exactly once by the environment's
 * bootstrap code (game entrypoint, test setup, etc.) before any mod calls
 * {@link #get()}. This is intentional: no individual mod can build a registry
 * that sees sibling mod layers — only the bootstrap, which runs after all
 * modules are loaded, has that visibility.
 *
 * <p>Bootstrap (e.g. a NeoForge mod-loading entrypoint):
 * <pre>{@code
 * Services.init(RuntimeServiceRegistry.of(allModLayers));
 * }</pre>
 *
 * <p>Any mod consuming a service:
 * <pre>{@code
 * private static final MyService MY_SERVICE = Services.get().require(MyService.class);
 * }</pre>
 */
@Deprecated(forRemoval = true)
public final class Services {

    private static volatile boolean ready = false;

    private Services() {}

    /**
     * Returns the active {@link ServiceRegistry}.
     */
    public static @NotNull ServiceRegistry get() {
        if (!ready) throw new IllegalStateException(
                "Services not yet initialised. ServicesBootstrap must complete first");
        return RegistryHolder.REGISTRY;
    }

    /**
     * Called by {@link ServiceBootstrap} after all handlers have run.
     * <p>
     * Sets {@code ready} only after the holder successfully initialises,
     * so a failed build leaves {@code ready} false and the holder untouched.
     */
    static void freeze() {
        LayerHolder.LAYERS.freeze();
        ready = true;
    }

    public static @NotNull Supplier<ServiceLayer> register(@NotNull Supplier<ServiceLayer> layer) {
        LayerHolder.LAYERS.register(layer);
        return layer;
    }

    private static final class LayerHolder {
        private static final ServiceLayerRegistry LAYERS = new ServiceLayerRegistry();
    }

    private static final class RegistryHolder {
        private static final ServiceRegistry REGISTRY = build();

        private static ServiceRegistry build() {
            return RuntimeServiceRegistry.of(LayerHolder.LAYERS);
        }
    }
}