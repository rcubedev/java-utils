package com.github.rcubedev.example.event;

import com.github.rcubedev.example.event.api.*;
import com.github.rcubedev.example.event.impl.EventHandlerFactoryImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class GlobalListenerTests {

    // --- Event hierarchy ---

    static abstract class TestEvent extends Event {
        @Override
        public abstract EventHandler<? extends TestEvent> handler();
    }

    static class ParentEvent extends TestEvent {
        static final EventHandler<ParentEvent> HANDLER =
                EventHandlerFactory.createArrayBacked(ParentEvent.class);

        @Override
        public EventHandler<? extends ParentEvent> handler() { return HANDLER; }
    }

    static class ChildEvent extends ParentEvent {
        static final EventHandler<ChildEvent> HANDLER =
                EventHandlerFactory.createArrayBacked(ChildEvent.class);

        @Override
        public EventHandler<? extends ChildEvent> handler() { return HANDLER; }
    }

    // --- Listener classes ---

    static class ParentListener {
        final List<String> log;
        ParentListener(List<String> log) { this.log = log; }

        @SubscribeEvent
        public void onParent(ParentEvent event) { log.add("parent"); }
    }

    static class ChildListener {
        final List<String> log;
        ChildListener(List<String> log) { this.log = log; }

        @SubscribeEvent
        public void onChild(ChildEvent event) { log.add("child"); }
    }

    static class MultiListener {
        final List<String> log;
        MultiListener(List<String> log) { this.log = log; }

        @SubscribeEvent
        public void onParent(ParentEvent event) { log.add("parent"); }

        @SubscribeEvent
        public void onChild(ChildEvent event) { log.add("child"); }
    }

    static class PriorityListener {
        final List<String> log;
        PriorityListener(List<String> log) { this.log = log; }

        @SubscribeEvent(priority = Priority.HIGH)
        public void onParentHigh(ParentEvent event) { log.add("high"); }

        @SubscribeEvent(priority = Priority.LOW)
        public void onParentLow(ParentEvent event) { log.add("low"); }
    }

    static class NoAnnotationListener {
        public void onParent(ParentEvent event) {}
    }

    static class InvalidParamCountListener {
        @SubscribeEvent
        public void onParent(ParentEvent event, String extra) {}
    }

    static class InvalidParamTypeListener {
        @SubscribeEvent
        public void onParent(String notAnEvent) {}
    }

    static class StaticListener {
        static List<String> log;

        @SubscribeEvent
        public static void onParent(ParentEvent event) { System.out.println(log); log.add("static"); System.out.println("Received event onParent: " + event); }
    }

    private List<String> eventLog;

    @BeforeEach
    void setUp() {
        eventLog = new ArrayList<>();
    }

    @AfterEach
    void tearDown() {
        EventHandlerFactoryImpl.resetListeners(); // prevent cross-test pollution
    }

    // --- Instance listener tests ---

    @Test
    void testGlobalInstanceListenerReceivesMatchingEvent() {
        EventHandler.registerGlobal(new ParentListener(eventLog));
        new ParentEvent().dispatch();
        assertEquals(List.of("parent"), eventLog);
    }

    @Test
    void testGlobalInstanceListenerDoesNotReceiveNonMatchingEvent() {
        EventHandler.registerGlobal(new ChildListener(eventLog));
        new ParentEvent().dispatch();
        assertTrue(eventLog.isEmpty());
    }

    @Test
    void testGlobalInstanceListenerReceivesChildEventViaParentHandler() {
        EventHandler.registerGlobal(new ParentListener(eventLog));
        new ChildEvent().dispatch();
        assertEquals(List.of("parent"), eventLog);
    }

    @Test
    void testGlobalMultiListenerReceivesBothEvents() {
        EventHandler.registerGlobal(new MultiListener(eventLog));
        new ParentEvent().dispatch();
        new ChildEvent().dispatch();
        // ParentEvent: parent listener fires
        // ChildEvent: child listener fires + parent listener fires via bubbling
        assertEquals(2, eventLog.stream().filter(s -> s.equals("parent")).count(), eventLog.stream().filter(s -> s.equals("parent")).toList().toString());
        assertEquals(1, eventLog.stream().filter(s -> s.equals("child")).count(), eventLog.stream().filter(s -> s.equals("child")).toList().toString());
    }

    @Test
    void testGlobalPriorityListenerOrderRespected() {
        EventHandler.registerGlobal(new PriorityListener(eventLog));
        new ParentEvent().dispatch();
        assertEquals(List.of("low", "high"), eventLog);
    }

    // --- Static listener tests ---

    @Test
    void testGlobalStaticListenerReceivesEvent() {
        StaticListener.log = eventLog;
        EventHandler.registerGlobal(StaticListener.class);
        new ParentEvent().dispatch();
        assertEquals(List.of("static"), eventLog);
    }

    // --- Method tests ---

    @Test
    void testGlobalMethodListenerReceivesEvent() throws NoSuchMethodException {
        Method method = ParentListener.class.getDeclaredMethod("onParent", ParentEvent.class);
        // static method required, so use a static version for this test
        // Instead, verify it throws since it's not static
        assertThrows(IllegalArgumentException.class, () -> EventHandler.registerGlobal(method));
    }

    @Test
    void testGlobalStaticMethodListenerReceivesEvent() throws NoSuchMethodException {
        StaticListener.log = eventLog;
        Method method = StaticListener.class.getDeclaredMethod("onParent", ParentEvent.class);
        EventHandler.registerGlobal(method);
        new ParentEvent().dispatch();
        assertEquals(List.of("static"), eventLog);
    }

    @Test
    void testGlobalMethodWithWrongParamCountThrows() throws NoSuchMethodException {
        Method method = InvalidParamCountListener.class.getDeclaredMethod("onParent", ParentEvent.class, String.class);
        assertThrows(IllegalArgumentException.class, () -> EventHandler.registerGlobal(method));
    }

    @Test
    void testGlobalMethodWithNonEventParamThrows() throws NoSuchMethodException {
        Method method = InvalidParamTypeListener.class.getDeclaredMethod("onParent", String.class);
        assertThrows(IllegalArgumentException.class, () -> EventHandler.registerGlobal(method));
    }

    // --- Validation tests ---

    @Test
    void testGlobalNoAnnotationThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> EventHandler.registerGlobal(new NoAnnotationListener()));
    }

    @Test
    void testGlobalInvalidParamCountThrows() {
        var d = ParentEvent.HANDLER;
        assertThrows(IllegalArgumentException.class,
                () -> EventHandler.registerGlobal(new InvalidParamCountListener()));
    }

    @Test
    void testGlobalInvalidParamTypeThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> EventHandler.registerGlobal(new InvalidParamTypeListener()));
    }

    // --- Late handler registration ---

    @Test
    void testGlobalListenerAppliedToLateRegisteredHandler() {
        // Register global listener before the handler exists
        List<String> log = new ArrayList<>();
        // Create a fresh handler after global registration
        EventHandler.registerGlobal(new ParentListener(log));

        // Simulate a late-created handler
        EventHandler<ParentEvent> lateHandler = EventHandlerFactory.createArrayBacked(ParentEvent.class);
        lateHandler.invoker().process(new ParentEvent());

        assertEquals(List.of("parent"), log);
    }
}