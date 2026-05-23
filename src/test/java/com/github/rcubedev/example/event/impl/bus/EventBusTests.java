package com.github.rcubedev.example.event.impl.bus;

import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.impl.bus.factory.RegistrationSessionFactory;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrationSession;
import com.github.rcubedev.example.event.impl.subscription.MasterSubscription;
import com.github.rcubedev.example.event.impl.subscription.factory.MasterSubscriptionFactory;
import com.github.rcubedev.example.event.impl.subscription.SubscriptionFactory;
import com.github.rcubedev.example.event.impl.subscriber.EventSubscriberCompiler;
import com.github.rcubedev.example.event.impl.bus.dispatch.Dispatcher;
import com.github.rcubedev.example.event.impl.bus.registry.HandlerRegistry;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventBusTests {

    @Mock HandlerRegistry<TestEvent> handlerRegistry;
    @Mock Dispatcher<TestEvent> dispatcher;
    @Mock SubscriptionFactory<TestEvent> factory;
    @Mock EventSubscriberCompiler<TestEvent> compiler;
    @Mock RegistrationSessionFactory<TestEvent> sessionFactory;
    @Mock MasterSubscriptionFactory masterSubFactory;

    private EventBus<TestEvent> bus;

    @BeforeEach
    public void setup() {
        bus = new EventBus<>(TestEvent.class, handlerRegistry, dispatcher, factory, compiler, sessionFactory, masterSubFactory);
    }

//    @Nested
//    class Initialization {
//        @Test
//        void testPublicConstructorInitializesDefaults() {
//            EventBus<TestEvent> prodBus = new EventBus<>(TestEvent.class, 3);
//            assertEquals(TestEvent.class, prodBus.getBusType());
//        }
//    }

    @Nested
    class Metadata {
        @Test
        void testGetBusType() {
            assertEquals(TestEvent.class, bus.getBusType());
        }
    }

    @Nested
    class Dispatching {
        @Test
        void testPost_DelegatesToDispatcher() {
            TestEvent event = new TestEvent();
            bus.post(event);
            verify(dispatcher).dispatch(event);
        }

        @Test
        void testOpenBypassTo_DelegatesToDispatcher() {
            bus.openBypassTo(5);
            verify(dispatcher).openBypassTo(5);
        }
    }

    @Nested
    class BasicRegistration {
        @Test
        void testRegisterBasic_Flow() {
            EventProcessor<TestEvent.SubEvent> processor = e -> {
            };
            Subscription mockSub = mock(Subscription.class);
            @SuppressWarnings("unchecked")
            RegistrySnapshot<TestEvent> mockSnapshot = mock(RegistrySnapshot.class);

            when(factory.createBasic(eq(TestEvent.SubEvent.class), any(), any())).thenReturn(mockSub);
            when(handlerRegistry.snapshot()).thenReturn(mockSnapshot);

            Subscription result = bus.register(TestEvent.SubEvent.class, Priority.NORMAL, processor);

            assertEquals(mockSub, result);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Supplier<RegistrySnapshot<TestEvent>>> captor = ArgumentCaptor.forClass(Supplier.class);
            verify(dispatcher).update(captor.capture());

            RegistrySnapshot<TestEvent> snapshotResult = captor.getValue().get();
            verify(handlerRegistry).add(TestEvent.SubEvent.class, Priority.NORMAL, processor, mockSub);
            assertEquals(mockSnapshot, snapshotResult);
        }
    }

    @Nested
    class ObjectRegistration {
        @Test
        void testRegisterObject_Flow() {
            Object target = new Object();

            @SuppressWarnings("unchecked")
            RegistrySnapshot<TestEvent> mockSnapshot = mock(RegistrySnapshot.class);
            MasterSubscription mockMaster = mock(MasterSubscription.class);
            @SuppressWarnings("unchecked")
            RegistrationSession<TestEvent> mockSession = mock(RegistrationSession.class);

            when(handlerRegistry.snapshot()).thenReturn(mockSnapshot);
            when(sessionFactory.create(any(), any(), any())).thenReturn(mockSession);
            when(masterSubFactory.create(any(), any())).thenReturn(mockMaster);

            Subscription master = bus.register(target);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Supplier<RegistrySnapshot<TestEvent>>> captor = ArgumentCaptor.forClass(Supplier.class);
            verify(dispatcher).update(captor.capture());

            RegistrySnapshot<TestEvent> result = captor.getValue().get();

            verify(sessionFactory).create(eq(handlerRegistry), eq(factory), any());
            verify(compiler).register(eq(target), eq(mockSession));
            verify(handlerRegistry).snapshot();
            assertEquals(mockSnapshot, result);
            assertNotNull(master);
        }

        @Test
        void testRegisterObject_InternalRegistrarBehavior() {
            Object target = new Object();

            @SuppressWarnings("unchecked")
            RegistrationSession<TestEvent> mockSession = mock(RegistrationSession.class);
            when(sessionFactory.create(any(), any(), any())).thenReturn(mockSession);
            when(masterSubFactory.create(any(), any())).thenReturn(mock(MasterSubscription.class));

            bus.register(target);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Supplier<RegistrySnapshot<TestEvent>>> updateCaptor = ArgumentCaptor.forClass(Supplier.class);
            verify(dispatcher).update(updateCaptor.capture());
            updateCaptor.getValue().get();

            verify(sessionFactory).create(eq(handlerRegistry), eq(factory), any());
            verify(compiler).register(eq(target), eq(mockSession));
        }

        @Test
        void testRegisterObject_UnsubscribeTriggersDispatcherUpdate() {
            Object target = new Object();

            MasterSubscription mockMaster = mock(MasterSubscription.class);
            @SuppressWarnings("unchecked")
            RegistrationSession<TestEvent> mockSession = mock(RegistrationSession.class);
            @SuppressWarnings("unchecked")
            RegistrySnapshot<TestEvent> mockSnapshot = mock(RegistrySnapshot.class);

            when(sessionFactory.create(any(), any(), any())).thenReturn(mockSession);
            when(masterSubFactory.create(any(), any())).thenReturn(mockMaster);
            when(handlerRegistry.snapshot()).thenReturn(mockSnapshot);

            doAnswer(invocation -> {
                Supplier<RegistrySnapshot<TestEvent>> supplier = invocation.getArgument(0);
                supplier.get();
                return null;
            }).when(dispatcher).update(any());

            bus.register(target);

            // Capture the rebuild runnable passed to masterSubFactory and invoke it directly
            ArgumentCaptor<Runnable> rebuildCaptor = ArgumentCaptor.forClass(Runnable.class);
            verify(masterSubFactory).create(any(), rebuildCaptor.capture());
            rebuildCaptor.getValue().run();

            verify(dispatcher, times(2)).update(any());
        }
    }
}
