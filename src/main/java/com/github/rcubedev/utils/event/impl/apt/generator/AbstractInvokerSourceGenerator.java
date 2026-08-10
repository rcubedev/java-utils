package com.github.rcubedev.utils.event.impl.apt.generator;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.api.annotation.SubscribeEvent;
import com.github.rcubedev.utils.event.api.descriptor.method.MethodDescriptor;
import com.github.rcubedev.utils.event.generated.SubscriberInvokerFactory;
import com.github.rcubedev.utils.event.impl.apt.scanner.DiscoveredListener;
import com.github.rcubedev.utils.event.impl.apt.scanner.DiscoveredMethod;
import com.github.rcubedev.utils.event.impl.apt.validation.module.ModuleValidator;

import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractInvokerSourceGenerator extends Generator {

    private static final String SEPARATOR = "_";
    private final ModuleValidator moduleValidator;

    protected AbstractInvokerSourceGenerator(ProcessingEnvironment processingEnv, ModuleValidator moduleValidator) {
        super(processingEnv);
        this.moduleValidator = moduleValidator;
    }

    protected abstract String factorySuffix();

    protected abstract Class<?> invokerInterface();

    protected abstract String renderInvokerMethods(DiscoveredMethod method, String listenerType, String eventType, String methodName);

    public Optional<String> generate(DiscoveredListener target) {
        ProcessingEnvironment processingEnv = this.getProcessingEnv();

        TypeElement listener = target.listenerClass();
        List<DiscoveredMethod> handlerMethods = target.handlerMethods();

        String packageName = processingEnv.getElementUtils().getPackageOf(listener).getQualifiedName().toString();
        String listenerType = listener.getQualifiedName().toString();

        String relativeClassName = packageName.isEmpty()
                ? listenerType
                : listenerType.substring(packageName.length() + 1);
        String flatListenerName = relativeClassName.replace(".", SEPARATOR);

        String factoryClassName = flatListenerName + SEPARATOR + factorySuffix();
        String fullFactoryClassName = packageName.isEmpty() ? factoryClassName : packageName + "." + factoryClassName;

        this.moduleValidator.validateModuleProvides(listener, fullFactoryClassName); // keep going to try report as many errors possible

        StringBuilder invokerEntries = new StringBuilder();
        StringBuilder nestedInvokers = new StringBuilder();

        for (int i = 0; i < handlerMethods.size(); i++) {
            DiscoveredMethod discoveredMethod = handlerMethods.get(i);
            ExecutableElement method = discoveredMethod.method();
            String methodName = method.getSimpleName().toString();

            SubscribeEvent subscribeEvent = method.getAnnotation(SubscribeEvent.class);
            assert subscribeEvent != null : "Validated when added to methods";

            Priority priority = subscribeEvent.priority();
            String priorityCode = Priority.class.getSimpleName() + "." + priority.name();
            boolean ignoreCancelled = subscribeEvent.ignoreCancelled();

            TypeMirror paramType = method.getParameters().getFirst().asType();
            TypeMirror erasedType = processingEnv.getTypeUtils().erasure(paramType);
            String eventType = erasedType.toString();

            String modifiersStr = method.getModifiers().stream()
                    .map(Modifier::toString)
                    .collect(Collectors.joining(" "));

            String returnTypeClass = processingEnv.getTypeUtils().erasure(method.getReturnType()).toString() + ".class";

            String paramClassLiterals = method.getParameters().stream()
                    .map(p -> processingEnv.getTypeUtils().erasure(p.asType()).toString() + ".class")
                    .collect(Collectors.joining(", "));

            String paramsArg = paramClassLiterals.isEmpty() ? "" : ", " + paramClassLiterals;

            String descriptorCode = "MethodDescriptor.of(\"%s\", %s.class, \"%s\", %s%s)".formatted(
                    modifiersStr,
                    listenerType,
                    methodName,
                    returnTypeClass,
                    paramsArg
            );

            String invokerInnerName = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1) + SEPARATOR + i + SEPARATOR + "Invoker";

            invokerEntries.append("new ").append(invokerInnerName).append("()");
            if (i < handlerMethods.size() - 1) {
                invokerEntries.append(",\n            ");
            }

            String invokerMethods = renderInvokerMethods(discoveredMethod, listenerType, eventType, methodName);

            nestedInvokers.append("""

                    public static final class %s implements %s<%s, %s> {

                        private static final MethodDescriptor DESCRIPTOR = %s;

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
                        public MethodDescriptor descriptor() {
                            return DESCRIPTOR;
                        }

                    %s}
                    """.formatted(
                    invokerInnerName,
                    invokerInterface().getSimpleName(),
                    listenerType, eventType,
                    descriptorCode,
                    eventType, eventType,
                    priorityCode, ignoreCancelled,
                    invokerMethods.indent(4)
            ));
        }

        String packageDecl = packageName.isEmpty() ? "" : "package " + packageName + ";\n\n";

        String code = """
                %simport %s;
                import %s;
                import %s;
                import %s;
                import %s;
                import %s;

                @%s("%s")
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
                MethodDescriptor.class.getCanonicalName(),
                invokerInterface().getCanonicalName(),
                Priority.class.getCanonicalName(),
                SubscriberInvokerFactory.class.getCanonicalName(),
                List.class.getCanonicalName(),
                Generated.class.getCanonicalName(),
                getClass().getCanonicalName(),
                factoryClassName,
                SubscriberInvokerFactory.class.getSimpleName(),
                listenerType,
                Event.class.getSimpleName(),
                invokerInterface().getSimpleName(),
                listenerType,
                Event.class.getSimpleName(),
                invokerEntries.toString(),
                listenerType,
                listenerType,
                invokerInterface().getSimpleName(),
                listenerType,
                Event.class.getSimpleName(),
                nestedInvokers.toString().indent(4)
        );

        boolean written = writeSourceFile(fullFactoryClassName, listener, writer -> writer.write(code));
        return written ? Optional.of(fullFactoryClassName) : Optional.empty();
    }
}