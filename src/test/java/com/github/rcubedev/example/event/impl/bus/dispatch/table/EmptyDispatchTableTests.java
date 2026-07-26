package com.github.rcubedev.example.event.impl.bus.dispatch.table;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.TestEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmptyDispatchTableTests {

    @Test
    void testEmptyFactoryMethodReturnsValidFunctionalInstance() {
        DispatchTable<Event> emptyTable = EmptyDispatchTable.empty();
        DispatchTable<Event> emptyTableSecondCall = EmptyDispatchTable.empty();

        TestEvent event = new TestEvent();

        assertNotNull(emptyTable);
        // Asserts the singleton strategy holds up perfectly
        assertSame(emptyTable, emptyTableSecondCall);

        // Verifies no exceptions occur on dispatch or closing steps
        assertDoesNotThrow(() -> emptyTable.dispatch(event));
        assertDoesNotThrow(emptyTable::close);
    }
}
