package com.github.rcubedev.utils.reflect;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JlsReflectionHelperTest {

    // --- Test Subject Classes ---
    public static class OverloadTarget {
        public final String result;
        public OverloadTarget(String s) { this.result = "String"; }
        public OverloadTarget(CharSequence cs) { this.result = "CharSequence"; }
        public OverloadTarget(int i) { this.result = "primitive"; }
        public OverloadTarget(Integer i) { this.result = "wrapper"; }
    }

    public static class VarArgsTarget {
        public final int length;
        public VarArgsTarget(int... nums) { this.length = nums.length; }
        public VarArgsTarget(String... strings) { this.length = strings.length; }
    }

    @Test
    @DisplayName("Phase 1: Widening & Identity using Builder")
    void testPhase1WithBuilder() {
        // String is more specific than CharSequence
        Argument<String> arg = Argument.Builder.of("test", new TypedClass<String>() {});

        OverloadTarget target = JlsReflectionHelper.getInstance(OverloadTarget.class, MethodHandles.lookup()).instantiate(arg);
        assertEquals("String", target.result);
    }

    @Test
    @DisplayName("Phase 2: Boxing using Primitive Builder")
    void testPhase2WithPrimitiveBuilder() {
        // Use the specialized primitive builder
        Argument<Integer> arg = Argument.Builder.of(10); // returns Argument<Integer> with int.class

        OverloadTarget target = JlsReflectionHelper.getInstance(OverloadTarget.class, MethodHandles.lookup()).instantiate(arg);
        assertEquals("primitive", target.result);
    }

    @Test
    @DisplayName("Phase 3: VarArgs using VarArgs Builder")
    void testPhase3WithVarArgsBuilder() {
        // Testing primitive varargs: int...
        Argument<int[]> arg = Argument.Builder.VarArgs.of(1, 2, 3, 4, 5);

        VarArgsTarget target = JlsReflectionHelper.getInstance(VarArgsTarget.class, MethodHandles.lookup()).instantiate(arg);
        assertEquals(5, target.length);
    }

    @Test
    @DisplayName("Generics: List<String> to List<? extends CharSequence>")
    void testGenericsWithBuilder() {
        record GenericReceiver(List<? extends CharSequence> items) {}

        // List<String>
        Argument<List<String>> arg = Argument.Builder.of(
                List.of("A", "B"),
                new TypedClass<List<String>>() {}
        );

        GenericReceiver receiver = assertDoesNotThrow(() -> JlsReflectionHelper.getInstance(GenericReceiver.class, MethodHandles.lookup()).instantiate(arg));
        assertNotNull(receiver);
        assertEquals(2, receiver.items.size());
    }

    @Test
    @DisplayName("Generics: List<Collection<String>> to List<? extends Collection<? extends CharSequence>")
    void testGenericsWithBuilder2() {
        record GenericReceiver(List<? extends Collection<? extends CharSequence>> items) {}

        Argument<List<Collection<String>>> arg = Argument.Builder.of(
                List.of(List.of("A", "B")),
                new TypedClass<>() {}
        );

        GenericReceiver receiver = assertDoesNotThrow(() -> JlsReflectionHelper.getInstance(GenericReceiver.class, MethodHandles.lookup()).instantiate(arg));
        assertNotNull(receiver);
        assertEquals(1, receiver.items.size(), "Items: " +  receiver.items);
        assertNotNull(receiver.items.getFirst(), "First item is null");
        assertEquals(2, receiver.items.getFirst().size(), "First List Items: " +  receiver.items.getFirst());
    }

    @Test
    @DisplayName("Generics: List<String> to List<? super Class<?>>")
    void testGenericsWithBuilder3() {
        record GenericReceiver(List<? super Class<?>> items) {}

        Argument<List<Object>> arg = Argument.Builder.of(
                List.of("A", "B"),
                new TypedClass<>() {}
        );

        assertDoesNotThrow(() -> JlsReflectionHelper.getInstance(GenericReceiver.class, MethodHandles.lookup()).instantiate(arg));
    //     etc
    }

    @Test
    @DisplayName("Generics: List<Collection<String>> to List<? super List<Object>")
    void testBadGenericsWithBuilder() {
        record GenericReceiver(List<? super List<Object>> items) {}

        Argument<List<Collection<String>>> arg = Argument.Builder.of(
                List.of(List.of("A", "B")),
                new TypedClass<>() {}
        );
        // List<Collection<String>> aaa = List.of(List.of("A", "B"));
        // new GenericReceiver(aaa); // <-- this wouldn't work in java but does work in my reflection helper for some reason.

        GenericReceiver receiver = JlsReflectionHelper.getInstance(GenericReceiver.class, MethodHandles.lookup()).instantiate(arg); // this should throw but doesn't.
        // assertNotNull(receiver);
        // assertEquals(1, receiver.items.size(), "Items: " +  receiver.items);
        // // assertNotNull(receiver.items.getFirst(), "First item is null");
        // // assertEquals(2, receiver.items.getFirst().size(), "First List Items: " +  receiver.items.getFirst());
    }

    @Test
    @DisplayName("Varargs Object: String... using Separate Arguments")
    void testObjectVarArgs() {
        Argument<String> arg1 = Argument.Builder.of("one", new TypedClass<>(){});
        Argument<String> arg2 = Argument.Builder.of("two", new TypedClass<>(){});

        VarArgsTarget target = JlsReflectionHelper.getInstance(VarArgsTarget.class, MethodHandles.lookup()).instantiate(arg1, arg2);
        assertEquals(2, target.length);
    }
}