package com.github.rcubedev.example.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.DispatchTable;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.RegisteredParentResolver;
import com.github.rcubedev.example.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Flattener<B extends Event> {

    private final RegistrySnapshot<B> snapshot;
    private final RegisteredParentResolver<B> resolver;

    public Flattener(RegistrySnapshot<B> snapshot, RegisteredParentResolver<B> resolver) {
        this.snapshot = snapshot;
        this.resolver = resolver;
    }

    @SuppressWarnings("unchecked")
    public @NotNull DispatchTable<B> flatten(List<List<Class<? extends B>>> families) {
        Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> handlers = snapshot.getHandlers();

        // Flatten into 1D dispatch array. Priority-first, superclass -> subclass within family
        List<EventProcessor<? extends B>> flatProcessors = new ArrayList<>();
        List<Class<? extends B>> flatTypes = new ArrayList<>();
        List<Integer> parentBitIndicesList = new ArrayList<>();
        List<Integer> selfBitIndicesList = new ArrayList<>();

        // Map every unique type to a bit index
        Map<Class<? extends B>, Integer> typeToBitIndex = new HashMap<>();
        Set<Class<? extends B>> handlerTypes = handlers.keySet();
        int idx = 0;
        for (Class<? extends B> type : handlerTypes) {
            typeToBitIndex.put(type, idx++);
        }
        // System.out.println("Type to bit index: " + typeToBitIndex);

        Priority[] priorities = Priority.values();
        int numPriorities = priorities.length;
        int numFamilies = families.size();

        // Segment (p, f) stored at p*numFamilies+f. Keeps break valid per (priority, family) chunk
        int[] segmentOffsets = new int[numPriorities * numFamilies];
        int[] segmentLengths = new int[numPriorities * numFamilies];

        for (int p = 0; p < numPriorities; p++) {
            Priority priority = priorities[p];
            for (int f = 0; f < numFamilies; f++) {
                int segIdx = p * numFamilies + f;
                segmentOffsets[segIdx] = flatProcessors.size();

                for (Class<? extends B> type : families.get(f)) {
                    Map<Priority, EventSinkSnapshot<? extends B>> priorityHandlers = handlers.get(type);
                    if (priorityHandlers == null) continue;

                    EventSinkSnapshot<? extends B> handler = priorityHandlers.get(priority);
                    if (handler == null) continue;

                    // Data for the dispatch table
                    flatProcessors.add(handler.invoker());
                    flatTypes.add(handler.eventType());

                    // Map self and parent bits
                    selfBitIndicesList.add(typeToBitIndex.get(type));

                    Class<? extends B> parent = resolver.getRegisteredParentAsExtendsBus(type);
                    parentBitIndicesList.add(parent == null ? -1 : typeToBitIndex.get(parent));

                    // flatChecking.add(new Pair<>(handler.eventType(), priority));
                }
                segmentLengths[segIdx] = flatProcessors.size() - segmentOffsets[segIdx];
            }
        }

        // System.out.println("Flat checking: " + flatChecking);
        return new DispatchTable<>(
                flatProcessors.toArray(EventProcessor[]::new),
                flatTypes.toArray(Class[]::new),
                segmentOffsets,
                segmentLengths,
                parentBitIndicesList.stream().mapToInt(i -> i).toArray(),
                selfBitIndicesList.stream().mapToInt(i -> i).toArray(),
                (handlerTypes.size() + 63) / 64
        );
    }
}
