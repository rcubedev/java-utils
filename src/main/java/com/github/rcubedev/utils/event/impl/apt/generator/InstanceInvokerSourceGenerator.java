package com.github.rcubedev.utils.event.impl.apt.generator;

import com.github.rcubedev.utils.event.generated.InstanceSubscriberInvoker;
import com.github.rcubedev.utils.event.impl.apt.scanner.DiscoveredMethod;
import com.github.rcubedev.utils.event.impl.apt.validation.module.ModuleValidator;

import javax.annotation.processing.ProcessingEnvironment;

public final class InstanceInvokerSourceGenerator extends AbstractInvokerSourceGenerator {

    public InstanceInvokerSourceGenerator(ProcessingEnvironment processingEnv, ModuleValidator moduleValidator) {
        super(processingEnv, moduleValidator);
    }

    @Override
    protected String factorySuffix() {
        return "InstanceEventProcessorInvokerFactory";
    }

    @Override
    protected Class<?> invokerInterface() {
        return InstanceSubscriberInvoker.class;
    }

    @Override
    protected String renderInvokerMethods(DiscoveredMethod method, String listenerType, String eventType, String methodName) {
        return  """
                @Override
                public boolean isWeak() {
                    return %s;
                }

                @Override
                public void invoke(%s listener, %s event) {
                    listener.%s(event);
                }""".formatted(method.isWeak(), listenerType, eventType, methodName);
    }
}