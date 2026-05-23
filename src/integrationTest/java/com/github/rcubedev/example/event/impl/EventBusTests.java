package com.github.rcubedev.example.event.impl;

import com.github.rcubedev.example.event.api.EventBusRegistry;
import com.github.rcubedev.example.event.api.EventProcessor;
import com.github.rcubedev.example.event.api.Priority;
import com.github.rcubedev.example.event.api.TestEvent;
import com.github.rcubedev.example.event.api.exceptions.EventStackOverflowException;
import com.github.rcubedev.example.event.api.spi.Linkable;
import com.github.rcubedev.example.event.api.spi.RecursionBypass;
import com.github.rcubedev.example.event.api.spi.Subscription;
import com.github.rcubedev.example.event.test.TestableDispatchTable;
import com.github.rcubedev.example.event.test.TestableEventBus;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EventBusTests {

    static abstract class OtherEvent extends TestEvent {}

    static class ParentEvent extends TestEvent {}
    static class ChildEvent extends ParentEvent {}
    static class GrandchildEvent extends ChildEvent {}

    static class BranchRootEvent extends TestEvent {}
    static class BranchAEvent extends BranchRootEvent {}
    static class BranchAChildEvent extends BranchAEvent {}
    static class BranchBEvent extends BranchRootEvent {}
    static class BranchBChildEvent extends BranchBEvent {}

    static class TestBus {
        static final TestableEventBus<TestEvent> INSTANCE = new EventBus<>(TestEvent.class, 128);
    }

    @Test
    void checkBusTypeMatches() {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 1);
        Class<?> checkedMatch = TestEvent.class;
        assertEquals(checkedMatch, bus.getBusType(), "Bus type mismatch. Bus type: " + bus.getBusType() + ", checked against: " + checkedMatch);
        TestableEventBus<Event> basicBus = new EventBus<>(Event.class, 1);
        checkedMatch = Event.class;
        assertEquals(checkedMatch, basicBus.getBusType(), "Bus type mismatch. Bus type: " + basicBus.getBusType() + ", checked against: " + checkedMatch);
    }

    @Test
    void checkBusRegisters() {
        try (MockedStatic<EventBusRegistry> mocked = mockStatic(EventBusRegistry.class)) {
            EventBusRegistry fakeRegistry = mock(EventBusRegistry.class);
            mocked.when(EventBusRegistry::getInstance).thenReturn(fakeRegistry);
            EventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 1);
            bus.register();
            verify(fakeRegistry, times(1)).register(bus);
        }
    }

    @Test
    void checkPostCallsDispatchTable() {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 1);
        EventBus.DispatchTable table = mock(EventBus.DispatchTable.class);

        ParentEvent event = new ParentEvent();
        bus.setDispatchTable(table);
        bus.post(event);
        verify(table, times(1)).dispatch(event);
    }

    // also handled by #checkRecursionExceptionDepth
    @Test
    void postThrowsOnRecursion() {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 1);
        bus.setRecursionDepth(1); // intentionally eq to maxStackDepth
        ParentEvent event = new ParentEvent();
        assertThrows(EventStackOverflowException.class, () -> bus.post(event));
    }

    @Test
    void checkRecursionExceptionOutputs() {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 1);
        bus.setRecursionDepth(1); // intentionally eq to maxStackDepth
        ParentEvent event = new ParentEvent();
        EventStackOverflowException ex = assertThrows(EventStackOverflowException.class, () -> bus.post(event));
        assertEquals(2, ex.getDepth());
        assertEquals(1, ex.getMaxDepth());

        TestableEventBus<TestEvent> bus2 = new EventBus<>(TestEvent.class, 5);
        bus2.setRecursionDepth(6); // intentionally higher than maxStackDepth
        EventStackOverflowException ex2 = assertThrows(EventStackOverflowException.class, () -> bus2.post(event));
        assertEquals(7, ex2.getDepth());
        assertEquals(5, ex2.getMaxDepth());
    }

    @Test
    void depthIsThreadIsolated() throws InterruptedException {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 128);
        bus.setRecursionDepth(50);

        Thread otherThread = new Thread(() -> {
            // ThreadLocal initial value should be 0, not 50
            assertEquals(0, bus.getCurrentRecursionDepth());
            bus.setRecursionDepth(10);
        });

        otherThread.start();
        otherThread.join();

        // Main thread depth must remain unchanged
        assertEquals(50, bus.getCurrentRecursionDepth());
    }

    @Test
    void openBypassThrowsOnNegativeBudget() {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 10);
        assertThrows(IllegalArgumentException.class, () -> bus.openBypassTo(-1));
    }

    @Test
    void openBypassCorrectlyManipulatesDepth() {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 128);
        bus.setRecursionDepth(100);

        try (RecursionBypass ignored = bus.openBypassTo(50)) {
            // expect depth to be set to recursionDepth - extraBudget
            assertEquals(100 - 50, bus.getCurrentRecursionDepth());
        }
        assertEquals(100, bus.getCurrentRecursionDepth());
    }

    @Test
    void openBypassToZeroIsNoOp() {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 128);
        bus.setRecursionDepth(120);

        try (RecursionBypass ignored = bus.openBypassTo(0)) {
            assertEquals(120, bus.getCurrentRecursionDepth());
        }

        assertEquals(120, bus.getCurrentRecursionDepth());
    }

    @Test
    void nestedBypassesAreAdditive() {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 128);
        bus.setRecursionDepth(100);

        try (RecursionBypass ignored = bus.openBypassTo(50)) {
            // 100 - 50 = 50
            assertEquals(50, bus.getCurrentRecursionDepth());

            try (RecursionBypass ignored1 = bus.openBypassTo(30)) {
                // 50 - 30 = 20
                assertEquals(20, bus.getCurrentRecursionDepth());

                try (RecursionBypass ignored2 = bus.openBypassTo(100)) {
                    // 20 - 100 = -80
                    assertEquals(-80, bus.getCurrentRecursionDepth());
                }
                // Back to 20
                assertEquals(20, bus.getCurrentRecursionDepth());
            }
            // Back to 50
            assertEquals(50, bus.getCurrentRecursionDepth());
        }
        // Back to 100
        assertEquals(100, bus.getCurrentRecursionDepth());
    }

    @Test
    void openBypassProtectsAgainstIntegerOverflow() {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 128);
        bus.setRecursionDepth(Integer.MIN_VALUE + 100);

        // Try to subtract a value that would wrap around to positive
        try (RecursionBypass ignored = bus.openBypassTo(200)) {
            assertEquals(Integer.MIN_VALUE, bus.getCurrentRecursionDepth());
        }

        assertEquals(Integer.MIN_VALUE + 100, bus.getCurrentRecursionDepth());
    }

    // todo this kinda bad
    @Test
    void checkRegisterRebuildsTable() {
        EventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 1);
        TestableDispatchTable preTable = bus.getDispatchTable();

        bus.register(ParentEvent.class, Priority.NORMAL, e -> {});
        TestableDispatchTable postTable = bus.getDispatchTable();
        assertNotEquals(preTable, postTable, "dispatchTable not rebuilt");
    }

    // todo this kinda bad
    @Test
    void checkUnsubscribeRebuildsTable() {
        EventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 1);
        Subscription sub = bus.register(ParentEvent.class, Priority.NORMAL, e -> {});
        TestableDispatchTable preTable = bus.getDispatchTable();

        sub.unsubscribe();
        TestableDispatchTable postTable = bus.getDispatchTable();
        assertNotEquals(preTable, postTable, "dispatchTable not rebuilt");
    }

    @Test
    void checkRegisterInjectsSubscriptionIntoLinkableProcessor() {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 10);

        // Create a mock that implements both interfaces
        abstract class LinkableProcessor implements EventProcessor<ParentEvent>, Linkable {}
        LinkableProcessor mockProcessor = mock(LinkableProcessor.class);

        Subscription sub = bus.register(ParentEvent.class, mockProcessor);
        verify(mockProcessor, times(1)).setSubscription(sub);
    }

    @Test
    void rebuildCorrectlyMapsSkippedParents() {
        EventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 10);

        // Parent (Registered) -> Child (Skipped) -> Grandchild (Registered)
        bus.register(ParentEvent.class, Priority.NORMAL, e -> {});
        bus.register(GrandchildEvent.class, Priority.NORMAL, e -> {});

        TestableDispatchTable table = bus.getDispatchTable();

        // Find the idx of Grandchild in the flat types array
        int grandchildIdx = -1;
        Class<?>[] flatTypes = table.getFlatTypes();
        for (int i = 0; i < flatTypes.length; i++) {
            if (flatTypes[i] == GrandchildEvent.class) {
                grandchildIdx = i;
                break;
            }
        }

        // Verify Grandchild's parentBitIndex points to ParentEvent's bit, NOT -1
        int parentBit = table.getParentBitIndices()[grandchildIdx];
        assertNotEquals(-1, parentBit, "Grandchild should have a linked parent bit index.");

        // Verify that the bit index at that location corresponds to ParentEvent
        int parentEventBitIndex = -1;
        int[] selfBitIndicies = table.getSelfBitIndices();
        for (int i = 0; i < flatTypes.length; i++) {
            if (flatTypes[i] == ParentEvent.class) {
                parentEventBitIndex = selfBitIndicies[i];
                break;
            }
        }
        assertEquals(parentEventBitIndex, parentBit, "Grandchild parent bit must point to registered ParentEvent.");
    }

    private static int findFamilyIdx(TestableDispatchTable table, Priority priority, Class<?> type) {
        int[] offsets = table.getSegmentOffsets();
        int[] lengths = table.getSegmentLengths();
        Class<?>[] types = table.getFlatTypes();

        // Find stride; num of event families
        int familyStride = offsets.length / Priority.values().length;

        int familyIdx = -1;
        for (int f = 0; f < familyStride; f++) {
            int segmentStart = (priority.ordinal() * familyStride) + f;
            int offset = offsets[segmentStart];
            int length = lengths[segmentStart];

            // Look at the types in this segment
            for (int i = offset; i < offset + length; i++) {
                if (type.equals(types[i])) {
                    familyIdx = f;
                    break;
                }
            }
        }
        return familyIdx;
    }

    @Test
    void checkSegmentMetadataCalculations() {
        EventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 10);

        // FAM1: Parent -> Child
        bus.register(ParentEvent.class, Priority.HIGHEST, e -> {});
        bus.register(ChildEvent.class, Priority.HIGHEST, e -> {});

        // FAM2: Other unrelated event; starts new family
        bus.register(OtherEvent.class, Priority.NORMAL, e -> {});

        TestableDispatchTable table = bus.getDispatchTable();
        int[] offsets = table.getSegmentOffsets();
        int[] lengths = table.getSegmentLengths();

        // Verify that segments are laid out sequentially without overlapping
        for (int i = 1; i < offsets.length; i++) {
            assertTrue(offsets[i] >= offsets[i-1] + lengths[i-1],
                    "Segment " + i + " starts before Segment " + (i-1) + " ended!");
        }

        // Find stride; num of event families
        int familyStride = offsets.length / Priority.values().length;
        assertEquals(2, familyStride, "Expected 2 distinct event families in the table.");

        // index is JVM dependent as it's ordered by HashMap.
        int parentFamilyIdx = findFamilyIdx(table, Priority.HIGHEST, ParentEvent.class);
        int otherFamilyIdx = findFamilyIdx(table, Priority.NORMAL, OtherEvent.class);

        assertNotEquals(-1, parentFamilyIdx, "Parent family index not found");
        assertNotEquals(-1, otherFamilyIdx, "Other family index not found");

        assertTrue((parentFamilyIdx == 0 && otherFamilyIdx == 1) || (parentFamilyIdx == 1 && otherFamilyIdx == 0),
                "Families should occupy distinct slots (0 and 1)");


        // Validate specific segment sizes
        // [Priority Index * Stride + Family Index]
        int highestFam1 = (Priority.HIGHEST.ordinal() * familyStride) + parentFamilyIdx;
        assertEquals(2, lengths[highestFam1], "HIGHEST/FAM1 segment should have 2 handlers");

        int normalFam2 = (Priority.NORMAL.ordinal() * familyStride) + otherFamilyIdx;
        assertEquals(1, lengths[normalFam2], "NORMAL/FAM2 segment should have 1 handler");
    }

    @Test
    void flatArrayOrderFollowsPriorityThenHierarchy() {
        EventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 10);

        // Register out of order
        bus.register(ParentEvent.class, Priority.HIGH, e -> {});
        bus.register(ChildEvent.class, Priority.LOW, e -> {});

        TestableDispatchTable table = bus.getDispatchTable();
        Class<?>[] types = table.getFlatTypes();

        // low priority must come before high priority in the flat array
        int highIdx = -1;
        int lowIdx = -1;

        for (int i = 0; i < types.length; i++) {
            if (types[i] == ParentEvent.class) highIdx = i;
            if (types[i] == ChildEvent.class) lowIdx = i;
        }

        assertTrue(lowIdx < highIdx, "LOW priority handlers must appear before HIGH priority handlers in flat array.");
    }

    @Test
    void bitsetCapacityCorrectlyCalculated() {
        EventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 10);
        ByteBuddy byteBuddy = new ByteBuddy();

        // Test boundary logic: 64 types = 1 long, 65 types = 2 longs
        for (int i = 0; i < 64; i++) {
            // Generate a unique subclass of TestEvent on the fly
            Class<? extends TestEvent> uniqueSubclass = byteBuddy
                    .subclass(TestEvent.class)
                    .name(this.getClass().getPackageName() + ".GeneratedEvent" + i) // Unique name
                    .make()
                    .load(TestEvent.class.getClassLoader(), ClassLoadingStrategy.Default.WRAPPER)
                    .getLoaded();

            bus.register(uniqueSubclass, e -> {});
        }
        assertEquals(1, bus.getDispatchTable().getBitSetSize(), "64 types should still fit in 1 long");

        bus.register(ParentEvent.class, e -> {});
        assertEquals(2, bus.getDispatchTable().getBitSetSize(), "65 types should require 2 longs");
    }

    @Test
    void siblingsAreSplitIntoDistinctFamilies() {
        EventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 10);
        // hierarchy: BranchRoot -> BranchA, BranchRoot -> BranchB
        bus.register(BranchRootEvent.class, Priority.NORMAL, e -> {});
        bus.register(BranchAEvent.class, Priority.NORMAL, e -> {});
        bus.register(BranchBEvent.class, Priority.NORMAL, e -> {});

        TestableDispatchTable table = bus.getDispatchTable();
        int idxA = findFamilyIdx(table, Priority.NORMAL, BranchAEvent.class);
        int idxB = findFamilyIdx(table, Priority.NORMAL, BranchBEvent.class);

        assertNotEquals(idxA, idxB, "Siblings must occupy different family slots to prevent skip collisions.");
    }

    @Test
    void unregisterPurgesSpecificHandlerInstance() {
        TestableEventBus<TestEvent> bus = new EventBus<>(TestEvent.class, 10);

        // todo if there are two of the same listeners it will remove both.
        // We use two different handlers for the same event type to prove
        // that unregistering one doesn't kill the other.
        EventProcessor<ParentEvent> handlerA = e -> {};
        EventProcessor<ParentEvent> handlerB = e -> {};

        Subscription subA = bus.register(ParentEvent.class, Priority.NORMAL, handlerA);
        bus.register(ParentEvent.class, Priority.NORMAL, handlerB);

        // Act: Remove only Handler A
        subA.unsubscribe();

        // The new table should contain B but NOT A
        TestableDispatchTable table = bus.getDispatchTable();
        EventProcessor<?>[] handlers = table.getFlatEventProcessors();

        boolean foundA = false;
        boolean foundB = false;

        for (EventProcessor<?> h : handlers) {
            if (h == handlerA) foundA = true;
            if (h == handlerB) foundB = true;
        }

        assertFalse(foundA, "Handler A should have been removed from the handler array.");
        assertTrue(foundB, "Handler B should still be present in the handler array.");
    }
}
