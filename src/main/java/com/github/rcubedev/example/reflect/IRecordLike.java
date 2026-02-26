package com.github.rcubedev.example.reflect;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.google.common.collect.MapMaker;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interface to mimic Java Record behavior for normal classes.
 * Implementing classes should provide the record components via {@link #recordComponents()}.
 */
public interface IRecordLike {

    @ApiStatus.Internal
    Map<IRecordLike, Component[]> CACHE = new MapMaker().weakKeys().makeMap();

    /**
     * Returns the "record components" of this object.
     * <p>
     * Must return the values of all fields in declaration order.
     * Use arrays or objects directly; no deep equality for arrays.
     *
     * @return an array of record component values.
     */
    @ApiStatus.OverrideOnly
    @NotNull Component @NotNull [] recordComponents();

    default Component[] getCachedComponents() {
        return CACHE.computeIfAbsent(this, key -> recordComponents());
    }

    /**
     * Default equals implementation mirroring Java Record equality.
     */
    default boolean recordEquals(Object obj) {
        if (this == obj) return true;
        if (obj == null || this.getClass() != obj.getClass()) return false;

        Object[] thisValues = Arrays.stream(getCachedComponents()).map(Component::value).toArray();
        Object[] otherValues = Arrays.stream(((IRecordLike) obj).getCachedComponents()).map(Component::value).toArray();

        if (thisValues.length != otherValues.length) return false;

        for (int i = 0; i < thisValues.length; i++) {
            if (!Objects.equals(thisValues[i], otherValues[i])) return false;
        }
        return true;
    }

    /**
     * Default hashCode implementation mirroring Java Record hashCode.
     */
    default int recordHashCode() {
        return Arrays.hashCode(
                Arrays.stream(getCachedComponents())
                        .map(Component::value)
                        .toArray()
        );
    }

    /**
     * Default toString implementation mirroring Java Record toString.
     */
    default String recordToString() {
        return getClass().getSimpleName() + "[" +
                Arrays.stream(getCachedComponents())
                        .map(c -> c.name() + "=" + c.value())
                        .collect(Collectors.joining(", ")) +
                "]";
    }

    record Component(@NotNull String name, @Nullable Object value) {}

    @Contract("_, _ -> new")
    static @NotNull Component of(@NotNull String name, @Nullable Object value) {
        return new Component(name, value);
    }
}
