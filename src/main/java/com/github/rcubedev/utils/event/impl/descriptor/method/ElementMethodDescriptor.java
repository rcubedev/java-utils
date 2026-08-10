package com.github.rcubedev.utils.event.impl.descriptor.method;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;
import java.util.stream.Collectors;

public final class ElementMethodDescriptor extends MethodDescriptor {

    private final String signature;

    private ElementMethodDescriptor(String modifiers, String declaringClass, String methodName, String returnType,
            List<String> parameterTypes, String signature) {
        super(modifiers, declaringClass, methodName, returnType, parameterTypes);
        this.signature = signature;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSignature() {
        return this.signature;
    }

    public static ElementMethodDescriptor of(ExecutableElement method, Types typeUtils, Elements elementUtils) {
        TypeElement enclosing = (TypeElement) method.getEnclosingElement();
        String modifiers = method.getModifiers().stream()
                .map(Modifier::toString)
                .collect(Collectors.joining(" "));

        String declaringClass = descriptor(enclosing, elementUtils);
        String methodName = method.getSimpleName().toString();
        String returnType = descriptor(method.getReturnType(), typeUtils, elementUtils);

        List<String> parameterTypes = method.getParameters().stream()
                .map(parameter -> descriptor(
                        parameter.asType(),
                        typeUtils,
                        elementUtils
                ))
                .toList();

        String signature = buildSignature(method, typeUtils, modifiers);
        return new ElementMethodDescriptor(modifiers, declaringClass, methodName,returnType, parameterTypes, signature);
    }

    private static String buildSignature(ExecutableElement method, Types typeUtils, String modifiers) {
        String prefix = modifiers.isEmpty() ? "" : modifiers + " ";
        String returnType = typeUtils.erasure(method.getReturnType()).toString();

        String declaringClass = ((TypeElement) method.getEnclosingElement()).getQualifiedName().toString();
        String parameters = method.getParameters().stream()
                .map(parameter -> typeUtils.erasure(parameter.asType()).toString())
                .collect(Collectors.joining(",", "(", ")"));

        return prefix + returnType + " " + declaringClass + "." + method.getSimpleName() + parameters;
    }

    private static String descriptor(TypeElement type, Elements elementUtils) {
        return "L" + elementUtils.getBinaryName(type) + ";";
    }

    private static String descriptor(TypeMirror type, Types typeUtils, Elements elementUtils) {
        TypeMirror erased = typeUtils.erasure(type);

        return switch (erased.getKind()) {
            case BOOLEAN -> "Z";
            case BYTE    -> "B";
            case SHORT   -> "S";
            case INT     -> "I";
            case LONG    -> "J";
            case CHAR    -> "C";
            case FLOAT   -> "F";
            case DOUBLE  -> "D";
            case VOID    -> "V";

            case ARRAY -> "[" + descriptor(
                    ((ArrayType) erased).getComponentType(),
                    typeUtils,
                    elementUtils
            );

            case DECLARED -> descriptor(
                    (TypeElement) typeUtils.asElement(erased),
                    elementUtils
            );

            default -> throw new IllegalArgumentException(
                    "Unsupported type: " + erased
            );
        };
    }
}