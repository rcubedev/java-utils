package com.github.rcubedev.utils.event.impl.apt.generator;

import com.github.rcubedev.utils.event.generated.StaticSubscriberInvoker;
import com.github.rcubedev.utils.event.impl.apt.scanner.DiscoveredMethod;
import com.github.rcubedev.utils.event.impl.apt.validation.module.ModuleValidator;

import javax.annotation.processing.ProcessingEnvironment;

public final class StaticInvokerSourceGenerator extends AbstractInvokerSourceGenerator {

    public StaticInvokerSourceGenerator(ProcessingEnvironment processingEnv, ModuleValidator moduleValidator) {
        super(processingEnv, moduleValidator);
    }

    @Override
    protected String factorySuffix() {
        return "StaticEventProcessorInvokerFactory";
    }

    @Override
    protected Class<?> invokerInterface() {
        return StaticSubscriberInvoker.class;
    }

    @Override
    protected String renderInvokerMethods(DiscoveredMethod method, String listenerType, String eventType, String methodName) {
        return  """
                @Override
                public void invoke(%s event) {
                    %s.%s(event);
                }""".formatted(eventType, listenerType, methodName);
    }
}