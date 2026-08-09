package com.github.rcubedev.utils.event.impl.apt.validation;

import com.github.rcubedev.utils.event.api.annotation.SubscribeEvent;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import java.util.Set;

public final class ClassValidator extends Validator {

    private final Types typeUtils;

    public ClassValidator(ProcessingEnvironment processingEnv) {
        super(processingEnv);
        this.typeUtils = processingEnv.getTypeUtils();
    }

    public boolean validate(TypeElement listenerClass) {
        if (listenerClass.getKind() != ElementKind.CLASS) {
            return throwError("@SubscribeEvent handlers can only be processed inside classes.", listenerClass);
        }
        // use non short-circuiting & to keep going to try report as many errors
        return checkModifiers(listenerClass) & checkSupertypes(listenerClass, listenerClass);
    }

    private boolean checkModifiers(TypeElement listenerClass) {
        boolean valid = true;
        Set<Modifier> modifiers = listenerClass.getModifiers();

        if (modifiers.contains(Modifier.ABSTRACT)) {
            valid = throwError("Event listener class '%s' cannot be abstract."
                    .formatted(listenerClass.getQualifiedName()), listenerClass);
        }
        return checkEnclosing(listenerClass) & valid;
    }

    private boolean checkEnclosing(TypeElement listenerClass) {
        Element current = listenerClass;
        while (current != null && current.getKind() != ElementKind.PACKAGE) {
            if (!current.getModifiers().contains(Modifier.PUBLIC)) {
                return throwError("Enclosing element '%s' must be public for listener class '%s'."
                        .formatted(current.getSimpleName(), listenerClass.getQualifiedName()), listenerClass);
            }
            current = current.getEnclosingElement();
        }
        return true;
    }

    private boolean checkSupertypes(TypeElement registeredType, TypeElement type) {
        if (type == null || "java.lang.Object".equals(type.getQualifiedName().toString())) return true;

        boolean valid = true;

        if (!typeUtils.isSameType(registeredType.asType(), type.asType())) {
            for (Element enclosed : type.getEnclosedElements()) {
                if (enclosed.getKind() == ElementKind.METHOD && enclosed.getAnnotation(SubscribeEvent.class) != null) {
                    ExecutableElement method = (ExecutableElement) enclosed;
                    valid &= throwError("""
                            Event listener class '%s' inherits from supertype '%s' which contains @SubscribeEvent method '%s'. \
                            @SubscribeEvent methods are only allowed directly on the listener class itself.
                            """.formatted(registeredType.getQualifiedName(), type.getQualifiedName(), method.getSimpleName()), registeredType);
                }
            }
        }
        TypeMirror superclassMirror = type.getSuperclass();
        if (superclassMirror.getKind() != TypeKind.NONE) {
            if (typeUtils.asElement(superclassMirror) instanceof TypeElement superclassElement) {
                valid &= checkSupertypes(registeredType, superclassElement);
            }
        }
        for (TypeMirror interfaceMirror : type.getInterfaces()) {
            if (typeUtils.asElement(interfaceMirror) instanceof TypeElement interfaceElement) {
                valid &= checkSupertypes(registeredType, interfaceElement);
            }
        }
        return valid;
    }
}
