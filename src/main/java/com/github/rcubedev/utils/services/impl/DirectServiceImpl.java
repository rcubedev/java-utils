package com.github.rcubedev.utils.services.impl;

import com.github.rcubedev.utils.services.api.spi.Service;

import java.util.Objects;
import java.util.function.Supplier;

public final class DirectServiceImpl<S> implements Service<S> {

    private final Class<? extends S> type;
    private final Supplier<? extends S> supplier;
    private final Object lock = new Object();
    private volatile S cachedValue;

    public DirectServiceImpl(Class<? extends S> type, Supplier<? extends S> supplier) {
        this.type = type;
        this.supplier = supplier;
    }

    @Override
    public Class<? extends S> type() {
        return this.type;
    }

    @Override
    public S get() {
        S result = this.cachedValue;
        if (result != null) return result;

        synchronized (lock) {
            result = this.cachedValue;
            if (result != null) return result;
            result = Objects.requireNonNull(supplier.get(), () -> "Service supplier for '" + type.getName() + "' returned null");
            return this.cachedValue = result;
        }
    }
}
