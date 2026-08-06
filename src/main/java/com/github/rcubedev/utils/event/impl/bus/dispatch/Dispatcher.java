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

import java.util.concurrent.locks.StampedLock;
import java.util.function.Supplier;

public class Dispatcher<B extends Event> {
    private final Class<B> busType;
    private final RecursionGuard guard;
    private final StampedLock lock = new StampedLock();

    private volatile DispatchTable<B> table;

    @UnitTestIgnored
    public Dispatcher(Class<B> busType, int maxStackDepth) {
        this(busType, new RecursionGuard(maxStackDepth), DispatchTable.empty());
    }

    Dispatcher(Class<B> busType, RecursionGuard guard, DispatchTable<B> table) {
        this.busType = busType;
        this.guard = guard;
        this.table = table;
    }

    public void update(@NotNull Supplier<@Nullable RegistrySnapshot<B>> task) {
        long stamp = lock.writeLock();
        try {
            RegistrySnapshot<B> snapshot = task.get();
            if (snapshot == null) return;

            DispatchTable<B> oldTable = this.table;
            this.table = DispatchTableBuilder.create(busType).setSnapshot(snapshot).build();
            oldTable.close();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public <E extends B> void dispatch(@NotNull E event) throws EventStackOverflowException {
        long stamp = lock.tryOptimisticRead();
        DispatchTable<B> stableTable = this.table;

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                stableTable = this.table;
            } finally {
                lock.unlockRead(stamp);
            }
        }

        int previous = this.guard.increment();
        // todo(jdk25): scoped values
        try {
            stableTable.dispatch(event);
        } finally {
            this.guard.resetTo(previous);
        }
    }

    public @NotNull RecursionBypass openBypassTo(int extraBudget) {
        return this.guard.bypass(extraBudget);
    }
}
