package com.github.rcubedev.example.event.api;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("Skipping because this triggers global static initialization and leaks singleton state.")
// todo move to integration
class EventBusRegistryTests {

    @Test
    void getInstance_ShouldReturnNonNullSingletonInstance() {
        EventBusRegistry registry = EventBusRegistry.getInstance();

        assertNotNull(registry, "The global registry instance holder should never be null.");

        assertSame(registry, EventBusRegistry.getInstance(), "Subsequent calls must return the identical holder instance.");
    }
}