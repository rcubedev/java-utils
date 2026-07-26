package com.github.rcubedev.example.event.impl.bus.dispatch;

import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.api.spi.RecursionBypass;
import com.github.rcubedev.example.event.impl.bus.dispatch.recursion.RecursionGuard;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.DispatchTable;
import com.github.rcubedev.example.event.impl.bus.dispatch.table.DispatchTableBuilder;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.concurrent.locks.StampedLock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DispatcherTests {

    @Mock private RecursionGuard guard;
    @Mock private DispatchTable<TestEvent> initialTable;
    @Mock private RegistrySnapshot<TestEvent> snapshot;

    private Dispatcher<TestEvent> dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new Dispatcher<>(TestEvent.class, guard, initialTable);
    }

    @Nested
    class Dispatching {

        @Test
        void dispatch_FollowsProtocol() {
            TestEvent event = new TestEvent();
            when(guard.increment()).thenReturn(42);

            dispatcher.dispatch(event);

            InOrder inOrder = inOrder(guard, initialTable);
            inOrder.verify(guard).increment();
            inOrder.verify(initialTable).dispatch(event);
            inOrder.verify(guard).resetTo(42);
        }

        @Test
        void dispatch_ResetsGuardOnException() {
            when(guard.increment()).thenReturn(5);
            doThrow(new RuntimeException()).when(initialTable).dispatch(any());

            assertThrows(RuntimeException.class, () -> dispatcher.dispatch(new TestEvent()));

            verify(guard).resetTo(5);
        }

        // fixme jank: uses reflection, mocks things that do not own (StampedLock)
        @Test
        void dispatch_RecoversViaReadLockWhenOptimisticReadIsInvalidated() throws Exception {
            Field lockField = Dispatcher.class.getDeclaredField("lock");
            lockField.setAccessible(true);

            StampedLock realLock = (StampedLock) lockField.get(dispatcher);
            StampedLock lockSpy = spy(realLock);

            lockField.set(dispatcher, lockSpy);

            doReturn(false).when(lockSpy).validate(anyLong());
            when(guard.increment()).thenReturn(7);

            TestEvent event = new TestEvent();
            dispatcher.dispatch(event);

            verify(lockSpy).readLock();
            verify(lockSpy).unlockRead(anyLong());
            verify(initialTable).dispatch(event);
            verify(guard).resetTo(7);
        }
    }

    @Nested
    class Updating {

        @Test
        void update_BailsOnNullSnapshot() {
            try (MockedStatic<DispatchTableBuilder> builderMock = mockStatic(DispatchTableBuilder.class)) {
                dispatcher.update(() -> null);
                builderMock.verifyNoInteractions();
                verify(initialTable, never()).close();
            }
        }

        @Test
        @SuppressWarnings("unchecked")
        void update_SwapsTableOnValidSnapshot() {
            try (MockedStatic<DispatchTableBuilder> builderMock = mockStatic(DispatchTableBuilder.class)) {
                DispatchTableBuilder<TestEvent> builder = mock(DispatchTableBuilder.class);
                DispatchTable<TestEvent> newTable = mock(DispatchTable.class);

                builderMock.when(() -> DispatchTableBuilder.create(any())).thenReturn(builder);
                when(builder.setSnapshot(any())).thenReturn(builder);
                when(builder.build()).thenReturn(newTable);

                dispatcher.update(() -> snapshot);

                verify(initialTable, times(1)).close();

                TestEvent testEvent = new TestEvent();
                dispatcher.dispatch(testEvent);

                verify(newTable).dispatch(testEvent);
                verify(initialTable, never()).dispatch(any(TestEvent.class));
            }
        }
    }

    @Nested
    class Helpers {
        @Test
        void openBypassTo_DelegatesToGuard() {
            RecursionBypass mockBypass = mock(RecursionBypass.class);
            when(guard.bypass(10)).thenReturn(mockBypass);

            assertEquals(mockBypass, dispatcher.openBypassTo(10));
            verify(guard).bypass(10);
        }
    }
}
