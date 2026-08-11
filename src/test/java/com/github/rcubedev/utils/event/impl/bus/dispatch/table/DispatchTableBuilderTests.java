package com.github.rcubedev.utils.event.impl.bus.dispatch.table;

import com.github.rcubedev.utils.event.api.Event;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.api.TestEvent;
import com.github.rcubedev.utils.event.impl.bus.handler.EventSinkSnapshot;
import com.github.rcubedev.utils.event.impl.bus.registry.RegistrySnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatchTableBuilderTests {

    @Mock
    private RegistrySnapshot<Event> mockSnapshot;

    @Mock
    private DispatchTable<Event> mockActiveTable;

    @Test
    void create_WithBusTypeAndActiveTable_ShouldInitializeWithEmptyTable() {
        DispatchTableBuilder<Event> builder = DispatchTableBuilder.create(Event.class, () -> mockActiveTable);
        DispatchTable<Event> table = builder.build();

        assertNotNull(table, "Initial table instance should never be null");
        assertSame(DispatchTable.empty(), table);
    }

    @Test
    void setSnapshot_WithEmptySnapshot_ShouldReturnEmptyTable() {
        when(mockSnapshot.getHandlers()).thenReturn(Map.of());
        DispatchTableBuilder<Event> builder = DispatchTableBuilder.create(Event.class, () -> mockActiveTable);

        DispatchTableBuilder<Event> returnedBuilder = builder.setSnapshot(mockSnapshot);
        DispatchTable<Event> table = builder.build();

        assertSame(builder, returnedBuilder);
        assertSame(DispatchTable.empty(), table);
    }

    @Test
    void staticCreate_WithSnapshot_ShouldBuildDirectly() {
        when(mockSnapshot.getHandlers()).thenReturn(Map.of());

        DispatchTable<Event> table = DispatchTableBuilder.create(Event.class, () -> mockActiveTable, mockSnapshot);

        assertNotNull(table);
        assertSame(DispatchTable.empty(), table);
    }

    @Test
    void buildCurrent_WithPopulatedSnapshot_ShouldInvokeInternalPipelineWithoutCrashing() {
        Map<Class<? extends Event>, Map<Priority, EventSinkSnapshot<? extends Event>>> mockHandlers = Map.of(TestEvent.class, Map.of());
        when(mockSnapshot.getHandlers()).thenReturn(mockHandlers);

        assertDoesNotThrow(() -> {
            DispatchTable<Event> table = DispatchTableBuilder.create(Event.class, () -> mockActiveTable, mockSnapshot);
            assertNotNull(table);
        });
    }
}