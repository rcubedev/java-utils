package com.example.reflect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class TypedClassTest {

    @Test
    @DisplayName("Basic Inheritance: String should be assignable to CharSequence")
    void testBasicInheritance() {
        TypedClass<String> stringType = new TypedClass<>() {};
        assertTrue(stringType.isAssignableTo(CharSequence.class));
        assertFalse(stringType.isAssignableTo(Integer.class));
    }

    @Test
    @DisplayName("Invariance: List<String> is NOT assignable to List<Object>")
    void testInvariance() {
        TypedClass<List<String>> listString = new TypedClass<>() {};
        TypedClass<List<Object>> listObject = new TypedClass<>() {};

        // JLS Invariance rule
        assertFalse(listString.isAssignableTo(listObject),
                "List<String> must not be assignable to List<Object> due to invariance.");
    }

    @Test
    @DisplayName("Covariance with Wildcards: List<String> is assignable to List<? extends CharSequence>")
    void testWildcardUpperBounds() {
        TypedClass<List<String>> source = new TypedClass<>() {};
        TypedClass<List<? extends CharSequence>> target = new TypedClass<>() {};

        assertTrue(source.isAssignableTo(target),
                "List<String> should satisfy List<? extends CharSequence>.");
    }

    @Test
    @DisplayName("Lower Bounds: Object is assignable to ? super String")
    void testWildcardLowerBounds() {
        TypedClass<Object> source = new TypedClass<>() {};
        TypedClass<List<? super String>> targetList = new TypedClass<>() {};

        // We need to extract the type argument manually for a clean test of the static method
        java.lang.reflect.ParameterizedType pt = (java.lang.reflect.ParameterizedType) targetList.getType();
        java.lang.reflect.Type wildcard = pt.getActualTypeArguments()[0];

        assertTrue(TypedClass.isAssignableTo(wildcard, Object.class),
                "Object should satisfy ? super String.");
        assertFalse(TypedClass.isAssignableTo(wildcard, Integer.class),
                "Integer should not satisfy ? super String.");
    }

    @Test
    @DisplayName("Recursive Generics: Map<String, List<String>> check")
    void testRecursiveGenerics() {
        TypedClass<Map<String, List<String>>> source = new TypedClass<>() {};
        TypedClass<Map<String, List<? extends CharSequence>>> target = new TypedClass<>() {};
        TypedClass<Map<String, List<Integer>>> invalidTarget = new TypedClass<>() {};

        assertTrue(source.isAssignableTo(source));
        assertTrue(target.isAssignableTo(target));
        assertFalse(source.isAssignableTo(target));
        assertFalse(source.isAssignableTo(invalidTarget));
    }

    @Test
    @DisplayName("Primitives: int.class should be handled via ofPrimitive")
    void testPrimitives() {
        TypedClass<Integer> intPrim = TypedClass.ofPrimitive(int.class);

        assertEquals(int.class, intPrim.getTypedClass());
        // Note: isAssignableTo(Integer.class) might depend on your JlsReflectionHelper's
        // boxing logic, but the raw class check should work:
        assertTrue(intPrim.isAssignableTo(int.class));
    }

    @Test
    @DisplayName("Type Variables: Check bounds satisfaction")
    void testTypeVariables() {
        // Mocking a TypeVariable bound is hard without a real class,
        // but we can test if String satisfies a bound of Comparable
        TypedClass<String> source = new TypedClass<>() {};

        // This simulates a constructor like <T extends Comparable<T>>
        assertTrue(source.isAssignableTo(Comparable.class));
    }
}