package com.github.rcubedev.example.event;

import com.github.rcubedev.example.event.api.*;
import com.github.rcubedev.example.event.api.buses.MainBus;
import com.github.rcubedev.example.event.api.exceptions.EventStackOverflowException;
import com.github.rcubedev.example.event.api.spi.RecursionBypass;
import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.impl.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class EventBusTests {

    // ── Test bus ──────────────────────────────────────────────────────────────

    static final class TestBus {
        static final EventBus<TestEvent> INSTANCE = (EventBus<TestEvent>) EventBusBuilder.create(TestEvent.class);
    }

    // ── Linear hierarchy ──────────────────────────────────────────────────────
    //  TestEvent → ParentEvent → ChildEvent → GrandchildEvent

    static class ParentEvent extends TestEvent {
        public ParentEvent() {
            super(EventBusRegistry.getInstance());
        }
    }
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
        @SuppressWarnings("unused")
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

        // todo this test does not seem right
        @Test
        void postUncheckedSkipsIfWrongBusType() {
            // MainBus should not fire for TestEvent since it accepts Event, not TestEvent subtype
            // Just verify TestBus only fires for its own type
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("fired"));
            TestBus.INSTANCE.post(new ParentEvent());
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
            TestBus.INSTANCE.register(TestEvent.class, e -> {});
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
            assertThrows(IllegalArgumentException.class,
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
            ((EventBus<Event>) MainBus.BUS).resetListeners();
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
            new ParentEvent().dispatch();
            assertTrue(log.contains("mainbus"));
            ((EventBus<Event>) MainBus.BUS).resetListeners();
        }

        @Test
        void dispatchOnlyFiresBusesWhoseTypeMatches() {
            // TestBus requires TestEvent subtype; posting a raw Event won't fire it
            TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("testbus"));
            // ParentEvent extends TestEvent so TestBus should fire
            new ParentEvent().dispatch();
            assertTrue(log.contains("testbus"));
        }
    }

    // ── Weak Referencing ──────────────────────────────────────────────────────

    @Nested
    class WeakReferencing {

        @Test
        void methodWeakListenerIsCollectedAndUnregistered() throws InterruptedException {
            // Use a helper to ensure 'listener' isn't trapped in this stack frame
            WeakReference<Object> ref = registerWeakMethod();

            // should fire initially
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(1, log.size());
            log.clear();

            // force GC, typically requires multiple hints in a loop for reliability
            for (int i = 0; i < 10; i++) {
                System.gc();
                if (ref.get() == null) break;
                Thread.sleep(10);
            }

            assertNull(ref.get(), "Listener instance should have been GC'd");

            // post again: the WeakEventProcessor should detect GC and self-destruct
            TestBus.INSTANCE.post(new ParentEvent());
            assertTrue(log.isEmpty(), "Listener should not fire after GC");
        }

        @Test
        void classWeakListenerIsCollectedAndUnregistered() throws InterruptedException {
            // Use a helper to ensure 'listener' isn't trapped in this stack frame
            WeakReference<Object> ref = registerWeakClass();

            // should fire initially
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(1, log.size());
            log.clear();

            // force GC, typically requires multiple hints in a loop for reliability
            for (int i = 0; i < 10; i++) {
                System.gc();
                if (ref.get() == null) break;
                Thread.sleep(10);
            }

            assertNull(ref.get(), "Listener instance should have been GC'd");

            // post again: the WeakEventProcessor should detect GC and self-destruct
            TestBus.INSTANCE.post(new ParentEvent());
            assertTrue(log.isEmpty(), "Listener should not fire after GC");
        }

        private WeakReference<Object> registerWeakMethod() {
            class WeakListener {
                @SubscribeEvent @Weak public void on(ParentEvent e) { log.add("weak"); }
            }
            WeakListener listener = new WeakListener();
            TestBus.INSTANCE.register(listener);
            return new WeakReference<>(listener);
        }

        private WeakReference<Object> registerWeakClass() {
            @Weak
            class WeakListener {
                @SubscribeEvent public void on(ParentEvent e) { log.add("weak"); }
            }
            WeakListener listener = new WeakListener();
            TestBus.INSTANCE.register(listener);
            return new WeakReference<>(listener);
        }

        @Test
        void staticMethodWeakThrows() {
            @Weak class Invalid {
                @SubscribeEvent public static void on(ParentEvent e) {}
            }
            assertThrows(IllegalArgumentException.class, () -> TestBus.INSTANCE.register(Invalid.class));
        }
    }

    // ── Subscription Handling ─────────────────────────────────────────────────

    @Nested
    class SubscriptionHandling {

        @Test
        void unsubscribeRemovesListener() {
            Subscription sub = TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("fired"));
            TestBus.INSTANCE.post(new ParentEvent());
            assertEquals(1, log.size());

            sub.unsubscribe();
            log.clear();

            TestBus.INSTANCE.post(new ParentEvent());
            assertTrue(log.isEmpty(), "Listener remained after unsubscribe");
        }

        @Test
        void tryWithResourcesUnsubscribes() {
            try (Subscription ignored = TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("fired"))) {
                TestBus.INSTANCE.post(new ParentEvent());
            }
            log.clear();
            TestBus.INSTANCE.post(new ParentEvent());
            assertTrue(log.isEmpty(), "Subscription did not close automatically");
        }

        @Test
        void masterSubscriptionUnsubscribesAllMethods() {
            MultiListener listener = new MultiListener(log);
            Subscription sub = TestBus.INSTANCE.register(listener);

            TestBus.INSTANCE.post(new ChildEvent());
            assertEquals(2, log.size()); // parent and child

            sub.unsubscribe();
            log.clear();

            TestBus.INSTANCE.post(new ChildEvent());
            assertTrue(log.isEmpty(), "Master subscription failed to unregister all methods");
        }

        @Test
        void unsubscribeIsIdempotent() {
            Subscription sub = TestBus.INSTANCE.register(ParentEvent.class, e -> log.add("fired"));
            sub.unsubscribe();
            assertDoesNotThrow(sub::unsubscribe);
            assertDoesNotThrow(sub::close);
        }
    }

    // ── Recursion Guard ───────────────────────────────────────────────────────

    @Nested
    class RecursionGuard {

        static class RecursiveEvent extends TestEvent {}

        @Test
        void circularDispatchTripsGuard() {
            TestBus.INSTANCE.register(RecursiveEvent.class, TestBus.INSTANCE::post);

            assertThrows(EventStackOverflowException.class, () -> TestBus.INSTANCE.post(new RecursiveEvent()),
                    "Infinite recursion should be caught by the guard");
        }

        @Test
        void openBypassAllowsDeepRecursion() {
            int depthLimit = 150; // Higher than default 128
            AtomicInteger count = new AtomicInteger();

            TestBus.INSTANCE.register(RecursiveEvent.class, e -> {
                if (count.incrementAndGet() < depthLimit) {
                    TestBus.INSTANCE.post(e);
                }
            });

            try (RecursionBypass ignored = TestBus.INSTANCE.openBypass()) {
                assertDoesNotThrow(() -> TestBus.INSTANCE.post(new RecursiveEvent()));
            }
            assertEquals(depthLimit, count.get());
        }

        @Test
        void openBypassToExtendsBudgetSpecifically() {
            AtomicInteger count = new AtomicInteger();
            // We want to exceed the default limit (128) but stay within
            // the new limit (128 + 72 = 200).
            int targetDepth = 200;

            TestBus.INSTANCE.register(RecursiveEvent.class, e -> {
                // try to go even deeper. atomic is 0 indexed so use incr&get to make sure it fires targetDepth times
                // System.out.println("Current recursion count: " + (count.get() + 1));
                if (count.incrementAndGet() < targetDepth) {
                    TestBus.INSTANCE.post(e);
                }
            });

            // Verify it fails without the bypass first
            assertThrows(EventStackOverflowException.class, () -> TestBus.INSTANCE.post(new RecursiveEvent()),
                    "Should fail at default limit");

            // System.out.println("Resetting recursion. Count: " + count.get());
            count.set(0);

            // Verify it succeeds with a specific extra budget
            try (RecursionBypass ignored = TestBus.INSTANCE.openBypassTo(72)) {
                // If default is 128, new limit is 200. 200 should pass.
                assertDoesNotThrow(() -> {
                    // We only trigger the first post; the listener handles the recursion
                    TestBus.INSTANCE.post(new RecursiveEvent());
                });
            }

            // Verify the limit is still enforced if we go TOO deep even with the budget
            // System.out.println("Resetting recursion 2. Count: " + count.get());
            count.set(0);
            try (RecursionBypass ignored = TestBus.INSTANCE.openBypassTo(10)) {
                // Default 128 + 10 = 138. 150 should fail.
                assertThrows(EventStackOverflowException.class, () -> TestBus.INSTANCE.post(new RecursiveEvent()));
            }
        }

        @Test
        void bypassIsThreadLocalAndScoped() {
            // Verify that after bypass closes, the guard is active again
            try (RecursionBypass ignored = TestBus.INSTANCE.openBypass()) {
                // Scope active
            }

            TestBus.INSTANCE.register(RecursiveEvent.class, TestBus.INSTANCE::post);
            assertThrows(EventStackOverflowException.class, () -> TestBus.INSTANCE.post(new RecursiveEvent()),
                    "Guard should be re-enabled after bypass close");
        }
    }
}