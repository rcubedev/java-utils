package com.github.rcubedev.utils.event.impl.descriptor.method;

import com.github.rcubedev.utils.event.api.descriptor.method.MethodDescriptor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class ReflectionMethodDescriptor extends MethodDescriptor {

    private final Method method;
    private String signature;

    private ReflectionMethodDescriptor(Method method, String modifiers, String declaringClass, String methodName, String returnType,
                                       List<String> parameterTypes) {
        super(modifiers, declaringClass, methodName, returnType, parameterTypes);
        this.method = method;
    }

    public static ReflectionMethodDescriptor of(Method method) {
        String modifiers = Modifier.toString(method.getModifiers());

        return new ReflectionMethodDescriptor(
                method,
                modifiers,
                descriptor(method.getDeclaringClass()),
                method.getName(),
                descriptor(method.getReturnType()),
                Arrays.stream(method.getParameterTypes())
                        .map(ReflectionMethodDescriptor::descriptor)
                        .toList()
        );
    }

    @Override
    public String getSignature() {
        String result = this.signature;
        if (result == null) {
            String modifiers = Modifier.toString(method.getModifiers());
            String prefix = modifiers.isBlank() ? "" : modifiers + " ";

            result = prefix + method.getReturnType().getTypeName() + " "
                    + method.getDeclaringClass().getTypeName() + "." + method.getName()
                    + Arrays.stream(method.getParameterTypes()).map(Class::getTypeName).collect(Collectors.joining(",", "(", ")"));
            this.signature = result;
        }
        return result;
    }

    private static String descriptor(Class<?> type) {
        return type.descriptorString();
    }
}