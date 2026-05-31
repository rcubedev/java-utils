package com.github.rcubedev.example.event.impl.bus.dispatch.table.builder;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.RegisteredParentResolver;

import java.util.Map;

public interface HierarchyFallbackResolverFactory<B extends Event> {

    HierarchyFallbackResolver<B> create(RegisteredParentResolver<B> resolver,
                                        Map<Class<? extends B>, EventProcessor<? super B>[]> preComputedPool);
}
