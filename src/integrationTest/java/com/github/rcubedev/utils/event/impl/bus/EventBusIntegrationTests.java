package com.github.rcubedev.utils.event.impl.bus;

import com.github.rcubedev.utils.event.api.TestEvent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

// todo
@ExtendWith(MockitoExtension.class)
public class EventBusIntegrationTests {

    @Nested
    class Initialization {
        @Test
        void testPublicConstructorInitializesDefaults() {
            EventBus<TestEvent> prodBus = new EventBus<>(TestEvent.class, 3);
            assertEquals(TestEvent.class, prodBus.getBusType());
        }
    }
}
