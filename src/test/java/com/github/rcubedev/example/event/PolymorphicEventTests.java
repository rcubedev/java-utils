package com.github.rcubedev.example.event;

import com.github.rcubedev.example.event.api.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PolymorphicEventTests {

    // Test event hierarchy
    static abstract class TestEvent extends Event {
        @Override
        public abstract EventHandler<? extends TestEvent> handler();
    }

    static class ParentEvent extends TestEvent {
        private static final EventHandler<ParentEvent> HANDLER =
                EventHandlerFactory.createArrayBacked(ParentEvent.class);

        @Override
        public EventHandler<? extends ParentEvent> handler() {
            return HANDLER;
        }
    }

    static class ChildEvent extends ParentEvent {
        private static final EventHandler<ChildEvent> HANDLER =
                EventHandlerFactory.createArrayBacked(ChildEvent.class);

        @Override
        public EventHandler<? extends ChildEvent> handler() {
            return HANDLER;
        }
    }

    static class GrandchildEvent extends ChildEvent {
        private static final EventHandler<GrandchildEvent> HANDLER =
                EventHandlerFactory.createArrayBacked(GrandchildEvent.class);

        @Override
        public EventHandler<? extends GrandchildEvent> handler() {
            return HANDLER;
        }
    }

    private List<String> eventLog;

    @BeforeEach
    void setUp() {
        eventLog = new ArrayList<>();
    }

    @Test
    void testParentListenerReceivesChildEvent() {
        ParentEvent.HANDLER.register(event -> {
            eventLog.add("parent:" + event.getClass().getSimpleName());
        });

        ChildEvent event = new ChildEvent();
        event.dispatch();

        assertEquals(1, eventLog.size());
        assertEquals("parent:ChildEvent", eventLog.getFirst());
    }

    @Test
    void testChildListenerReceivesOwnEvent() {
        ChildEvent.HANDLER.register(event -> {
            eventLog.add("child");
        });

        ChildEvent event = new ChildEvent();
        event.dispatch();

        assertEquals(1, eventLog.size());
        assertEquals("child", eventLog.getFirst());
    }

    @Test
    void testBothListenersReceiveEvent() {
        ParentEvent.HANDLER.register(event -> {
            eventLog.add("parent");
        });

        ChildEvent.HANDLER.register(event -> {
            eventLog.add("child");
        });

        ChildEvent event = new ChildEvent();
        event.dispatch();

        assertEquals(2, eventLog.size());
        assertTrue(eventLog.contains("parent"));
        assertTrue(eventLog.contains("child"));
    }

    @Test
    void testPrioritiesRespected() {
        ParentEvent.HANDLER.register(Priority.LOW, event -> {
            eventLog.add("parent:LOW");
        });
        ParentEvent.HANDLER.register(Priority.HIGH, event -> {
            eventLog.add("parent:HIGH");
        });

        ChildEvent.HANDLER.register(Priority.NORMAL, event -> {
            eventLog.add("child:NORMAL");
        });
        ChildEvent.HANDLER.register(Priority.LOWEST, event -> {
            eventLog.add("child:LOWEST");
        });

        ChildEvent event = new ChildEvent();
        event.dispatch();

        // Should be ordered: LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR
        assertEquals(4, eventLog.size());
        assertEquals("child:LOWEST", eventLog.get(0));
        assertEquals("parent:LOW", eventLog.get(1));
        assertEquals("child:NORMAL", eventLog.get(2));
        assertEquals("parent:HIGH", eventLog.get(3));
    }

    @Test
    void testMultipleLevelsOfHierarchy() {
        ParentEvent.HANDLER.register(event -> {
            eventLog.add("parent");
        });

        ChildEvent.HANDLER.register(event -> {
            eventLog.add("child");
        });

        GrandchildEvent.HANDLER.register(event -> {
            eventLog.add("grandchild");
        });

        GrandchildEvent event = new GrandchildEvent();
        event.dispatch();

        assertEquals(3, eventLog.size());
        assertTrue(eventLog.contains("parent"));
        assertTrue(eventLog.contains("child"));
        assertTrue(eventLog.contains("grandchild"));
    }

    @Test
    void testDispatchMethod() {
        ParentEvent.HANDLER.register(event -> {
            eventLog.add("parent");
        });

        ChildEvent.HANDLER.register(event -> {
            eventLog.add("child");
        });

        ChildEvent event = new ChildEvent();
        event.dispatch();

        assertEquals(2, eventLog.size());
        assertTrue(eventLog.contains("parent"));
        assertTrue(eventLog.contains("child"));
    }

    @Test
    void testParentDoesNotReceiveUnrelatedEvent() {
        ParentEvent.HANDLER.register(event -> {
            eventLog.add("parent");
        });

        ParentEvent event = new ParentEvent();
        event.dispatch();

        assertEquals(1, eventLog.size());
        assertEquals("parent", eventLog.getFirst());
    }

    @Test
    void testNoListenersDoesNotCrash() {
        ParentEvent event = new ParentEvent();
        event.dispatch();
        // Should not throw, should complete successfully
        assertTrue(true);
    }

    @Test
    void testMultipleListenersOnSameHandler() {
        ChildEvent.HANDLER.register(event -> {
            eventLog.add("listener1");
        });
        ChildEvent.HANDLER.register(event -> {
            eventLog.add("listener2");
        });
        ChildEvent.HANDLER.register(event -> {
            eventLog.add("listener3");
        });

        ChildEvent event = new ChildEvent();
        event.dispatch();

        assertEquals(3, eventLog.size());
        assertTrue(eventLog.contains("listener1"));
        assertTrue(eventLog.contains("listener2"));
        assertTrue(eventLog.contains("listener3"));
    }

    @Test
    void testPriorityMonitor() {
        ParentEvent.HANDLER.register(Priority.MONITOR, event -> {
            eventLog.add("monitor");
        });
        ParentEvent.HANDLER.register(Priority.NORMAL, event -> {
            eventLog.add("normal");
        });

        ChildEvent.HANDLER.register(event -> {
            eventLog.add("child");
        });

        ChildEvent event = new ChildEvent();
        event.dispatch();

        assertEquals(3, eventLog.size());
        // MONITOR should be last
        assertEquals("monitor", eventLog.get(2));
    }

    // not needed as java inits parent class first if trying to init a subclass
    // @Test
    // void testLateParentRegistration() {
    //     // Child handler exists first, parent created after
    //     // This simulates parent handler initialized after child
    //     EventHandler<ChildEvent> childHandler = EventHandlerFactory.createArrayBacked(ChildEvent.class);
    //     EventHandler<ParentEvent> parentHandler = EventHandlerFactory.createArrayBacked(ParentEvent.class);
    //
    //     parentHandler.register(event -> eventLog.add("parent"));
    //     childHandler.register(event -> eventLog.add("child"));
    //
    //     // dispatch via childHandler's invoker directly since we can't use the static HANDLER fields
    //     childHandler.invoker().process(new ChildEvent());
    //
    //     assertEquals(2, eventLog.size());
    //     assertTrue(eventLog.contains("parent"));
    //     assertTrue(eventLog.contains("child"));
    // }
}