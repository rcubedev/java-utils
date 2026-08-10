package com.github.rcubedev.utils.event.impl.descriptor.method;

import com.github.rcubedev.utils.event.api.descriptor.method.MethodDescriptor;

import java.util.List;

public class BasicMethodDescriptor extends MethodDescriptor {

    public BasicMethodDescriptor(String modifiers, String declaringClass, String methodName, String returnType, List<String> parameterTypes) {
        super(modifiers, declaringClass, methodName, returnType, parameterTypes);
    }
}
