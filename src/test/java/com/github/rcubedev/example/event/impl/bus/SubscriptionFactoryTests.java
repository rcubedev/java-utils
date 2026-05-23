package com.github.rcubedev.example.event.impl.bus;

import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.api.spi.Linkable;
import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.impl.subscription.BasicSubscription;
import com.github.rcubedev.example.event.impl.subscription.SubscriptionFactory;
import com.github.rcubedev.example.event.impl.subscription.BatchedSubscription;
import com.github.rcubedev.example.event.impl.bus.dispatch.Dispatcher;
import com.github.rcubedev.example.event.impl.bus.registry.HandlerRegistry;
import com.github.rcubedev.example.event.impl.bus.registry.RegistrySnapshot;
import com.github.rcubedev.example.event.impl.subscription.factory.BasicSubscriptionFactory;
import com.github.rcubedev.example.event.impl.subscription.factory.BatchedSubscriptionFactory;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionFactoryTests {

    @Mock private HandlerRegistry<TestEvent> registry;
    @Mock private Dispatcher<TestEvent> dispatcher;
    @Mock private BasicSubscriptionFactory basicFactory;
    @Mock private BatchedSubscriptionFactory batchedFactory;
    @InjectMocks private SubscriptionFactory<TestEvent> factory;

    @Nested
    class BasicSubscriptions {

        @Test
        @SuppressWarnings("unchecked")
        void unsubscribe_ShouldSnapshot_WhenRemovalSucceeds() {
            BasicSubscription mockBasicSub = mock(BasicSubscription.class);
            RegistrySnapshot<TestEvent> mockSnapshot = mock(RegistrySnapshot.class);
            ArgumentCaptor<Consumer<Subscription>> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
            ArgumentCaptor<Supplier<RegistrySnapshot<TestEvent>>> updateCaptor = ArgumentCaptor.forClass(Supplier.class);

            when(basicFactory.create(consumerCaptor.capture())).thenReturn(mockBasicSub);
            when(registry.remove(eq(TestEvent.class), eq(Priority.NORMAL), eq(mockBasicSub))).thenReturn(true);
            when(registry.snapshot()).thenReturn(mockSnapshot);

            factory.createBasic(TestEvent.class, Priority.NORMAL, e -> {});

            consumerCaptor.getValue().accept(mockBasicSub);

            verify(dispatcher).update(updateCaptor.capture());
            assertEquals(mockSnapshot, updateCaptor.getValue().get());
        }

        @Test
        @SuppressWarnings("unchecked")
        void unsubscribe_ShouldReturnNull_WhenRemovalFails() {
            BasicSubscription mockBasicSub = mock(BasicSubscription.class);
            ArgumentCaptor<Consumer<Subscription>> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
            ArgumentCaptor<Supplier<RegistrySnapshot<TestEvent>>> updateCaptor = ArgumentCaptor.forClass(Supplier.class);

            when(basicFactory.create(consumerCaptor.capture())).thenReturn(mockBasicSub);
            when(registry.remove(any(), any(), any())).thenReturn(false);

            factory.createBasic(TestEvent.class, Priority.NORMAL, e -> {});

            consumerCaptor.getValue().accept(mockBasicSub);

            verify(dispatcher).update(updateCaptor.capture());
            assertNull(updateCaptor.getValue().get(), "Should return null to signal no engine update needed");
            verify(registry, never()).snapshot();
        }
    }

    @Nested
    class BatchedSubscriptions {

        @Test
        @SuppressWarnings("unchecked")
        void standaloneUnsubscribe_ShouldSnapshot_WhenRemovalSucceeds() {
            BatchedSubscription mockBatchedSub = mock(BatchedSubscription.class);
            RegistrySnapshot<TestEvent> mockSnapshot = mock(RegistrySnapshot.class);
            ArgumentCaptor<Consumer<Subscription>> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
            ArgumentCaptor<Supplier<RegistrySnapshot<TestEvent>>> updateCaptor = ArgumentCaptor.forClass(Supplier.class);

            when(batchedFactory.create(any(), consumerCaptor.capture())).thenReturn(mockBatchedSub);
            when(registry.remove(eq(TestEvent.class), eq(Priority.NORMAL), eq(mockBatchedSub))).thenReturn(true);
            when(registry.snapshot()).thenReturn(mockSnapshot);

            factory.createBatched(TestEvent.class, Priority.NORMAL, e -> {});

            consumerCaptor.getValue().accept(mockBatchedSub);

            verify(dispatcher).update(updateCaptor.capture());
            assertEquals(mockSnapshot, updateCaptor.getValue().get());
        }

        @Test
        @SuppressWarnings("unchecked")
        void standaloneUnsubscribe_ShouldReturnNull_WhenRemovalFails() {
            BatchedSubscription mockBatchedSub = mock(BatchedSubscription.class);
            ArgumentCaptor<Consumer<Subscription>> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
            ArgumentCaptor<Supplier<RegistrySnapshot<TestEvent>>> updateCaptor = ArgumentCaptor.forClass(Supplier.class);

            when(batchedFactory.create(any(), consumerCaptor.capture())).thenReturn(mockBatchedSub);
            when(registry.remove(any(), any(), any())).thenReturn(false);

            factory.createBatched(TestEvent.class, Priority.NORMAL, e -> {});

            consumerCaptor.getValue().accept(mockBatchedSub);

            verify(dispatcher).update(updateCaptor.capture());
            assertNull(updateCaptor.getValue().get(), "Should return null to signal no engine update needed");
            verify(registry, never()).snapshot();
        }

        @Test
        @SuppressWarnings("unchecked")
        void internalRemove_ShouldReturnTrue_WhenRegistrySucceeds() {
            BatchedSubscription mockBatchedSub = mock(BatchedSubscription.class);
            ArgumentCaptor<Predicate<Subscription>> predicateCaptor = ArgumentCaptor.forClass(Predicate.class);

            when(batchedFactory.create(predicateCaptor.capture(), any())).thenReturn(mockBatchedSub);
            when(registry.remove(eq(TestEvent.class), eq(Priority.NORMAL), eq(mockBatchedSub))).thenReturn(true);

            factory.createBatched(TestEvent.class, Priority.NORMAL, e -> {});

            assertTrue(predicateCaptor.getValue().test(mockBatchedSub));
            verify(registry).remove(TestEvent.class, Priority.NORMAL, mockBatchedSub);
        }
    }

    @Nested
    class LinkingLogic {

        @Test
        void link_ShouldInjectSubscription_WhenListenerIsLinkable() {
            class LinkableListener implements EventProcessor<TestEvent>, Linkable {
                Subscription injected;
                @Override public void process(TestEvent event) {}
                @Override public void setSubscription(Subscription s) { this.injected = s; }
            }
            LinkableListener listener = new LinkableListener();
            BasicSubscription mockBasicSub = mock(BasicSubscription.class);
            when(basicFactory.create(any())).thenReturn(mockBasicSub);

            Subscription sub = factory.createBasic(TestEvent.class, Priority.NORMAL, listener);

            assertEquals(mockBasicSub, listener.injected);
        }

        @Test
        void link_ShouldDoNothing_WhenListenerIsNotLinkable() {
            EventProcessor<TestEvent> plainListener = e -> {};
            BasicSubscription mockBasicSub = mock(BasicSubscription.class);
            when(basicFactory.create(any())).thenReturn(mockBasicSub);

            Subscription sub = factory.createBasic(TestEvent.class, Priority.NORMAL, plainListener);

            assertEquals(mockBasicSub, sub);
        }
    }
}