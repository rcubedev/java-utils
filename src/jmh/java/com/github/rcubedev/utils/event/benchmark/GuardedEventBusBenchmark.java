package com.github.rcubedev.utils.event.benchmark;

import com.github.rcubedev.utils.event.api.Cancellable;
import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.EventBusBuilder;
import com.github.rcubedev.utils.event.api.Identity;
import com.github.rcubedev.utils.event.api.annotation.CompiledEventHandlers;
import com.github.rcubedev.utils.event.api.annotation.SubscribeEvent;
import com.github.rcubedev.utils.event.api.spi.IEventBus;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(value = 3)
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5)
@BenchmarkMode(value = {Mode.AverageTime})
@OutputTimeUnit(value = TimeUnit.NANOSECONDS)
public class GuardedEventBusBenchmark {

    private IEventBus<TestEvent> compiledBus;
    private IEventBus<TestEvent> nonCompiledBusLmf;
    private IEventBus<TestEvent> nonCompiledBusMH;
    private IEventBus<TestEvent> callbackBus;

    private TestEvent1 compiledEvent1;
    private TestEvent2 compiledEvent2;
    private TestEvent1 nonCompiledEvent1Lmf;
    private TestEvent2 nonCompiledEvent2Lmf;
    private TestEvent1 nonCompiledEvent1MH;
    private TestEvent2 nonCompiledEvent2MH;
    private TestEvent1 callbackEvent1;
    private TestEvent2 callbackEvent2;

    @Setup(Level.Trial)
    public void setup() {
        this.compiledBus = EventBusBuilder.builder(TestEvent.class).recursionGuard(true).global(false).build();
        this.compiledBus.register(new CompiledTestListener(), Identity.of(MethodHandles.lookup()));
        this.compiledEvent1 = new TestEvent1();
        this.compiledEvent2 = new TestEvent2();

        this.nonCompiledBusLmf = EventBusBuilder.builder(TestEvent.class).recursionGuard(true).global(false).build();
        this.nonCompiledBusLmf.register(new TestListenerLmf(), Identity.of(MethodHandles.lookup()));
        this.nonCompiledEvent1Lmf = new TestEvent1();
        this.nonCompiledEvent2Lmf = new TestEvent2();

        this.nonCompiledBusMH = EventBusBuilder.builder(TestEvent.class).recursionGuard(true).global(false).build();
        this.nonCompiledBusMH.register(new TestListenerMH(), Identity.ofPublic()); // pub ID forces fallback path
        this.nonCompiledEvent1MH = new TestEvent1();
        this.nonCompiledEvent2MH = new TestEvent2();

        this.callbackBus = EventBusBuilder.builder(TestEvent.class).recursionGuard(true).global(false).build();
        this.callbackBus.register(TestEvent1.class, e -> e.count++, Identity.ofPublic());
        this.callbackBus.register(TestEvent2.class, e -> {
            if (e instanceof Cancellable c && c.isCancelled()) return;
            e.count++;
        }, Identity.ofPublic());
        this.callbackEvent1 = new TestEvent1();
        this.callbackEvent2 = new TestEvent2();
    }

    @Benchmark
    public void testCompiledEventDispatch(Blackhole bh) {
        compiledBus.post(compiledEvent1);
        bh.consume(compiledEvent1.count);
    }

    @Benchmark
    public void testCompiledEventDispatchCancellable(Blackhole bh) {
        compiledBus.post(compiledEvent2);
        bh.consume(compiledEvent2.count);
    }

    @Benchmark
    public void testNonCompiledLmfEventDispatch(Blackhole bh) {
        nonCompiledBusLmf.post(nonCompiledEvent1Lmf);
        bh.consume(nonCompiledEvent1Lmf.count);
    }

    @Benchmark
    public void testNonCompiledLmfEventDispatchCancellable(Blackhole bh) {
        nonCompiledBusLmf.post(nonCompiledEvent2Lmf);
        bh.consume(nonCompiledEvent2Lmf.count);
    }

    @Benchmark
    public void testNonCompiledMHEventDispatch(Blackhole bh) {
        nonCompiledBusMH.post(nonCompiledEvent1MH);
        bh.consume(nonCompiledEvent1MH.count);
    }

    @Benchmark
    public void testNonCompiledMHEventDispatchCancellable(Blackhole bh) {
        nonCompiledBusMH.post(nonCompiledEvent2MH);
        bh.consume(nonCompiledEvent2MH.count);
    }

    @Benchmark
    public void testCallbackEventDispatch(Blackhole bh) {
        callbackBus.post(callbackEvent1);
        bh.consume(callbackEvent1.count);
    }

    @Benchmark
    public void testCallbackEventDispatchCancellable(Blackhole bh) {
        callbackBus.post(callbackEvent2);
        bh.consume(callbackEvent2.count);
    }

    public static abstract class TestEvent extends Event {
        public int count = 0;
    }

    public static class TestEvent1 extends TestEvent {}

    public static class TestEvent2 extends TestEvent {}


    @CompiledEventHandlers
    public static class CompiledTestListener {

        @SubscribeEvent
        public void onTestEvent(TestEvent1 event) {
            event.count++;
        }

        @SubscribeEvent(ignoreCancelled = true) // just to add the cancellable wrapper
        public void onEvent(TestEvent2 event) {
            event.count++;
        }
    }

    public static class TestListenerLmf {

        @SubscribeEvent//(ignoreCancelled = true)
        public void onTestEvent(TestEvent1 event) {
            event.count++;
        }

        @SubscribeEvent(ignoreCancelled = true) // just to add the cancellable wrapper
        public void onEvent(TestEvent2 event) {
            event.count++;
        }
    }

    public static class TestListenerMH {

        @SubscribeEvent//(ignoreCancelled = true)
        public void onTestEvent(TestEvent1 event) {
            event.count++;
        }

        @SubscribeEvent(ignoreCancelled = true) // just to add the cancellable wrapper
        public void onEvent(TestEvent2 event) {
            event.count++;
        }
    }
}
