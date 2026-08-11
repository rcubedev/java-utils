package com.github.rcubedev.utils.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.DispatchTable;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.resolver.Resolver;

import java.util.Collection;
import java.util.function.Supplier;

public interface DispatchTableFactory<E extends Event> {
    DispatchTable<E> create(Resolver<E> fallback, Supplier<? extends DispatchTable<E>> activeTable, Collection<Class<? extends E>> warmUpTypes);
}
