package com.example.reflect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;

class ArgumentTest {

    @Nested
    @DisplayName("Reference Type Arguments")
    class ReferenceTypes {
        @Test
        void testSuperTypeConstraints() {
            // Testing that Argument<T> accepts TypedClass<? super T>
            // This is crucial for JLS §15.12 polymorphism
            TypedClass<CharSequence> charSeqType = new TypedClass<>() {};
            Argument<String> arg = Argument.Builder.of("Hello", charSeqType);

            assertEquals("Hello", arg.get());
            assertEquals(charSeqType, arg.getStaticType());
        }

        @Test
        void testNullArgumentForReferenceType() {
            TypedClass<List<String>> listType = new TypedClass<>() {};
            Argument<List<String>> arg = Argument.Builder.of(null, listType);

            assertNull(arg.get());
            assertEquals(Argument.Kind.STANDARD, arg.getKind());
        }
    }

    @Nested
    @DisplayName("Primitive & Varargs Arguments")
    class PrimitiveAndVarargs {
        @Test
        void testPrimitiveIntBuilder() {
            Argument<Integer> arg = Argument.Builder.of(100);

            assertEquals(100, arg.get());
            assertTrue(arg.getStaticType().getTypedClass().isPrimitive());
            assertEquals(int.class, arg.getStaticType().getTypedClass());
        }

        @Test
        void testPrimitiveArrayVarargs() {
            // Tests Argument.Builder.VarArgs.of(int...)
            Argument<int[]> arg = Argument.Builder.VarArgs.of(1, 2, 3);

            assertArrayEquals(new int[]{1, 2, 3}, arg.get());
            assertEquals(int[].class, arg.getStaticType().getTypedClass());
            assertEquals(Argument.Kind.VAR_ARGS, arg.getKind());
        }

        @Test
        void testObjectArrayVarargs() {
            // Tests String... varargs
            TypedClass<String[]> type = new TypedClass<>() {};
            Argument<String[]> arg = Argument.Builder.VarArgs.of(type, "A", "B");

            assertArrayEquals(new String[]{"A", "B"}, arg.get());
            assertEquals(Argument.Kind.VAR_ARGS, arg.getKind());
        }
    }

    @Nested
    @DisplayName("High-Fidelity Type Matching (isAssignableTo)")
    class TypeMatching {



        @Test
        void testGenericInvariance() {
            TypedClass<List<String>> stringList = new TypedClass<>() {};
            TypedClass<List<Object>> objectList = new TypedClass<>() {};

            // List<String> is NOT a List<Object>
            assertFalse(stringList.isAssignableTo(objectList.getType()));
        }

        @Test
        void testWildcardCovariance() {
            TypedClass<ArrayList<String>> concreteList = new TypedClass<>() {};
            TypedClass<List<? extends CharSequence>> wildcardList = new TypedClass<>() {};

            // ArrayList<String> IS a List<? extends CharSequence>
            assertTrue(concreteList.isAssignableTo(wildcardList.getType()));
        }

        @Test
        void testRecursiveLowerBounds() {
            TypedClass<List<Object>> source = new TypedClass<>() {};
            TypedClass<List<? super String>> target = new TypedClass<>() {};

            List<Object> a = new ArrayList<>();
            List<? super String> b = new ArrayList<>();
            assertTrue(source.isAssignableTo(target));
            assertFalse(target.isAssignableTo(source));
        }
    }

    @Nested
    @DisplayName("Safety & Internal Guardrails")
    class Safety {
        @Test
        void testPrimitiveVarargsCannotContainNull() { // target internal API
            assertThrows(NullPointerException.class, () -> Argument.ofPrimitiveVarArgs(TypedClass.ofPrimitiveArray(int[].class), new Integer[]{1, null, 3}));
        }

        @Test
        void testObjectVarargsCanContainNull() {
            // Attempting to put null into non-primitive var args should succeed
            assertDoesNotThrow(() -> Argument.Builder.VarArgs.of(new TypedClass<>(){}, 1, null, 3));
        }

        @Test
        void testTypedClassPrimitiveArrayIdentity() {
            TypedClass<int[]> type1 = TypedClass.ofPrimitiveArray(int[].class);
            TypedClass<int[]> type2 = TypedClass.ofPrimitiveArray(int[].class);

            assertSame(type1, type2, "Primitive array TypedClass should be memoized");
        }
    }
}