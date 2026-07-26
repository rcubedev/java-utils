package com.github.rcubedev.example.services.impl;

import com.github.rcubedev.example.services.api.spi.Service;

import java.util.ServiceLoader;

public class ProviderServiceImpl<S> implements Service<S> {

    private final ServiceLoader.Provider<S> provider;

    public ProviderServiceImpl(ServiceLoader.Provider<S> provider) {
        this.provider = provider;
    }

    @Override
    public Class<? extends S> type() {
        return this.provider.type();
    }

    @Override
    public S get() {
        return this.provider.get();
    }
}
