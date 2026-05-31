package com.github.rcubedev.example.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.DispatchTable;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.FallbackResolver;

import java.util.Map;

public interface DispatchTableFactory<E extends Event> {
    DispatchTable<E> create(Map<Class<? extends E>, EventProcessor<? super E>[]> preComputedPool, FallbackResolver<E> fallback);
}
