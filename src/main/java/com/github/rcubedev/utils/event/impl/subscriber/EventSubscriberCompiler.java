package com.github.rcubedev.utils.event.impl.subscriber;

import java.lang.reflect.Method;
import java.util.List;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Identity;
import com.github.rcubedev.utils.event.api.annotation.SubscribeEvent;
import com.github.rcubedev.utils.event.api.spi.Registrar;
import com.github.rcubedev.utils.event.api.spi.Subscription;
import com.github.rcubedev.utils.event.api.subscriber.CompiledSubscriberBinder;
import com.github.rcubedev.utils.event.impl.subscriber.scanner.DiscoveredMethod;
import com.github.rcubedev.utils.event.impl.subscriber.scanner.ReflectiveSubscriberScanner;
import com.github.rcubedev.utils.test.UnitTestIgnored;

/**
 * Handles registration of {@link SubscribeEvent @SubscribeEvent} methods to a {@link Registrar}.
 */
public final class EventSubscriberCompiler<B extends Event> {

    private final ReflectiveSubscriberScanner scanner;
    private final EventHandlerCompiler<B> compiler;

    @UnitTestIgnored
    public EventSubscriberCompiler() {
        this(new ReflectiveSubscriberScanner(), new EventHandlerCompiler<>());
    }

    EventSubscriberCompiler(ReflectiveSubscriberScanner scanner, EventHandlerCompiler<B> compiler) {
        this.scanner = scanner;
        this.compiler = compiler;
    }

    /**
     * Register all {@link SubscribeEvent @SubscribeEvent} methods from a target to the given registrar.
     * <p>
     * Must be called in {@code rebuildLock} (if applicable).
     *
     * @param target an instance, {@link Class} (for static methods), or {@link Method}
     * @param identity the identity of the registering caller
     * @param registrar the registration callback provided by the bus
     * @throws IllegalArgumentException if invalid listener or no {@link SubscribeEvent @SubscribeEvent} methods found
     */
    public void build(Object target, Identity identity, Registrar<B> registrar) {
        if (target == null) throw new IllegalArgumentException("Cannot register null listener");

        // todo this is a WIP temp variant
        List<Subscription> compiled = CompiledSubscriberBinder.getInstance().register(target, identity, registrar);
        if (!compiled.isEmpty()) return;

        if (target instanceof Method method) {
            DiscoveredMethod discovered = this.scanner.scanSingleMethod(method);
            this.compiler.registerListener(discovered.instance(), discovered.method(), identity, registrar);
            return;
        }

        List<DiscoveredMethod> discoveredMethods = this.scanner.scanContainer(target, registrar.classValidator());
        for (DiscoveredMethod discovered : discoveredMethods) {
            this.compiler.registerListener(discovered.instance(), discovered.method(), identity, registrar);
        }
    }
}
