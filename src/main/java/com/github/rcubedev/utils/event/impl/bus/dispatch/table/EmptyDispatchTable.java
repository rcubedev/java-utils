package com.github.rcubedev.utils.event.impl.bus.dispatch.table;

import com.github.rcubedev.utils.event.api.Event;
import org.jetbrains.annotations.NotNull;

public final class EmptyDispatchTable implements DispatchTable<Event> {

    @SuppressWarnings("unchecked")
    public static <E extends Event> @NotNull DispatchTable<E> empty() {
        return (DispatchTable<E>) Holder.EMPTY;
    }

    @Override
    public void dispatch(@NotNull Event event) {}

    @Override
    public void close() {}

    private static class Holder {
        private static final EmptyDispatchTable EMPTY = new EmptyDispatchTable();
    }
}
