package com.github.rcubedev.utils.reflect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class TypedClassTest {

    @Test
    void testBasicInheritance() {
        // String should be assignable to CharSequence
        TypedClass<String> stringType = new TypedClass<>() {};
        assertTrue(stringType.isAssignableTo(CharSequence.class));
        assertFalse(stringType.isAssignableTo(Integer.class));
    }

    @Test
    void testInvariance() {
        // List<String> is NOT assignable to List<Object>
        TypedClass<List<String>> listString = new TypedClass<>() {};
        TypedClass<List<Object>> listObject = new TypedClass<>() {};

        // JLS Invariance rule
        assertFalse(listString.isAssignableTo(listObject),
                "List<String> must not be assignable to List<Object> due to invariance.");
    }

    @Test
    void testWildcardUpperBounds() {
        // Covariance w/ wildcards: List<String> assignable to List<? extends CharSequence>
        TypedClass<List<String>> source = new TypedClass<>() {};
        TypedClass<List<? extends CharSequence>> target = new TypedClass<>() {};

        assertTrue(source.isAssignableTo(target),
                "List<String> should satisfy List<? extends CharSequence>.");
    }

    @Test
    void testWildcardLowerBounds() {
        TypedClass<List<? super String>> target = new TypedClass<>() {};

        TypedClass<List<Object>> listObject = new TypedClass<>() {};
        assertTrue(listObject.isAssignableTo(target), "List<Object> should be assignable to List<? super String>");

        TypedClass<List<Integer>> listInteger = new TypedClass<>() {};
        assertFalse(listInteger.isAssignableTo(target), "List<Object> should not be assignable to List<? super Integer>");

        TypedClass<Object> object = new TypedClass<>() {};
        assertFalse(object.isAssignableTo(target), "Object should be assignable to List<? super String>");
        // assertTrue(TypedClass.isAssignableTo(wildcard, new TypedClass<List<Object>>(){}.getType()),
        //         "Object should satisfy ? super String.");
        // assertFalse(TypedClass.isAssignableTo(wildcard, Integer.class),
        //         "Integer should not satisfy ? super String.");
    }

    @Test
    void testRecursiveGenerics() {
        // Map<String, List<String>> check
        TypedClass<Map<String, List<String>>> source = new TypedClass<>() {};
        TypedClass<Map<String, List<? extends CharSequence>>> target = new TypedClass<>() {};
        TypedClass<Map<String, List<Integer>>> invalidTarget = new TypedClass<>() {};

        // Map<String, List<String>> a = new HashMap<>();
        // Map<String, List<? extends CharSequence>> b = new HashMap<>();
        // a = b;
        // b = a;
        assertTrue(source.isAssignableTo(source));
        assertTrue(target.isAssignableTo(target));
        assertFalse(source.isAssignableTo(target));
        assertFalse(source.isAssignableTo(invalidTarget));
    }

    @Test
    void testPrimitives() {
        TypedClass<Integer> intPrim = TypedClass.ofPrimitive(int.class);

        assertEquals(int.class, intPrim.getTypedClass());
        assertTrue(intPrim.isAssignableTo(int.class));
    }

    @Test
    void testTypeVariables() {
        TypedClass<String> source = new TypedClass<>() {};
        assertTrue(source.isAssignableTo(Comparable.class));
    }
}