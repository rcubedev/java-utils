package com.github.rcubedev.example.event;

import com.github.rcubedev.example.event.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SubscribeEventTests {

    private static class TestEvent extends Event {
        private static final EventHandler<TestEvent> EVENT =
            EventHandlerFactory.createArrayBacked(TestEvent.class);

        @Override
        public EventHandler<? extends TestEvent> handler() {
            return EVENT;
        }
    }

    private static class CancellableTestEvent extends CancellableEvent {
        private static final EventHandler<CancellableTestEvent> EVENT =
            EventHandlerFactory.createArrayBacked(CancellableTestEvent.class);

        @Override
        public EventHandler<? extends CancellableTestEvent> handler() {
            return EVENT;
        }
    }

    private List<String> eventLog;

    @BeforeEach
    void setUp() {
        eventLog = new ArrayList<>();
    }

    @Test
    void testInstanceListenerRegistration() {
        class Listener {
            @SubscribeEvent
            public void onEvent(TestEvent event) {
                eventLog.add("instance");
            }
        }

        Listener listener = new Listener();
        TestEvent.EVENT.register(listener);

        TestEvent event = new TestEvent();
        event.dispatch();

        assertEquals(1, eventLog.size());
        assertEquals("instance", eventLog.getFirst());
    }

    @Test
    void testStaticListenerRegistration() {
        class Listener {
            @SubscribeEvent
            public static void onEvent(TestEvent event) {
                // Can't access eventLog from static context, so we'll just verify it registers
            }
        }

        TestEvent.EVENT.register(Listener.class);
        // If no exception, registration was successful
        assertTrue(true);
    }

    @Test
    void testMultipleInstanceListeners() {
        class Listener {
            @SubscribeEvent
            public void onEvent1(TestEvent event) {
                eventLog.add("listener1");
            }

            @SubscribeEvent
            public void onEvent2(TestEvent event) {
                eventLog.add("listener2");
            }
        }

        Listener listener = new Listener();
        TestEvent.EVENT.register(listener);

        TestEvent event = new TestEvent();
        event.dispatch();

        assertEquals(2, eventLog.size());
        assertTrue(eventLog.contains("listener1"));
        assertTrue(eventLog.contains("listener2"));
    }

    @Test
    void testPriorityRegistration() {
        class Listener {
            @SubscribeEvent(priority = Priority.HIGH)
            public void onEvent(TestEvent event) {
                eventLog.add("high");
            }
        }

        Listener listener = new Listener();
        TestEvent.EVENT.register(listener);

        TestEvent event = new TestEvent();
        event.dispatch();

        assertEquals(1, eventLog.size());
        assertEquals("high", eventLog.getFirst());
    }

    @Test
    void testIgnoreCancelledDefault() {
        class Listener {
            @SubscribeEvent
            public void onEvent(CancellableTestEvent event) {
                eventLog.add("called");
            }
        }

        Listener listener = new Listener();
        CancellableTestEvent.EVENT.register(listener);

        CancellableTestEvent event = new CancellableTestEvent();
        event.cancel();
        event.dispatch();

        // Default ignoreCancelled=false, so it should still be called
        assertEquals(1, eventLog.size());
    }

    @Test
    void testIgnoreCancelledTrue() {
        class Listener {
            @SubscribeEvent(ignoreCancelled = true)
            public void onEvent(CancellableTestEvent event) {
                eventLog.add("called");
            }
        }

        Listener listener = new Listener();
        CancellableTestEvent.EVENT.register(listener);

        CancellableTestEvent event = new CancellableTestEvent();
        event.cancel();
        event.dispatch();

        // ignoreCancelled=true, so it should not be called
        assertEquals(0, eventLog.size());
    }

    @Test
    void testNullListenerThrows() {
        assertThrows(IllegalArgumentException.class, () -> {
            TestEvent.EVENT.register((Object) null);
        });
    }

    @Test
    void testNoSubscribeEventMethodsThrows() {
        class EmptyListener {
            public void someMethod(TestEvent event) {
                // No @SubscribeEvent annotation
            }
        }

        EmptyListener listener = new EmptyListener();
        assertThrows(IllegalArgumentException.class, () -> TestEvent.EVENT.register(listener), "has no @SubscribeEvent methods");
    }

    @Test
    void testNonPublicMethodThrows() {
        class Listener {
            @SubscribeEvent
            private void onEvent(TestEvent event) {
                eventLog.add("called");
            }
        }

        Listener listener = new Listener();
        assertThrows(IllegalArgumentException.class, () -> TestEvent.EVENT.register(listener), "must be public");
    }

    @Test
    void testWrongParameterCountThrows() {
        class Listener {
            @SubscribeEvent
            public void onEvent(TestEvent event, String extra) {
                eventLog.add("called");
            }
        }

        Listener listener = new Listener();
        assertThrows(IllegalArgumentException.class, () -> TestEvent.EVENT.register(listener), "single argument");
    }

    @Test
    void testWrongParameterTypeThrows() {
        class Listener {
            @SubscribeEvent
            public void onEvent(String notAnEvent) {
                eventLog.add("called");
            }
        }

        Listener listener = new Listener();
        assertThrows(IllegalArgumentException.class, () -> TestEvent.EVENT.register(listener), "Event subtype");
    }

    @Test
    void testNonVoidReturnTypeThrows() {
        class Listener {
            @SubscribeEvent
            public int onEvent(TestEvent event) {
                return 0;
            }
        }

        Listener listener = new Listener();
        assertThrows(IllegalArgumentException.class, () -> TestEvent.EVENT.register(listener), "return void");
    }

    @Test
    void testStaticMethodMismatchThrows() {
        class Listener {
            @SubscribeEvent
            public static void onEvent(TestEvent event) {
                // static method
            }
        }

        //noinspection InstantiationOfUtilityClass
        Listener listener = new Listener();
        assertThrows(IllegalArgumentException.class, () -> TestEvent.EVENT.register(listener), "NOT be static");
    }

    @Test
    void testSuperTypeWithSubscribeEventThrows() {
        class BaseListener {
            @SubscribeEvent
            public void onEvent(TestEvent event) {
                eventLog.add("base");
            }
        }

        class ChildListener extends BaseListener {
            // No @SubscribeEvent methods
        }

        ChildListener listener = new ChildListener();
        assertThrows(IllegalArgumentException.class, () -> TestEvent.EVENT.register(listener), "supertype");
    }

    @Test
    void testMethodDirectRegistration() throws NoSuchMethodException {
        class Listener {
            @SubscribeEvent
            public static void onEvent(TestEvent event) {
                // static method for direct registration
            }
        }

        //noinspection JavaReflectionMemberAccess
        Method method = Listener.class.getDeclaredMethod("onEvent", TestEvent.class);
        TestEvent.EVENT.register(method);

        // If no exception, registration was successful
        assertTrue(true);
    }

    @Test
    void testNonStaticMethodDirectRegistrationThrows() throws NoSuchMethodException {
        class Listener {
            @SubscribeEvent
            public void onEvent(TestEvent event) {
                // instance method
            }
        }

        //noinspection JavaReflectionMemberAccess
        Method method = Listener.class.getDeclaredMethod("onEvent", TestEvent.class);
        assertThrows(IllegalArgumentException.class, () -> TestEvent.EVENT.register(method), "not static");
    }
}