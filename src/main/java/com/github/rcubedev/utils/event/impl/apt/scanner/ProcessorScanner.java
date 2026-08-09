package com.github.rcubedev.utils.event.impl.apt.scanner;

import com.github.rcubedev.utils.event.api.annotation.SubscribeEvent;
import com.github.rcubedev.utils.event.impl.apt.validation.ClassValidator;
import com.github.rcubedev.utils.event.impl.apt.validation.MethodValidator;
import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.*;

public final class ProcessorScanner {

    private final ProcessingEnvironment processingEnv;
    private final ClassValidator validator;
    private final MethodValidator methodValidator;

    public ProcessorScanner(ProcessingEnvironment processingEnv, ClassValidator validator, MethodValidator methodValidator) {
        this.processingEnv = processingEnv;
        this.validator = validator;
        this.methodValidator = methodValidator;
    }

    // null == error
    public @Nullable DiscoveredListener scanContainer(TypeElement clazz) {
        if (!this.validator.validate(clazz)) return null;

        List<? extends Element> enclosedElements = clazz.getEnclosedElements();
        List<DiscoveredMethod> methods = new ArrayList<>(enclosedElements.size());

        Boolean isStaticContainer = null;
        boolean failed = false;

        for (Element enclosed : enclosedElements) {
            if (enclosed.getKind() != ElementKind.METHOD) continue;
            if (enclosed.getAnnotation(SubscribeEvent.class) == null) continue;

            ExecutableElement method = (ExecutableElement) enclosed;
            DiscoveredMethod discoveredMethod = new DiscoveredMethod(clazz, method);

            if (!this.methodValidator.validate(discoveredMethod)) {
                failed = true;
                continue; // keep going to try report as many errors
            }

            boolean isStatic = method.getModifiers().contains(Modifier.STATIC);
            if (isStaticContainer == null) {
                isStaticContainer = isStatic;
            } else if (isStaticContainer != isStatic) {
                throwError("""
                        Class %s mixes static and non-static @SubscribeEvent methods. \
                        All @SubscribeEvent methods in a listener must be either all static or all non-static.
                        """.formatted(clazz.getQualifiedName()), method);
                failed = true;
                continue; // keep going to try report as many errors
            }

            methods.add(discoveredMethod);
        }

        if (failed) return null;

        if (methods.isEmpty()) {
            throwError("""
                    %s has no @SubscribeEvent methods, but register was called anyway. \
                    The event bus only recognizes listener methods annotated with @SubscribeEvent.
                    """.formatted(clazz), clazz);
            return null;
        }
        return new DiscoveredListener(clazz, methods);
    }

    /*public List<DiscoveredListener> scanAll(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Map<TypeElement, List<DiscoveredMethod>> methodsByListener = new LinkedHashMap<>();

        for (TypeElement typeElement : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(typeElement);

            for (Element element : elements) {
                if (element.getKind() != ElementKind.CLASS) continue;

                TypeElement listenerClass = (TypeElement) element;
                if (!this.methodValidator.isClassAccessible(listenerClass)) continue;

                for (Element enclosed : listenerClass.getEnclosedElements()) {
                    if (enclosed.getKind() != ElementKind.METHOD) continue;
                    if (enclosed.getAnnotation(SubscribeEvent.class) == null) continue;

                    ExecutableElement method = (ExecutableElement) enclosed;
                    DiscoveredMethod discoveredMethod = new DiscoveredMethod(listenerClass, method);

                    if (this.methodValidator.validate(discoveredMethod)) {
                        methodsByListener.computeIfAbsent(listenerClass, k -> new ArrayList<>())
                                .add(discoveredMethod);
                    }
                }
            }
        }

        List<DiscoveredListener> discoveredListeners = new ArrayList<>();
        for (Map.Entry<TypeElement, List<DiscoveredMethod>> entry : methodsByListener.entrySet()) {
            discoveredListeners.add(new DiscoveredListener(entry.getKey(), entry.getValue()));
        }
        return discoveredListeners;
    }*/

    public boolean throwError(String error, Element e) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error, e);
        return false;
    }

    public boolean throwError(Exception ex, Element e) {
        return throwError(ex.getMessage(), e);
    }
}