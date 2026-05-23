package com.github.rcubedev.example;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.DispatchTable;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.RegisteredParentResolver;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.builder.FamilyBuilder;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.builder.Flattener;
import com.github.rcubedev.example.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class DispatchTableBenchmarkTest {

    @Test
    public void runJmhBenchmarks() throws Exception {
        System.setProperty("jmh.separateClasspathJAR", "true");

        Options opt = new OptionsBuilder()
                .include(this.getClass().getSimpleName())
                .shouldDoGC(true)
                .jvmArgsAppend(/*"-XX:+PrintCompilation", "-XX:+UnlockDiagnosticVMOptions",*/ "-XX:-PrintWarnings")
                .build();
        new Runner(opt).run();
    }

    private static final int NUM_FAMILIES       = 20;
    private static final int NUM_MID_PER_ROOT   = 10;
    private static final int NUM_LEAF_PER_MID   = 3;
    private static final int NUM_GRAND_PER_LEAF = 2;

    // This is your actual production Immutable table instance compiled right from your source code
    private DispatchTable<Event>            prodBitSetTable;

    private FlatStampDispatchTable<Event>   flatStampTable;
    private LinearDispatchTable<Event>      linearTable;
    private NaiveDispatchTable<Event>       naiveTable;
    private ClassValueDispatchTable<Event>  classValueTable;

    private Event matchingEvent;
    private Event nonMatchingEvent;

    public final int[] sideEffectCounter = {0};

    @Setup(Level.Trial)
    public void setup() throws Exception {
        ByteBuddy bb = new ByteBuddy();
        ClassLoader cl = getClass().getClassLoader();
        ClassLoadingStrategy<ClassLoader> strat = ClassLoadingStrategy.Default.INJECTION;

        EventProcessor<Event> proc = event -> sideEffectCounter[0]++;

        // Containers for metadata matching your production bus registry expectations
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap = new LinkedHashMap<>();

        Class<? extends Event> matchTarget = null;

        for (int f = 0; f < NUM_FAMILIES; f++) {
            Class<? extends Event> rootClass = gen(bb, cl, strat, "com.example.gen.Root_F" + f, Event.class);
            registerType(rootClass, handlersMap, proc);

            for (int m = 0; m < NUM_MID_PER_ROOT; m++) {
                Class<? extends Event> midClass = gen(bb, cl, strat, "com.example.gen.Mid_F" + f + "_M" + m, rootClass);
                registerType(midClass, handlersMap, proc);

                for (int l = 0; l < NUM_LEAF_PER_MID; l++) {
                    Class<? extends Event> leafClass = gen(bb, cl, strat, "com.example.gen.Leaf_F" + f + "_M" + m + "_L" + l, midClass);
                    registerType(leafClass, handlersMap, proc);

                    for (int g = 0; g < NUM_GRAND_PER_LEAF; g++) {
                        Class<? extends Event> grandClass = gen(bb, cl, strat, "com.example.gen.Grand_F" + f + "_M" + m + "_L" + l + "_G" + g, leafClass);
                        registerType(grandClass, handlersMap, proc);

                        if (f == 0 && m == 0 && l == (NUM_LEAF_PER_MID - 1) && g == (NUM_GRAND_PER_LEAF - 1)) {
                            matchTarget = grandClass;
                        }
                    }
                }
            }
        }

        Class<? extends Event> unrelated = gen(bb, cl, strat, "com.example.gen.UnrelatedEvent", Event.class);
        matchingEvent    = matchTarget.getDeclaredConstructor().newInstance();
        nonMatchingEvent = unrelated.getDeclaredConstructor().newInstance();

        // Implement the production abstractions to feed into the real builders cleanly
        RegistrySnapshot<Event> snapshot = new RegistrySnapshot<>(handlersMap);
        RegisteredParentResolver<Event> resolver = new RegisteredParentResolver<>(Event.class, snapshot);

        // RUN YOUR GENUINE PRODUCTION BUILDER & FLATTENER ARCHITECTURE
        FamilyBuilder<Event> familyBuilder = new FamilyBuilder<>(snapshot, resolver);
        List<List<Class<? extends Event>>> realFamilies = familyBuilder.buildFamilies();

        Flattener<Event> flattener = new Flattener<>(snapshot, resolver);
        prodBitSetTable = flattener.flatten(realFamilies);

        // Extract production internal data segments via reflection to back alternative test variants
        var flatFields = DispatchTableInspector.extractFields(prodBitSetTable);

//        System.out.println("flatfield flat count: " + Arrays.stream(flatFields.flat).count() + "handlersMap size: " + handlersMap.size() + " calc stamp size: " + (Arrays.stream(flatFields.selfBitIndices).max().orElse(-1) + 1));
        flatStampTable = new FlatStampDispatchTable<>(
                flatFields.flat, flatFields.flatTypes,
                flatFields.parentBitIndices, flatFields.selfBitIndices,
                handlersMap.size());

        linearTable = new LinearDispatchTable<>(
                flatFields.flat, flatFields.flatTypes, flatFields.segmentOffsets, flatFields.segmentLengths);

        naiveTable = new NaiveDispatchTable<>(
                flatFields.flat, flatFields.flatTypes);

        Map<Class<? extends Event>, List<EventProcessor<? extends Event>>> lookupMap = new LinkedHashMap<>();

        // Prime the map with keys for every single registered class type
        for (Class<? extends Event> type : handlersMap.keySet()) {
            lookupMap.put(type, new ArrayList<>());
        }
        // Also register an explicit slot for the unrelated event so the constructor primes it
        lookupMap.put(unrelated, new ArrayList<>());

        // Walk the flattened array structure to map processors down the type hierarchy inheritance chains
        for (int i = 0; i < flatFields.flatTypes.length; i++) {
            Class<? extends Event> handlerType = flatFields.flatTypes[i];
            EventProcessor<Event> processor = flatFields.flat[i];

            for (Class<? extends Event> eventType : lookupMap.keySet()) {
                if (handlerType.isAssignableFrom(eventType)) {
                    lookupMap.get(eventType).add(processor);
                }
            }
        }

        classValueTable = new ClassValueDispatchTable<>(
                flatFields.flat, flatFields.flatTypes);
    }

    @SuppressWarnings("unchecked")
    private void registerType(Class<? extends Event> type,
                              Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> handlersMap,
                              EventProcessor<Event> proc) {
        Map<Priority, EventSinkSnapshot<? extends Event>> pMap = new HashMap<>();
        pMap.put(Priority.NORMAL, new EventSinkSnapshot<>((Class<Event>) type, Priority.NORMAL, proc));
        handlersMap.put(type, pMap);
    }

    private static <T extends Event> Class<? extends T> gen(ByteBuddy bb, ClassLoader cl,
                                              ClassLoadingStrategy<ClassLoader> strat,
                                              String name, Class<T> parent) {
        return bb.subclass(parent).name(name).make().load(cl, strat).getLoaded();
    }

    // ==========================================
    // BENCHMARKS
    // ==========================================

    @Benchmark
    public void bitset_Matching(Blackhole bh) {
        prodBitSetTable.dispatch(matchingEvent);
        bh.consume(sideEffectCounter[0]);
    }

    @Benchmark
    public void flatStamp_Matching(Blackhole bh) {
        flatStampTable.dispatch(matchingEvent);
        bh.consume(sideEffectCounter[0]);
    }

    @Benchmark
    public void flatStamp_NonMatching(Blackhole bh) {
        flatStampTable.dispatch(nonMatchingEvent);
        bh.consume(sideEffectCounter[0]);
    }

    @Benchmark
    public void bitset_NonMatching(Blackhole bh) {
        prodBitSetTable.dispatch(nonMatchingEvent);
        bh.consume(sideEffectCounter[0]);
    }

    @Benchmark
    public void linear_Matching(Blackhole bh) {
        linearTable.dispatch(matchingEvent);
        bh.consume(sideEffectCounter[0]);
    }

    @Benchmark
    public void linear_NonMatching(Blackhole bh) {
        linearTable.dispatch(nonMatchingEvent);
        bh.consume(sideEffectCounter[0]);
    }

    @Benchmark
    public void naiveExhaustive_Matching(Blackhole bh) {
        naiveTable.dispatch(matchingEvent);
        bh.consume(sideEffectCounter[0]);
    }

    @Benchmark
    public void naiveExhaustive_NonMatching(Blackhole bh) {
        naiveTable.dispatch(nonMatchingEvent);
        bh.consume(sideEffectCounter[0]);
    }

    // ==========================================
    // FIELD EXTRACTION SHIM
    // ==========================================

    private static class DispatchTableInspector {
        static class Unpacked {
            EventProcessor<Event>[] flat;
            Class<Event>[] flatTypes;
            int[] segmentOffsets;
            int[] segmentLengths;
            int[] parentBitIndices;
            int[] selfBitIndices;
        }

        @SuppressWarnings("unchecked")
        static Unpacked extractFields(DispatchTable<Event> table) throws Exception {
            Unpacked u = new Unpacked();
            Class<?> cls = table.getClass();

            Field fFlat = cls.getDeclaredField("flat"); fFlat.setAccessible(true);
            u.flat = (EventProcessor<Event>[]) fFlat.get(table);

            Field fFlatTypes = cls.getDeclaredField("flatTypes"); fFlatTypes.setAccessible(true);
            u.flatTypes = (Class<Event>[]) fFlatTypes.get(table);

            Field fSegOffsets = cls.getDeclaredField("segmentOffsets"); fSegOffsets.setAccessible(true);
            u.segmentOffsets = (int[]) fSegOffsets.get(table);

            Field fSegLengths = cls.getDeclaredField("segmentLengths"); fSegLengths.setAccessible(true);
            u.segmentLengths = (int[]) fSegLengths.get(table);

            Field fParentBits = cls.getDeclaredField("parentBitIndices"); fParentBits.setAccessible(true);
            u.parentBitIndices = (int[]) fParentBits.get(table);

            Field fSelfBits = cls.getDeclaredField("selfBitIndices"); fSelfBits.setAccessible(true);
            u.selfBitIndices = (int[]) fSelfBits.get(table);

            return u;
        }
    }

    // ==========================================
    // ALTERNATIVE BENCHMARK RUNTIMES
    // ==========================================

    public static final class NaiveDispatchTable<E extends Event> {
        private final EventProcessor<? extends E>[] flat;
        private final Class<? extends E>[]          flatTypes;

        public NaiveDispatchTable(EventProcessor<? extends E>[] flat, Class<? extends E>[] flatTypes) {
            this.flat      = flat;
            this.flatTypes = flatTypes;
        }

        public void dispatch(@NotNull E event) {
            for (int i = 0; i < flat.length; i++) {
                if (flatTypes[i].isInstance(event)) {
                    @SuppressWarnings("unchecked")
                    EventProcessor<? super E> processor = (EventProcessor<? super E>) flat[i];
                    processor.process(event);
                }
            }
        }
    }

    public static final class LinearDispatchTable<E extends Event> {
        private final EventProcessor<? extends E>[] flat;
        private final Class<? extends E>[]          flatTypes;
        private final int[]                         segmentOffsets;
        private final int[]                         segmentLengths;

        public LinearDispatchTable(EventProcessor<? extends E>[] flat,
                                   Class<? extends E>[]          flatTypes,
                                   int[]                         segmentOffsets,
                                   int[]                         segmentLengths) {
            this.flat           = flat;
            this.flatTypes      = flatTypes;
            this.segmentOffsets = segmentOffsets;
            this.segmentLengths = segmentLengths;
        }

        public void dispatch(@NotNull E event) {
            if (flat.length == 0) return;
            for (int s = 0; s < segmentOffsets.length; s++) {
                int start = segmentOffsets[s];
                int end   = start + segmentLengths[s];
                for (int i = start; i < end; i++) {
                    if (!flatTypes[i].isInstance(event)) break;
                    @SuppressWarnings("unchecked")
                    EventProcessor<? super E> processor = (EventProcessor<? super E>) flat[i];
                    processor.process(event);
                }
            }
        }
    }

    public static final class FlatStampDispatchTable<E extends Event> {
        private final EventProcessor<? extends E>[] flat;
        private final Class<? extends E>[]          flatTypes;
        private final int[]                         parentBitIndices;
        private final int[]                         selfBitIndices;
        private final int                           stampSize;

        public FlatStampDispatchTable(EventProcessor<? extends E>[] flat,
                                      Class<? extends E>[]          flatTypes,
                                      int[]                         parentBitIndices,
                                      int[]                         selfBitIndices,
                                      int                           stampSize) {
            this.flat             = flat;
            this.flatTypes        = flatTypes;
            this.parentBitIndices = parentBitIndices;
            this.selfBitIndices   = selfBitIndices;
            this.stampSize        = stampSize;
        }

        public void dispatch(@NotNull E event) {
            if (flat.length == 0) return;

//            DispatchCache cache = CacheHolder.CACHE;
//            if (cache.stamps.length < stampSize) {
//                cache.stamps = new int[stampSize];
//            }
//            if (++cache.gen == 0) {
//                Arrays.fill(cache.stamps, 0);
//                cache.gen = 1;
//            }
//            final int gen = cache.gen;
////            final int   gen    = ++cache.gen;
//            final int[] stamps = cache.stamps;
            final int[] stamps = new int[stampSize];

            for (int i = 0; i < flat.length; i++) {
                final int pIdx = parentBitIndices[i];
                if (pIdx == -1) {
                    if (!flatTypes[i].isInstance(event)) continue;
                } else {
                    if (stamps[pIdx] != 1) continue;
                }
                //if (pIdx != -1 && stamps[pIdx] != gen) continue;
                //if (!flatTypes[i].isInstance(event)) continue;
                stamps[selfBitIndices[i]] = 1;
                @SuppressWarnings("unchecked")
                EventProcessor<? super E> processor = (EventProcessor<? super E>) flat[i];
                processor.process(event);
            }
        }
    }

    public static final class ClassValueDispatchTable<E extends Event> {
        // Hidden internal cache mapping a Class type directly to its valid processors
        private final ClassValue<EventProcessor<? super E>[]> cache;

        @SuppressWarnings("unchecked")
        public ClassValueDispatchTable(EventProcessor<? extends E>[] flat, Class<? extends E>[] flatTypes) {
            this.cache = new ClassValue<>() {
                @Override
                protected EventProcessor<? super E>[] computeValue(@NonNull Class<?> type) {
                    List<EventProcessor<? super E>> matched = new ArrayList<>();

                    // Scan the flat types to resolve applicable handlers for this specific event type
                    for (int i = 0; i < flatTypes.length; i++) {
                        if (flatTypes[i].isAssignableFrom(type)) {
                            matched.add((EventProcessor<? super E>) flat[i]);
                        }
                    }
                    return matched.toArray(EventProcessor[]::new);
                }
            };

            // Eagerly prime the cache for all known registered event types in the system
            for (Class<? extends E> flatType : flatTypes) {
                this.cache.get(flatType);
            }
        }

        public void dispatch(@NotNull E event) {
            // Instantaneous metadata retrieval without hash math or collisions
            final EventProcessor<? super E>[] processors = cache.get(event.getClass());

            // Pure unrolled sequential array loop execution
            for (EventProcessor<? super E> processor : processors) {
                processor.process(event);
            }
        }
    }
}