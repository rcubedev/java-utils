package com.github.rcubedev.utils.event.impl.apt.validation;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.impl.apt.scanner.DiscoveredMethod;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import java.util.Set;

public final class MethodValidator extends Validator {

    public MethodValidator(ProcessingEnvironment processingEnv) {
        super(processingEnv);
    }

    public boolean validate(DiscoveredMethod discoveredMethod) {
        ProcessingEnvironment processingEnv = getProcessingEnv();
        ExecutableElement method = discoveredMethod.method();
        Set<Modifier> modifiers = method.getModifiers();
        boolean valid = true;

        if (!modifiers.contains(Modifier.PUBLIC)) {
            valid &= throwError("@SubscribeEvent method must be public: " + method, method);
        }

        if (discoveredMethod.isWeak() && modifiers.contains(Modifier.STATIC)) {
            valid &= throwError("@Weak cannot be applied to static event handler methods or its declaring class: " + method, method);
        }

        if (method.getReturnType().getKind() != TypeKind.VOID) {
            valid &= throwError("@SubscribeEvent method must return void: " + method, method);
        }

        if (method.getParameters().size() != 1) {
            valid &= throwError("Method " + method + " has @SubscribeEvent annotation. " +
                    "It has " + method.getParameters().size() + " arguments, " +
                    "but event handler methods require a single argument only.", method);
        } else {
            TypeMirror paramType = method.getParameters().getFirst().asType();
            TypeElement eventElement = processingEnv.getElementUtils().getTypeElement(Event.class.getCanonicalName());

            if (eventElement == null) return throwError("Could not resolve base Event type on classpath", method);

            TypeMirror eventType = eventElement.asType();
            if (!processingEnv.getTypeUtils().isAssignable(paramType, eventType)) {
                valid &= throwError("Method " + method + " has @SubscribeEvent but parameter is not an Event subtype: "
                        + paramType, method);
            }
        }

        return valid;
    }
}
