package com.github.rcubedev.example.event.test;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.spi.IEventBus;
import com.github.rcubedev.example.event.impl.EventBus;
import org.jetbrains.annotations.NotNull;

public interface TestableEventBus<B extends Event> extends IEventBus<B> {

    @NotNull TestableDispatchTable getDispatchTable();
    void setDispatchTable(@NotNull EventBus.DispatchTable table);
    int getCurrentRecursionDepth();
    void setRecursionDepth(int depth);
}
