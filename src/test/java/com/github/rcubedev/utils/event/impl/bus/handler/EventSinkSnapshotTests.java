package com.github.rcubedev.utils.event.impl.bus.handler;

import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.api.TestEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EventSinkSnapshotTests {

    @Mock private EventProcessor<TestEvent> mockInvoker;

    @Test
    void shouldStoreAndReturnConstructorArgumentsCorrectly() {
        EventSinkSnapshot<TestEvent> snapshot = new EventSinkSnapshot<>(
                TestEvent.class, 
                Priority.HIGH, 
                mockInvoker
        );

        assertEquals(TestEvent.class, snapshot.eventType());
        assertEquals(Priority.HIGH, snapshot.priority());
        assertEquals(mockInvoker, snapshot.invoker());
    }
}