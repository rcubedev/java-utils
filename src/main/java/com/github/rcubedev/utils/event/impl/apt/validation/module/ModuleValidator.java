package com.github.rcubedev.utils.event.impl.apt.validation.module;

import com.github.rcubedev.utils.event.generated.SubscriberInvokerFactory;
import com.github.rcubedev.utils.event.impl.apt.validation.Validator;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;

public final class ModuleValidator extends Validator {

    public ModuleValidator(ProcessingEnvironment processingEnv) {
        super(processingEnv);
    }

    public boolean validateModuleProvides(TypeElement listener, String factoryFqcn) {
        ProcessingEnvironment processingEnv = getProcessingEnv();
        ModuleElement module = processingEnv.getElementUtils().getModuleOf(listener);
        if (module.isUnnamed()) return true;

        String serviceInterface = SubscriberInvokerFactory.class.getCanonicalName();
        boolean providesFactory = false;

        for (ModuleElement.Directive directive : module.getDirectives()) {
            if (directive.getKind() == ModuleElement.DirectiveKind.PROVIDES) {
                ModuleElement.ProvidesDirective provides = (ModuleElement.ProvidesDirective) directive;

                if (provides.getService().getQualifiedName().contentEquals(serviceInterface)) {
                    for (TypeElement impl : provides.getImplementations()) {
                        if (impl.getQualifiedName().contentEquals(factoryFqcn)) {
                            providesFactory = true;
                            break;
                        }
                    }
                }
            }
        }

        if (providesFactory) return true;
        return throwError("Module '" + module.getQualifiedName() + "' must declare the generated factory in module-info.java:\n" +
                "    provides " + serviceInterface + " with " + factoryFqcn + ";", module);
    }
}
