package com.github.rcubedev.utils.event.test;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.spi.IEventBus;
import com.github.rcubedev.utils.event.impl.EventBus;
import org.jetbrains.annotations.NotNull;

@Deprecated
public interface TestableEventBus<B extends Event> extends IEventBus<B> {

    @NotNull TestableDispatchTable getDispatchTable();
    void setDispatchTable(@NotNull EventBus.DispatchTable table);
    int getCurrentRecursionDepth();
    void setRecursionDepth(int depth);
}
