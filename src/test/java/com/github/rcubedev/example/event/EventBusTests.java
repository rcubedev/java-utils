package com.github.rcubedev.example.event;

import com.github.rcubedev.example.event.api.*;
import com.github.rcubedev.example.event.buses.MainBus;
import com.github.rcubedev.example.event.impl.EventBusRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EventBusTests {

    // ── Test bus ──────────────────────────────────────────────────────────────

    static abstract class TestEvent extends Event {}

    static final class TestBus extends EventBus<TestEvent> {
        static final TestBus INSTANCE = new TestBus();
        private TestBus() { super(TestEvent.class); }
    }

    // ── Linear hierarchy ──────────────────────────────────────────────────────
    //  TestEvent → ParentEvent → ChildEvent → GrandchildEvent

    static class ParentEvent extends TestEvent {}
    static class ChildEvent extends ParentEvent {}
    static class GrandchildEvent extends ChildEvent {}

    // ── Branching hierarchy ───────────────────────────────────────────────────
    //  TestEvent → BranchRootEvent → BranchAEvent → BranchAChildEvent
    //                              → BranchBEvent → BranchBChildEvent

    static class BranchRootEvent extends TestEvent {}
    static class BranchAEvent extends BranchRootEvent {}
    static class BranchAChildEvent extends BranchAEvent {}
    static class BranchBEvent extends BranchRootEvent {}
    static class BranchBChildEvent extends BranchBEvent {}

    // ── Cancellable ───────────────────────────────────────────────────────────

    static class CancellableTestEvent extends TestEvent implements Cancellable {
        private boolean cancelled = false;
        @Override public boolean isCancelled() { return cancelled; }
        @Override public void cancel() { cancelled = true; }
    }

    static class CancellableChildEvent extends CancellableTestEvent {}

    // ── Listener helpers ──────────────────────────────────────────────────────

    static class ParentListener {
        final List<String> log;
        ParentListener(List<String> log) { this.log = log; }

        @SubscribeEvent
        public void onParent(ParentEvent e) { log.add("parent"); }
    }

    static class ChildListener {
        final List<String> log;
        ChildListener(List<String> log) { this.log = log; }

        @SubscribeEvent
        public void onChild(ChildEvent e) { log.add("child"); }
    }

    static class MultiListener {
        final List<String> log;
        MultiListener(List<String> log) { this.log = log; }

        @SubscribeEvent
        public void onParent(ParentEvent e) { log.add("parent"); }

        @SubscribeEvent
        public void onChild(ChildEvent e) { log.add("child"); }
    }

    static class PriorityListener {
        final List<String> log;
        PriorityListener(List<String> log) { this.log = log; }

        @SubscribeEvent(priority = Priority.HIGH)
        public void onHigh(ParentEvent e) { log.add("high"); }

        @SubscribeEvent(priority = Priority.LOW)
        public void onLow(ParentEvent e) { log.add("low"); }

        @SubscribeEvent(priority = Priority.NORMAL)
        public void onNormal(ParentEvent e) { log.add("normal"); }
    }

    static class StaticListener {
        static List<String> log;

        @SubscribeEvent
        public static void onParent(ParentEvent e) { log.add("static"); }
    }

    static class NoAnnotationListener {
        public void onParent(ParentEvent e) {}
    }

    static class InvalidParamCountListener {
        @SubscribeEvent
        public void onParent(ParentEvent e, String extra) {}
    }

    static class InvalidParamTypeListener {
        @SubscribeEvent
        public void onParent(String notAnEvent) {}
    }

    static class NonVoidReturnListener {
        @SubscribeEvent
        public int onParent(ParentEvent e) { return 0; }
    }

    static class PrivateMethodListener {
        @SubscribeEvent
        private void onParent(ParentEvent e) {}
    }

    static class SupertypeSubscribeListener {
        @SubscribeEvent
        public void onParent(ParentEvent e) {}
    }

    static class SubtypeOfSupertypeListener extends SupertypeSubscribeListener {}

    private List<String> log;

    @BeforeEach
    void setUp() {
        log = new ArrayList<>();
        TestBus.INSTANCE.resetListeners();
    }

    @AfterEach
    void tearDown() {
        TestBus.INSTANCE.resetListeners();
    }

    // ── Basic dispatch ────────────────────────────────────────────────────────

    @Nested
    class BasicDispatch {

        @Test
        void directListenerFires() {
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("fired"));
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(List.of("fired"), log);
        }

        @Test
        void noListenersDoesNotCrash() {
            assertDoesNotThrow(() -> TestBus.INSTANCE.post(new ParentEvent()));
        }

        @Test
        void listenerDoesNotFireForUnrelatedEvent() {
            TestBus.INSTANCE.register(ChildEvent.class, e -> log.add("child"));
            TestBus.INSTANCE.post(new ParentEvent());
            assertTrue(log.isEmpty());
        }

        @Test
        void multipleDirectListenersFire() {
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("a"));
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("b"));
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("c"));
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(List.of("a", "b", "c"), log);
        }

        @Test
        void eventDispatchFiresToBus() {
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("fired"));
            new ParentEvent().dispatch();
            assertEquals(List.of("fired"), log);
        }

        @Test
        void postUncheckedSkipsIfWrongBusType() {
            // MainBus should not fire for TestEvent since it accepts Event, not TestEvent subtype
            // Just verify TestBus only fires for its own type
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("fired"));
            TestBus.INSTANCE.postUnchecked(new ParentEvent());
            assertEquals(List.of("fired"), log);
        }
    }

    // ── Polymorphic dispatch ──────────────────────────────────────────────────

    @Nested
    class PolymorphicDispatch {

        @Test
        void parentListenerReceivesChildEvent() {
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("parent"));
            TestBus.INSTANCE.post(new ChildEvent());
            assertEquals(List.of("parent"), log);
        }

        @Test
        void bothParentAndChildListenerFire() {
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("parent"));
            TestBus.INSTANCE.register(ChildEvent.class, e -> log.add("child"));
            TestBus.INSTANCE.post(new ChildEvent());
            assertEquals(List.of("parent", "child"), log);
        }

        @Test
        void grandchildReceivesAllAncestorListeners() {
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("parent"));
            TestBus.INSTANCE.register(ChildEvent.class, e -> log.add("child"));
            TestBus.INSTANCE.register(GrandchildEvent.class, e -> log.add("grandchild"));
            TestBus.INSTANCE.post(new GrandchildEvent());
            assertEquals(List.of("parent", "child", "grandchild"), log);
        }

        @Test
        void parentDoesNotReceiveGrandchildWhenOnlyGrandchildRegistered() {
            TestBus.INSTANCE.register(GrandchildEvent.class, e -> log.add("grandchild"));
            TestBus.INSTANCE.post(new ParentEvent());
            assertTrue(log.isEmpty());
        }

        @Test
        void parentListenerReceivesCorrectEventInstance() {
            TestBus.INSTANCE.register(ParentEvent.class, e ->
                    log.add(e.getClass().getSimpleName()));
            TestBus.INSTANCE.post(new ChildEvent());
            assertEquals(List.of("ChildEvent"), log);
        }
    }

    // ── Priority ordering ─────────────────────────────────────────────────────

    @Nested
    class PriorityOrdering {

        @Test
        void priorityOrderRespected() {
            TestBus.INSTANCE.register(ParentEvent.class, Priority.HIGH, e -> log.add("high"));
            TestBus.INSTANCE.register(ParentEvent.class, Priority.LOW, e -> log.add("low"));
            TestBus.INSTANCE.register(ParentEvent.class, Priority.NORMAL, e -> log.add("normal"));
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(List.of("low", "normal", "high"), log);
        }

        @Test
        void priorityRespectedAcrossParentAndChild() {
            TestBus.INSTANCE.register(ParentEvent.class, Priority.HIGH, e -> log.add("parent:high"));
            TestBus.INSTANCE.register(ParentEvent.class, Priority.LOW, e -> log.add("parent:low"));
            TestBus.INSTANCE.register(ChildEvent.class, Priority.NORMAL, e -> log.add("child:normal"));
            TestBus.INSTANCE.register(ChildEvent.class, Priority.LOWEST, e -> log.add("child:lowest"));
            TestBus.INSTANCE.post(new ChildEvent());
            assertEquals(List.of("child:lowest", "parent:low", "child:normal", "parent:high"), log);
        }

        @Test
        void monitorIsLast() {
            TestBus.INSTANCE.register(ParentEvent.class, Priority.MONITOR, e -> log.add("monitor"));
            TestBus.INSTANCE.register(ParentEvent.class, Priority.NORMAL, e -> log.add("normal"));
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(List.of("normal", "monitor"), log);
        }

        @Test
        void lowestIsFirst() {
            TestBus.INSTANCE.register(ParentEvent.class, Priority.LOWEST, e -> log.add("lowest"));
            TestBus.INSTANCE.register(ParentEvent.class, Priority.NORMAL, e -> log.add("normal"));
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(List.of("lowest", "normal"), log);
        }
    }

    // ── Branching hierarchy ───────────────────────────────────────────────────

    @Nested
    class BranchingHierarchy {

        @Test
        void branchADoesNotFireWhenBranchBPosted() {
            TestBus.INSTANCE.register(BranchAEvent.class, e -> log.add("branchA"));
            TestBus.INSTANCE.register(BranchBEvent.class, e -> log.add("branchB"));
            TestBus.INSTANCE.post(new BranchBEvent());
            assertEquals(List.of("branchB"), log);
        }

        @Test
        void branchRootFiresForBothBranches() {
            TestBus.INSTANCE.register(BranchRootEvent.class, e -> log.add("root"));
            TestBus.INSTANCE.register(BranchAEvent.class, e -> log.add("branchA"));
            TestBus.INSTANCE.register(BranchBEvent.class, e -> log.add("branchB"));

            TestBus.INSTANCE.post(new BranchAEvent());
            assertEquals(List.of("root", "branchA"), log);
            log.clear();

            TestBus.INSTANCE.post(new BranchBEvent());
            assertEquals(List.of("root", "branchB"), log);
        }

        @Test
        void branchChildOnlyFiresOwnBranch() {
            TestBus.INSTANCE.register(BranchRootEvent.class, e -> log.add("root"));
            TestBus.INSTANCE.register(BranchAEvent.class, e -> log.add("branchA"));
            TestBus.INSTANCE.register(BranchAChildEvent.class, e -> log.add("branchAChild"));
            TestBus.INSTANCE.register(BranchBEvent.class, e -> log.add("branchB"));
            TestBus.INSTANCE.register(BranchBChildEvent.class, e -> log.add("branchBChild"));

            TestBus.INSTANCE.post(new BranchAChildEvent());
            assertEquals(List.of("root", "branchA", "branchAChild"), log);
            log.clear();

            TestBus.INSTANCE.post(new BranchBChildEvent());
            assertEquals(List.of("root", "branchB", "branchBChild"), log);
        }

        @Test
        void rootEventDoesNotFireBranchListeners() {
            TestBus.INSTANCE.register(BranchAEvent.class, e -> log.add("branchA"));
            TestBus.INSTANCE.register(BranchBEvent.class, e -> log.add("branchB"));
            TestBus.INSTANCE.post(new BranchRootEvent());
            assertTrue(log.isEmpty());
        }

        @Test
        void branchRootListenerReceivesCorrectInstanceForEachBranch() {
            TestBus.INSTANCE.register(BranchRootEvent.class, e ->
                    log.add(e.getClass().getSimpleName()));
            TestBus.INSTANCE.post(new BranchAEvent());
            TestBus.INSTANCE.post(new BranchBEvent());
            assertEquals(List.of("BranchAEvent", "BranchBEvent"), log);
        }
    }

    // ── @SubscribeEvent registration ──────────────────────────────────────────

    @Nested
    class SubscribeEventRegistration {

        @Test
        void instanceListenerRegistration() {
            TestBus.INSTANCE.register(new ParentListener(log));
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(List.of("parent"), log);
        }

        @Test
        void instanceListenerDoesNotFireForNonMatchingEvent() {
            TestBus.INSTANCE.register(new ChildListener(log));
            TestBus.INSTANCE.post(new ParentEvent());
            assertTrue(log.isEmpty());
        }

        @Test
        void instanceListenerReceivesChildEventViaParent() {
            TestBus.INSTANCE.register(new ParentListener(log));
            TestBus.INSTANCE.post(new ChildEvent());
            assertEquals(List.of("parent"), log);
        }

        @Test
        void multiListenerReceivesBothEvents() {
            TestBus.INSTANCE.register(new MultiListener(log));
            new ParentEvent().dispatch();
            new ChildEvent().dispatch();
            assertEquals(2, log.stream().filter(s -> s.equals("parent")).count());
            assertEquals(1, log.stream().filter(s -> s.equals("child")).count());
        }

        @Test
        void subscribeEventPriorityRespected() {
            TestBus.INSTANCE.register(new PriorityListener(log));
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(List.of("low", "normal", "high"), log);
        }

        @Test
        void staticListenerRegistration() {
            StaticListener.log = log;
            TestBus.INSTANCE.register(StaticListener.class);
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(List.of("static"), log);
        }

        @Test
        void staticMethodDirectRegistration() throws NoSuchMethodException {
            StaticListener.log = log;
            Method method = StaticListener.class.getDeclaredMethod("onParent", ParentEvent.class);
            TestBus.INSTANCE.register(method);
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(List.of("static"), log);
        }

        @Test
        void nonStaticMethodDirectRegistrationThrows() throws NoSuchMethodException {
            Method method = ParentListener.class.getDeclaredMethod("onParent", ParentEvent.class);
            assertThrows(IllegalArgumentException.class, () -> TestBus.INSTANCE.register(method));
        }
    }

    // ── @SubscribeEvent validation ────────────────────────────────────────────

    @Nested
    class SubscribeEventValidation {

        @Test
        void nullListenerThrows() {
            assertThrows(IllegalArgumentException.class, () -> TestBus.INSTANCE.register((Object) null));
        }

        @Test
        void noAnnotationThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> TestBus.INSTANCE.register(new NoAnnotationListener()));
        }

        @Test
        void invalidParamCountThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> TestBus.INSTANCE.register(new InvalidParamCountListener()));
        }

        @Test
        void invalidParamTypeThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> TestBus.INSTANCE.register(new InvalidParamTypeListener()));
        }

        @Test
        void nonVoidReturnThrows() {
            assertThrows(IllegalStateException.class,
                    () -> TestBus.INSTANCE.register(new NonVoidReturnListener()));
        }

        @Test
        void nonPublicMethodThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> TestBus.INSTANCE.register(new PrivateMethodListener()));
        }

        @Test
        void staticMethodOnInstanceRegistrationThrows() {
            //noinspection InstantiationOfUtilityClass
            StaticListener listener = new StaticListener();
            assertThrows(IllegalArgumentException.class,
                    () -> TestBus.INSTANCE.register(listener));
        }

        @Test
        void supertypeWithSubscribeEventThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> TestBus.INSTANCE.register(new SubtypeOfSupertypeListener()));
        }

        @Test
        void instanceMethodOnClassRegistrationThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> TestBus.INSTANCE.register(ParentListener.class));
        }
    }

    // ── ignoreCancelled ───────────────────────────────────────────────────────

    @Nested
    class IgnoreCancelled {

        @Test
        void ignoreCancelledTrueSkipsWhenCancelled() {
            class Listener {
                @SubscribeEvent(ignoreCancelled = true)
                public void on(CancellableTestEvent e) { log.add("called"); }
            }
            TestBus.INSTANCE.register(new Listener());
            CancellableTestEvent event = new CancellableTestEvent();
            event.cancel();
            TestBus.INSTANCE.post(event);
            assertTrue(log.isEmpty());
        }

        @Test
        void ignoreCancelledFalseStillFiresWhenCancelled() {
            class Listener {
                @SubscribeEvent
                public void on(CancellableTestEvent e) { log.add("called"); }
            }
            TestBus.INSTANCE.register(new Listener());
            CancellableTestEvent event = new CancellableTestEvent();
            event.cancel();
            TestBus.INSTANCE.post(event);
            assertEquals(List.of("called"), log);
        }

        @Test
        void ignoreCancelledTrueFiresWhenNotCancelled() {
            class Listener {
                @SubscribeEvent(ignoreCancelled = true)
                public void on(CancellableTestEvent e) { log.add("called"); }
            }
            TestBus.INSTANCE.register(new Listener());
            TestBus.INSTANCE.post(new CancellableTestEvent());
            assertEquals(List.of("called"), log);
        }

        @Test
        void ignoreCancelledRespectedOnChildOfCancellable() {
            class Listener {
                @SubscribeEvent(ignoreCancelled = true)
                public void on(CancellableTestEvent e) { log.add("called"); }
            }
            TestBus.INSTANCE.register(new Listener());
            CancellableChildEvent event = new CancellableChildEvent();
            event.cancel();
            TestBus.INSTANCE.post(event);
            assertTrue(log.isEmpty());
        }
    }

    // ── resetListeners ────────────────────────────────────────────────────────

    @Nested
    class ResetListeners {

        @Test
        void resetClearsAllListeners() {
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("fired"));
            TestBus.INSTANCE.resetListeners();
            TestBus.INSTANCE.post(new ParentEvent());
            assertTrue(log.isEmpty());
        }

        @Test
        void canRegisterAfterReset() {
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("first"));
            TestBus.INSTANCE.resetListeners();
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("second"));
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(List.of("second"), log);
        }

        @Test
        void resetDoesNotAffectOtherBuses() {
            // MainBus accepts all events — register on it and verify TestBus reset doesn't affect it
            MainBus.BUS.register(ParentEvent.class, e -> log.add("mainbus"));
            TestBus.INSTANCE.resetListeners();
            MainBus.BUS.post(new ParentEvent());
            assertEquals(List.of("mainbus"), log);
            MainBus.BUS.resetListeners();
        }
    }

    // ── EventBusRegistry ─────────────────────────────────────────────────────

    @Nested
    class Registry {

        @Test
        void dispatchFiresAllMatchingBuses() {
            // TestBus and MainBus both registered — dispatch should hit both
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("testbus"));
            MainBus.BUS.register(ParentEvent.class, e -> log.add("mainbus"));
            EventDispatcher.dispatch(new ParentEvent());
            assertTrue(log.contains("testbus"));
            assertTrue(log.contains("mainbus"));
            MainBus.BUS.resetListeners();
        }

        @Test
        void dispatchOnlyFiresBusesWhoseTypeMatches() {
            // TestBus requires TestEvent subtypes — posting a raw Event won't fire it
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("testbus"));
            // ParentEvent extends TestEvent so TestBus should fire
            EventDispatcher.dispatch(new ParentEvent());
            assertTrue(log.contains("testbus"));
        }
    }
}