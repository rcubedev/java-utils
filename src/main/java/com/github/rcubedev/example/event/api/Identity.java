package com.github.rcubedev.example.event.api;

import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

public final class Identity {

    private final MethodHandles.Lookup lookup;

    private Identity(@NotNull MethodHandles.Lookup lookup) {
        this.lookup = Objects.requireNonNull(lookup, "lookup cannot be null");
    }

    public static Identity of(@NotNull MethodHandles.Lookup lookup) {
        return new Identity(lookup);
    }

    public static Identity ofPublic() {
        return Identity.of(MethodHandles.publicLookup());
    }

    public MethodHandles.Lookup lookup() {
        return this.lookup;
    }
}
