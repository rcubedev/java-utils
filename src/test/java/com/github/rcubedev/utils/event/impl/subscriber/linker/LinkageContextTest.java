package com.github.rcubedev.utils.event.impl.subscriber.linker;

import com.github.rcubedev.utils.event.api.TestEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

class LinkageContextTest {

    private MethodHandles.Lookup lookup;
    private Method testMethod;
    private Class<TestEvent> paramType;

    // Dummy class to extract a Method reference from
    static class SampleListener {
        public void onEvent(TestEvent event) {}
    }

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        this.lookup = MethodHandles.lookup();
        this.testMethod = SampleListener.class.getMethod("onEvent", TestEvent.class);
        this.paramType = TestEvent.class;
    }

    @Test
    void testRecordStatePreservation() {
        LinkageContext<TestEvent> context = new LinkageContext<>(lookup, testMethod, paramType);

        assertSame(lookup, context.lookup(), "Lookup instance should match the constructor argument");
        assertSame(testMethod, context.method(), "Method instance should match the constructor argument");
        assertSame(paramType, context.paramType(), "Param type class should match the constructor argument");
    }

    @Test
    void testTargetClassResolution() {
        LinkageContext<TestEvent> context = new LinkageContext<>(lookup, testMethod, paramType);

        Class<?> expectedDeclaringClass = SampleListener.class;
        assertEquals(expectedDeclaringClass, context.targetClass(), 
                "targetClass() must correctly resolve the declaring class of the method");
    }

    @Test
    void testEqualsAndHashCode() {
        LinkageContext<TestEvent> context1 = new LinkageContext<>(lookup, testMethod, paramType);
        LinkageContext<TestEvent> context2 = new LinkageContext<>(lookup, testMethod, paramType);

        assertEquals(context1, context2, "Records with identical components must be equal");
        assertEquals(context1.hashCode(), context2.hashCode(), "Equal records must have matching hash codes");
    }

    @Test
    void testToStringContainsComponents() {
        LinkageContext<TestEvent> context = new LinkageContext<>(lookup, testMethod, paramType);
        String toStringResult = context.toString();

        assertTrue(toStringResult.contains("lookup"), "toString should output the lookup component name");
        assertTrue(toStringResult.contains("method"), "toString should output the method component name");
        assertTrue(toStringResult.contains("paramType"), "toString should output the paramType component name");
    }
}