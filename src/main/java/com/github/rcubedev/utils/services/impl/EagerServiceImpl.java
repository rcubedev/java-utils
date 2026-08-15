package com.github.rcubedev.utils.services.impl;

import com.github.rcubedev.utils.services.api.spi.Service;

public final class EagerServiceImpl<S> implements Service<S> {

    private final S instance;

    public EagerServiceImpl(S instance) {
        this.instance = instance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends S> type() {
        return (Class<? extends S>) this.instance.getClass();
    }

    @Override
    public S get() {
        return this.instance;
    }
}
