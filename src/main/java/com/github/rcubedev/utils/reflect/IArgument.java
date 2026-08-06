package com.github.rcubedev.utils.reflect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IArgument<T> {

    /**
     * Returns the held item.
     *
     * @return The held item.
     */
    @Nullable T get();

    /**
     * Returns the static type of the argument.
     *
     * @return The typed class.
     */
    @NotNull TypedClass<? super T> getStaticType();

    /**
     * Returns the {@link Argument.Kind}.
     * @return The {@link Argument.Kind}.
     */
    @NotNull Argument.Kind getKind();
}
