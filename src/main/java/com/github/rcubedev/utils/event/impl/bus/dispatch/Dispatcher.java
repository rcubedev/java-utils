package com.github.rcubedev.utils.event.impl.bus.dispatch;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.exceptions.EventStackOverflowException;
import com.github.rcubedev.utils.event.api.spi.RecursionBypass;
import com.github.rcubedev.utils.event.impl.bus.dispatch.recursion.RecursionGuard;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.DispatchTable;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.DispatchTableBuilder;
import com.github.rcubedev.utils.event.impl.bus.registry.RegistrySnapshot;
import com.github.rcubedev.utils.test.UnitTestIgnored;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class Dispatcher<B extends Event> {
    private final Class<B> busType;
    private final @Nullable RecursionGuard guard;
    private final Object lock = new Object();

    private volatile DispatchTable<B> table;

    @UnitTestIgnored
    public Dispatcher(Class<B> busType, int maxStackDepth, boolean recursionGuardEnabled) {
        this(busType, recursionGuardEnabled ? new RecursionGuard(maxStackDepth) : null, DispatchTable.empty());
    }

    Dispatcher(Class<B> busType, @Nullable RecursionGuard guard, DispatchTable<B> table) {
        this.busType = busType;
        this.guard = guard;
        this.table = table;
    }

    // be weary of races
    public @NotNull DispatchTable<B> getTable() {
        return this.table;
    }

    public void update(@NotNull Supplier<@Nullable RegistrySnapshot<B>> task) {
        synchronized (lock) {
            RegistrySnapshot<B> snapshot = task.get();
            if (snapshot == null) return;

            DispatchTable<B> oldTable = this.table;
            // this is still atomic as getTable is only used as fallback if table is closed
            this.table = DispatchTableBuilder.create(busType, this::getTable).setSnapshot(snapshot).build();
            oldTable.close();
        }
    }

    public <E extends B> void dispatch(@NotNull E event) throws EventStackOverflowException {
        DispatchTable<B> stableTable = this.table;
        if (this.guard == null) {
            stableTable.dispatch(event);
            return;
        }

        this.guard.run(() -> stableTable.dispatch(event));
    }

    public @NotNull RecursionBypass openBypassTo(int extraBudget) {
        if (this.guard == null) return () -> {};
        return this.guard.bypass(extraBudget);
    }
}
