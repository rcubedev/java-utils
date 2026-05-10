package com.github.rcubedev.example.mixin;

import folk.sisby.kaleido.lib.quiltconfig.api.Config;
import folk.sisby.kaleido.lib.quiltconfig.api.WrappedConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@SuppressWarnings("deprecation")
@Mixin(WrappedConfig.class)
public interface WrappedConfigAccessor {

    @Accessor("wrapped")
    Config test$getWrapped();
}
