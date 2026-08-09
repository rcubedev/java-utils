package com.github.rcubedev.utils.event.impl.apt.generator;

import com.github.rcubedev.utils.event.impl.apt.scanner.DiscoveredListener;
import com.github.rcubedev.utils.event.impl.apt.validation.MethodValidator;
import com.github.rcubedev.utils.event.impl.apt.validation.module.ModuleValidator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import java.util.Optional;

public final class InvokerSourceGenerator {

    private final InstanceInvokerSourceGenerator instanceGenerator;
    private final StaticInvokerSourceGenerator staticGenerator;

    public InvokerSourceGenerator(ProcessingEnvironment processingEnv, ModuleValidator moduleValidator) {
        this.instanceGenerator = new InstanceInvokerSourceGenerator(processingEnv, moduleValidator);
        this.staticGenerator = new StaticInvokerSourceGenerator(processingEnv, moduleValidator);
    }

    public Optional<String> generate(DiscoveredListener target) {
        if (target.handlerMethods().isEmpty()) return Optional.empty(); // this should never happen but js incase

        boolean isStatic = target.handlerMethods().getFirst().method().getModifiers().contains(Modifier.STATIC);
        return isStatic ? this.staticGenerator.generate(target) : this.instanceGenerator.generate(target);
    }
}
