package com.github.rcubedev.utils.event.impl.bus.dispatch.table;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.builder.DispatchTableFactory;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.builder.FamilyBuilder;
import com.github.rcubedev.utils.event.impl.bus.dispatch.table.builder.Flattener;
import com.github.rcubedev.utils.event.impl.bus.registry.RegistrySnapshot;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Supplier;

public final class DispatchTableBuilder<B extends Event> {

    private final Class<B> busType;
    private final Supplier<? extends DispatchTable<B>> activeTable;
    private final DispatchTableFactory<B> tableFactory;
    private RegistrySnapshot<B> snapshot;
    private @NotNull DispatchTable<B> table = DispatchTable.empty();

    private DispatchTableBuilder(Class<B> busType, Supplier<? extends DispatchTable<B>> activeTable) {
        this(busType, activeTable, DispatchTable::create);
    }

    private DispatchTableBuilder(Class<B> busType, Supplier<? extends DispatchTable<B>> activeTable, DispatchTableFactory<B> tableFactory) {
        this.busType = busType;
        this.activeTable = activeTable;
        this.tableFactory = tableFactory;
    }

    // activeTable should point to a different table after the table is closed.
    public static <B extends Event> @NotNull DispatchTableBuilder<B> create(Class<B> busType, Supplier<? extends DispatchTable<B>> activeTable) {
        return new DispatchTableBuilder<>(busType, activeTable);
    }

    // activeTable should point to a different table after the table is closed
    public static <B extends Event> @NotNull DispatchTable<B> create(Class<B> busType, Supplier<? extends DispatchTable<B>> activeTable, RegistrySnapshot<B> snapshot) {
        return new DispatchTableBuilder<>(busType, activeTable).setSnapshot(snapshot).build();
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
        Flattener.Result<B> result = flattener.flatten(families);
        return tableFactory.create(result.resolver(), activeTable, result.warmUpTypes());
    }
}