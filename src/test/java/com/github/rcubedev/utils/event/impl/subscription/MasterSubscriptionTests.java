package com.github.rcubedev.utils.event.impl.subscription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterSubscriptionTests {

    @Mock
    private Runnable mockRebuild;

    @Test
    void unsubscribe_ShouldUnregisterAllChildren_AndRebuild_WhenAnyRemoved() {
        BatchedSubscription child1 = mock(BatchedSubscription.class);
        BatchedSubscription child2 = mock(BatchedSubscription.class);
        when(child1.unregister()).thenReturn(true);
        when(child2.unregister()).thenReturn(true);

        MasterSubscription master = new MasterSubscription(new BatchedSubscription[]{child1, child2}, mockRebuild);
        master.unsubscribe();

        verify(child1, times(1)).unregister();
        verify(child2, times(1)).unregister();
        verify(mockRebuild, times(1)).run();
    }

    @Test
    void unsubscribe_ShouldNotRebuild_WhenNoChildrenRemoved() {
        BatchedSubscription child1 = mock(BatchedSubscription.class);
        BatchedSubscription child2 = mock(BatchedSubscription.class);
        when(child1.unregister()).thenReturn(false);
        when(child2.unregister()).thenReturn(false);

        MasterSubscription master = new MasterSubscription(new BatchedSubscription[]{child1, child2}, mockRebuild);
        master.unsubscribe();

        verify(mockRebuild, never()).run();
    }

    @Test
    void unsubscribe_ShouldRebuild_WhenOnlyOneChildRemoved() {
        BatchedSubscription child1 = mock(BatchedSubscription.class);
        BatchedSubscription child2 = mock(BatchedSubscription.class);
        when(child1.unregister()).thenReturn(false);
        when(child2.unregister()).thenReturn(true);

        MasterSubscription master = new MasterSubscription(new BatchedSubscription[]{child1, child2}, mockRebuild);
        master.unsubscribe();

        verify(mockRebuild, times(1)).run();
    }

    @Test
    void unsubscribe_ShouldShortCircuit_WhenCalledMultipleTimes() {
        BatchedSubscription child = mock(BatchedSubscription.class);
        when(child.unregister()).thenReturn(true);

        MasterSubscription master = new MasterSubscription(new BatchedSubscription[]{child}, mockRebuild);
        master.unsubscribe();
        master.unsubscribe();

        verify(child, times(1)).unregister();
        verify(mockRebuild, times(1)).run();
    }

    @Test
    void unsubscribe_ShouldHandleEmptyChildren() {
        MasterSubscription master = new MasterSubscription(new BatchedSubscription[]{}, mockRebuild);

        master.unsubscribe();

        verify(mockRebuild, never()).run();
    }
}