package com.github.rcubedev.utils.event.impl.bus.registry;

import com.github.rcubedev.utils.event.api.EventProcessor;
import com.github.rcubedev.utils.event.api.Priority;
import com.github.rcubedev.utils.event.api.TestEvent;
import com.github.rcubedev.utils.event.api.spi.Subscription;
import com.github.rcubedev.utils.event.impl.subscription.BatchedSubscription;
import com.github.rcubedev.utils.event.impl.subscription.SubscriptionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationBufferTests {

    private RegistrationBuffer<TestEvent> registrationBuffer;
    private List<BatchedSubscription> subscriptionsList;

    @Mock private SubscriptionFactory<TestEvent> mockFactory;
    @Mock private BatchedSubscription mockBatchedSubscription;
    @Mock private EventProcessor<TestEvent> mockProcessor;
    @Mock private HandlerRegistry<TestEvent> mockRegistry;

    @BeforeEach
    void setUp() {
        subscriptionsList = new ArrayList<>();
        registrationBuffer = new RegistrationBuffer<>(mockFactory, subscriptionsList);
    }

    @Test
    void register_ShouldCreateSubscriptionAndStoreItInInternalStructures() {
        Class<TestEvent> type = TestEvent.class;
        Priority priority = Priority.HIGH;
        
        doReturn(mockBatchedSubscription).when(mockFactory).createBatched(type, priority, mockProcessor);

        Subscription returnedSubscription = registrationBuffer.register(type, priority, mockProcessor);

        assertNotNull(returnedSubscription);
        assertEquals(mockBatchedSubscription, returnedSubscription);
        
        assertEquals(1, subscriptionsList.size());
        assertEquals(mockBatchedSubscription, subscriptionsList.getFirst());
        
        verify(mockFactory, times(1)).createBatched(type, priority, mockProcessor);
    }

    @Test
    void flush_ShouldForwardAllStagedRegistrationsToTheProvidedRegistry() {
        Class<TestEvent> type = TestEvent.class;
        Priority priority = Priority.MONITOR;
        
        doReturn(mockBatchedSubscription).when(mockFactory).createBatched(type, priority, mockProcessor);
        
        registrationBuffer.register(type, priority, mockProcessor);

        registrationBuffer.flush(mockRegistry);

        verify(mockRegistry, times(1)).add(type, priority, mockProcessor, mockBatchedSubscription);
    }

    @Test
    void flush_WhenBufferIsEmpty_ShouldDoNothing() {
        registrationBuffer.flush(mockRegistry);

        verifyNoInteractions(mockRegistry);
    }
}