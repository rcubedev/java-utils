package com.github.rcubedev.example.event.impl.bus.registry.factory;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.impl.bus.handler.ArrayBackedEventSink;
import com.github.rcubedev.example.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

// fixme this isn't a great name
public final class RegistrySnapshotFactory<B extends Event> {

    public RegistrySnapshot<B> create(Map<Class<? extends B>, Map<Priority, ArrayBackedEventSink<? extends B>>> handlers) {
        Map<Class<? extends B>, Map<Priority, EventSinkSnapshot<? extends B>>> snapshot = HashMap.newHashMap(handlers.size());

        var handlersSet = handlers.entrySet();
        for (var entry : handlersSet) {
            Map<Priority, ArrayBackedEventSink<? extends B>> sinks = entry.getValue();

            if (sinks == null || sinks.isEmpty()) continue;

            Map<Priority, EventSinkSnapshot<? extends B>> priorityMap = new EnumMap<>(Priority.class);
            for (var sinkEntry : sinks.entrySet()) {
                priorityMap.put(sinkEntry.getKey(), sinkEntry.getValue().snapshot());
            }
            snapshot.put(entry.getKey(), priorityMap);
        }

        return new RegistrySnapshot<>(snapshot);
    }
}