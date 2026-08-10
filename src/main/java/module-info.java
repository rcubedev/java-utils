import com.github.rcubedev.utils.event.generated.SubscriberInvokerFactory;
import com.github.rcubedev.utils.event.impl.apt.SubscribeProcessor;

import javax.annotation.processing.Processor;

module com.github.rcubedev.utils {
    requires org.slf4j; // fixme this might be gone soon

    requires com.google.common;
    requires kaleido.config; // fixme possible own config lib in future

    requires static org.jetbrains.annotations;
    requires java.compiler;

    exports com.github.rcubedev.utils.config;
    exports com.github.rcubedev.utils.config.elements;
    exports com.github.rcubedev.utils.config.serialization;

    exports com.github.rcubedev.utils.event.api;
    exports com.github.rcubedev.utils.event.api.annotation;
    exports com.github.rcubedev.utils.event.api.buses;
    exports com.github.rcubedev.utils.event.api.descriptor.method;
    exports com.github.rcubedev.utils.event.api.exceptions;
    exports com.github.rcubedev.utils.event.api.hooks;
    exports com.github.rcubedev.utils.event.api.spi;
    exports com.github.rcubedev.utils.event.api.subscriber;
    exports com.github.rcubedev.utils.event.api.subscriber.validation;
    exports com.github.rcubedev.utils.event.generated; // open for annotation processor
    provides Processor with SubscribeProcessor;
    uses SubscriberInvokerFactory;

    exports com.github.rcubedev.utils.reflect;
    exports com.github.rcubedev.utils.reflect.util;

    exports com.github.rcubedev.utils.registry.api;
    exports com.github.rcubedev.utils.registry.api.exception;

    exports com.github.rcubedev.utils.services.api;
    exports com.github.rcubedev.utils.services.api.exception;
    exports com.github.rcubedev.utils.services.api.spi;
    exports com.github.rcubedev.utils.services.impl.layer; // fixme temp
}