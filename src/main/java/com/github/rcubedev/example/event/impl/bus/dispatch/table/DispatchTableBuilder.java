package com.github.rcubedev.example.event.impl.bus.dispatch.table;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.builder.FamilyBuilder;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.builder.Flattener;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DispatchTableBuilder<B extends Event> {

    private final Class<B> busType;
    private RegistrySnapshot<B> snapshot;
    private @NotNull DispatchTable<B> table = DispatchTable.empty();

    private DispatchTableBuilder(Class<B> busType) {
        this.busType = busType;
    }

    public static <B extends Event> @NotNull DispatchTableBuilder<B> create(Class<B> busType) {
        return new DispatchTableBuilder<>(busType);
    }

    public static <B extends Event> @NotNull DispatchTable<B> create(Class<B> busType, RegistrySnapshot<B> snapshot) {
        return new DispatchTableBuilder<>(busType).setSnapshot(snapshot).build();
    }

    public @NotNull DispatchTableBuilder<B> setSnapshot(@NotNull RegistrySnapshot<B> snapshot) {
        this.snapshot = snapshot;
        this.table = buildCurrent();
        return this;
    }

    public @NotNull DispatchTable<B> build() {
        return table;
    }

    private @NotNull DispatchTable<B> buildCurrent() {
        RegisteredParentResolver<B> resolver = new RegisteredParentResolver<>(busType, snapshot);
        if (snapshot.getHandlers().isEmpty()) return DispatchTable.empty();

        FamilyBuilder<B> familyBuilder = new FamilyBuilder<>(snapshot, resolver);
        Flattener<B> flattener = new Flattener<>(snapshot, resolver);

        List<List<Class<? extends B>>> families = familyBuilder.buildFamilies();
        return flattener.flatten(families);
    }
}