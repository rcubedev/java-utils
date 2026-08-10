package com.github.rcubedev.utils.event.impl.descriptor.method;

import com.github.rcubedev.utils.event.impl.subscriber.MethodKey;

import java.lang.invoke.MethodType;
import java.util.List;
import java.util.stream.Collectors;

public final class MethodKeyDescriptor extends MethodDescriptor {

    private final MethodKey key;
    private String signature;

    private MethodKeyDescriptor(MethodKey key, String modifiers, String declaringClass, String methodName, String returnType, List<String> parameterTypes) {
        super(modifiers, declaringClass, methodName, returnType, parameterTypes);
        this.key = key;
    }

    public static MethodKeyDescriptor of(MethodKey key) {
        MethodType type = key.type();

        return new MethodKeyDescriptor(
                key,
                key.modifiers(),
                descriptor(key.clazz()),
                key.methodName(),
                descriptor(type.returnType()),
                type.parameterList().stream()
                        .map(MethodKeyDescriptor::descriptor)
                        .toList()
        );
    }

    @Override
    public String getSignature() {
        String result = this.signature;
        if (result == null) {
            result = key.modifiers().isBlank() ? "" : key.modifiers() + " " + key.type().returnType().getTypeName() + " "
                    + key.clazz().getTypeName() + "." + key.methodName()
                    + key.type().parameterList().stream().map(Class::getTypeName).collect(Collectors.joining(",", "(", ")"));
            this.signature = result;
        }
        return result;
    }

    private static String descriptor(Class<?> type) {
        return type.descriptorString();
    }
}
