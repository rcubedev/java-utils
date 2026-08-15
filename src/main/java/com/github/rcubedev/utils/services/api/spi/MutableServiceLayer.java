package com.github.rcubedev.utils.services.api.spi;

import java.util.function.Supplier;

public interface MutableServiceLayer extends ServiceLayer {

    <S, T extends S> MutableServiceLayer register(Class<S> contract, Supplier<T> factory, Class<T> implType);
}