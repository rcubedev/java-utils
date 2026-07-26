package com.github.rcubedev.example.services.impl;

import com.github.rcubedev.example.services.api.spi.Service;

public final class EagerServiceImpl<S> implements Service<S> {

    private final Class<? extends S> type;
    private final S instance;

    public EagerServiceImpl(Class<? extends S> type, S instance) {
        this.type = type;
        this.instance = instance;
    }

    @Override
    public Class<? extends S> type() {
        return this.type;
    }

    @Override
    public S get() {
        return this.instance;
    }
}
