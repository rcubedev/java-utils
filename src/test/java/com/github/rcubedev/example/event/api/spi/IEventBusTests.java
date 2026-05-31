package com.github.rcubedev.example.event.api.spi;

import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.api.exceptions.EventStackOverflowException;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IEventBusTests {

    private IEventBus<TestEvent> bus;

    private Class<?> capturedClass;
    private Priority capturedPriority;
    private EventProcessor<?> capturedProcessor;
    private int capturedExtraBudget;

    @Mock private EventProcessor<TestEvent> mockProcessor;
    @Mock private Subscription mockSubscription;
    @Mock private RecursionBypass mockBypass;

    @BeforeEach
    void setUp() {
        capturedClass = null;
        capturedPriority = null;
        capturedProcessor = null;
        capturedExtraBudget = -1;

        bus = new IEventBus<>() {
            @Override
            public <E extends TestEvent> void post(E event) throws EventStackOverflowException {}

            @Override
            public <E extends TestEvent> @NotNull Subscription register(Class<E> eventType, Priority priority, EventProcessor<E> listener) {
                capturedClass = eventType;
                capturedPriority = priority;
                capturedProcessor = listener;
                return mockSubscription;
            }

            @Override
            public @NotNull Subscription register(Object target) {
                return mockSubscription;
            }

            @Override
            public @NotNull Class<TestEvent> getBusType() {
                return TestEvent.class;
            }

            @Override
            public @NotNull RecursionBypass openBypassTo(int extraBudget) {
                capturedExtraBudget = extraBudget;
                return mockBypass;
            }
        };
    }

    @Nested
    class RecursionBypassDefaults {

        @Test
        void openBypass_ShouldDelegateToOpenBypassToWithMaxIntegerHalved() {
            RecursionBypass result = bus.openBypass();

            assertNotNull(result);
            assertEquals(mockBypass, result);
            assertEquals(Integer.MAX_VALUE / 2, capturedExtraBudget);
        }
    }

    @Nested
    class RegistrationDefaults {

        @Test
        void register_WithTypeAndProcessor_ShouldDefaultToPriorityNormal() {
            Subscription result = bus.register(TestEvent.class, mockProcessor);

            assertEquals(mockSubscription, result);
            assertEquals(TestEvent.class, capturedClass);
            assertEquals(Priority.NORMAL, capturedPriority);
            assertEquals(mockProcessor, capturedProcessor);
        }

        @Test
        void register_WithProcessorAndPriority_ShouldUseBusType() {
            Subscription result = bus.register(mockProcessor, Priority.HIGH);

            assertEquals(mockSubscription, result);
            assertEquals(TestEvent.class, capturedClass);
            assertEquals(Priority.HIGH, capturedPriority);
            assertEquals(mockProcessor, capturedProcessor);
        }

        @Test
        void register_WithProcessorOnly_ShouldDefaultToBusTypeAndPriorityNormal() {
            Subscription result = bus.register(mockProcessor);

            assertEquals(mockSubscription, result);
            assertEquals(TestEvent.class, capturedClass);
            assertEquals(Priority.NORMAL, capturedPriority);
            assertEquals(mockProcessor, capturedProcessor);
        }
    }
}