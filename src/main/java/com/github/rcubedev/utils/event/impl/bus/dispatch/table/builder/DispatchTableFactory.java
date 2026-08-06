package com.github.rcubedev.utils.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.DispatchTable;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.resolver.Resolver;

import java.util.Collection;

public interface DispatchTableFactory<E extends Event> {
    DispatchTable<E> create(Resolver<E> fallback, Collection<Class<? extends E>> warmUpTypes);
}
