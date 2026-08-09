package com.github.rcubedev.utils.event.impl.fixme;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.api.annotation.SubscribeEvent;
import com.github.rcubedev.utils.event.api.annotation.CompiledEventHandlers;
import com.github.rcubedev.utils.event.generated.EventSubscriberInvoker;
import com.github.rcubedev.utils.event.generated.SubscriberInvokerFactory;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

// fixme fix solid violations as this is becoming a god class
public final class SubscribeProcessor extends AbstractProcessor {

    private final Set<String> generatedFactories = new LinkedHashSet<>();

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(CompiledEventHandlers.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_21;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            this.writeService();
            return true;
        }

        Map<TypeElement, List<ExecutableElement>> listenerMap = new LinkedHashMap<>();
        for (TypeElement typeElement : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(typeElement);

            for (Element element : elements) {
                if (element.getKind() != ElementKind.CLASS) continue;

                TypeElement listenerClass = (TypeElement) element;

                if (!isClassAccessible(listenerClass)) continue;

                List<ExecutableElement> validMethods = new ArrayList<>();

                for (Element enclosed : listenerClass.getEnclosedElements()) {
                    if (enclosed.getKind() != ElementKind.METHOD) continue;
                    if (enclosed.getAnnotation(SubscribeEvent.class) == null) continue;

                    ExecutableElement method = (ExecutableElement) enclosed;
                    if (validateMethod(method)) validMethods.add(method);
                }

                if (!validMethods.isEmpty()) this.generateFactorySource(listenerClass, validMethods);
            }
        }

        for (Map.Entry<TypeElement, List<ExecutableElement>> entry : listenerMap.entrySet()) {
            this.generateFactorySource(entry.getKey(), entry.getValue());
        }
        return true;
    }

    private boolean isClassAccessible(TypeElement listenerClass) {
        Element current = listenerClass;
        while (current != null && current.getKind() != ElementKind.PACKAGE) {
            if (current.getModifiers().contains(Modifier.PRIVATE)) {
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.ERROR,
                        "Listener or enclosing element '" + current.getSimpleName() + "' cannot be private",
                        listenerClass
                );
                return false;
            }
            current = current.getEnclosingElement();
        }
        return true;
    }

    private boolean validateMethod(ExecutableElement method) {
        if (method.getParameters().size() != 1) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@SubscribeEvent method must have exactly 1 parameter", method);
            return false;
        }

        Set<Modifier> modifiers = method.getModifiers();
        if (modifiers.contains(Modifier.PRIVATE)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@SubscribeEvent method cannot be private", method);
            return false;
        }
        if (modifiers.contains(Modifier.ABSTRACT)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@SubscribeEvent method cannot be abstract", method);
            return false;
        }
        // fixme this is temporary
        if (modifiers.contains(Modifier.STATIC)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@SubscribeEvent method cannot be static", method);
            return false;
        }

        TypeMirror paramType = method.getParameters().getFirst().asType();
        TypeElement eventElement = processingEnv.getElementUtils()
                .getTypeElement("com.github.rcubedev.utils.event.api.Event");
        if (eventElement == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Could not resolve base Event type on classpath", method);
            return false;
        }

        TypeMirror eventType = eventElement.asType();
        if (!processingEnv.getTypeUtils().isAssignable(paramType, eventType)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "@SubscribeEvent method parameter must extend Event", method);
            return false;
        }

        if (paramType.getKind() == TypeKind.DECLARED) {
            DeclaredType declaredType = (DeclaredType) paramType;
            if (!declaredType.getTypeArguments().isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                        "@SubscribeEvent event parameter cannot have generic type arguments", method);
                return false;
            }
        }
        return true;
    }

    private void generateFactorySource(TypeElement listener, List<ExecutableElement> methods) {
        String packageName = processingEnv.getElementUtils().getPackageOf(listener).getQualifiedName().toString();
        String listenerType = listener.getQualifiedName().toString();

        // fixme use _ instead maybe? will $ cause issues on sm JVMs?
        String seperator = "_";
        String relativeClassName = packageName.isEmpty()
                ? listenerType
                : listenerType.substring(packageName.length() + 1);
        String flatListenerName = relativeClassName.replace(".", seperator);

        String factoryClassName = flatListenerName + seperator + "EventProcessorInvokerFactory";
        String fullFactoryClassName = packageName.isEmpty() ? factoryClassName : packageName + "." + factoryClassName;

        this.verifyModuleProvides(listener, fullFactoryClassName);

        StringBuilder invokerEntries = new StringBuilder();
        StringBuilder nestedInvokers = new StringBuilder();

        for (int i = 0; i < methods.size(); i++) {
            ExecutableElement method = methods.get(i);
            String methodName = method.getSimpleName().toString();

            SubscribeEvent subscribeEvent = method.getAnnotation(SubscribeEvent.class);
            assert subscribeEvent != null : "Validated when added to methods";
            Priority priority = subscribeEvent.priority();
            String priorityCode = priority.getDeclaringClass().getSimpleName() + "." + priority.name();
            boolean ignoreCancelled = subscribeEvent.ignoreCancelled();

            TypeMirror paramType = method.getParameters().getFirst().asType();
            TypeMirror erasedType = processingEnv.getTypeUtils().erasure(paramType);
            String eventType = erasedType.toString();

            String invokerInnerName = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1) + seperator + i + seperator + "Invoker";

            invokerEntries.append("new ").append(invokerInnerName).append("()");

            if (i < methods.size() - 1) {
                invokerEntries.append(",\n            ");
            }

            nestedInvokers.append("""

                    public static final class %s implements %s<%s, %s> {

                        @Override
                        public Class<%s> eventType() {
                            return %s.class;
                        }

                        @Override
                        public Priority priority() {
                            return %s;
                        }

                        @Override
                        public boolean ignoreCancelled() {
                            return %s;
                        }

                        @Override
                        public void invoke(%s listener, %s event) {
                            listener.%s(event);
                        }
                    }
                    """.formatted(invokerInnerName,
                    EventSubscriberInvoker.class.getSimpleName(),
                    listenerType, eventType, eventType, eventType,
                    priorityCode, ignoreCancelled, listenerType,
                    eventType, methodName));
        }

        String packageDecl = packageName.isEmpty() ? "" : "package " + packageName + ";\n\n";

        // fixme maybe use javapoet
        String code = """
                %simport %s;
                import %s;
                import %s;
                import %s;
                import java.util.List;
                
                public final class %s implements %s<%s, %s> {
                
                    private static final List<%s<%s, ? extends %s>> INVOKERS = List.of(
                            %s
                    );

                    @Override
                    public Class<%s> targetClass() {
                        return %s.class;
                    }
                
                    @Override
                    public List<%s<%s, ? extends %s>> invokers() {
                        return INVOKERS;
                    }
                %s}""".formatted(
                packageDecl,
                Event.class.getCanonicalName(),
                EventSubscriberInvoker.class.getCanonicalName(),
                Priority.class.getCanonicalName(),
                SubscriberInvokerFactory.class.getCanonicalName(),
                factoryClassName,
                SubscriberInvokerFactory.class.getSimpleName(),
                listenerType,
                Event.class.getSimpleName(),
                EventSubscriberInvoker.class.getSimpleName(),
                listenerType,
                Event.class.getSimpleName(),
                invokerEntries.toString(),
                listenerType,
                listenerType,
                EventSubscriberInvoker.class.getSimpleName(),
                listenerType,
                Event.class.getSimpleName(),
                nestedInvokers.toString().indent(4));

        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(fullFactoryClassName, listener);
            try (Writer writer = file.openWriter()) {
                writer.write(code);
            }
            this.generatedFactories.add(fullFactoryClassName);
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Failed to generate factory: " + e.getMessage());
        }
    }

    // always write for named modules as fallback if not run in modularity
    private void writeService() {
        if (this.generatedFactories.isEmpty()) return;
        String servicePath = "META-INF/services/" + SubscriberInvokerFactory.class.getCanonicalName();

        try {
            FileObject resource = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT,
                    "", servicePath);

            try (BufferedWriter writer = new BufferedWriter(resource.openWriter())) {
                for (String factoryFqcn : this.generatedFactories) {
                    writer.write(factoryFqcn);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to write SPI service file: " + e.getMessage());
        }
    }

    private void verifyModuleProvides(TypeElement listener, String factoryFqcn) {
        ModuleElement module = processingEnv.getElementUtils().getModuleOf(listener);
        if (module.isUnnamed()) return;

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

        if (!providesFactory) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Module '" + module.getQualifiedName() + "' must declare the generated factory in module-info.java:\n" +
                            "    provides " + serviceInterface + " with " + factoryFqcn + ";", module);
        }
    }
}
