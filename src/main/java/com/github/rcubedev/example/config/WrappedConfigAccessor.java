package com.github.rcubedev.example.config;

import folk.sisby.kaleido.lib.quiltconfig.api.Config;

// unused currently but will likely be used in future for config reloading.
// users will be expected to implement this (e.g. with an accessor ext this ife)
// as this is a java lib without mixin.
@FunctionalInterface
public interface WrappedConfigAccessor {

    Config test$getWrapped();
}
