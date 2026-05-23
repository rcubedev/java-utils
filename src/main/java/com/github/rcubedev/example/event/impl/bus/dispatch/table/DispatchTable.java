package com.github.rcubedev.example.event.impl.bus.dispatch.table;

import com.github.rcubedev.example.event.api.Event;
import com.github.rcubedev.example.event.api.EventProcessor;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Immutable snapshot of the flat dispatch array and family metadata.
 * <p>
 * Built at registration time, read locklessly at dispatch.
 */
public final class DispatchTable<E extends Event> {

    /**
     * Flat array of merged invokers. One per (eventType, priority) handler.
     * <p>
     * Sorted: priority-first, superclass -> subclass within each priority block per family.
     */
    private final EventProcessor<? extends E>[] flat;

    /**
     * Parallel to {@link #flat}; the event type each processor belongs to.
     * <p>
     * Used for per-element {@code isInstance} check at dispatch.<br>
     * When {@link Class#isInstance} returns {@code false}, skip to the next family.
     */
    private final Class<? extends E>[] flatTypes;

    /**
     * Start index for segment (priority, family), stored at priority * numFamilies + family.
     */
    private final int[] segmentOffsets;

    /**
     * Entry count for segment (priority, family), stored at priority * numFamilies + family.
     */
    private final int[] segmentLengths;

    // BitSet metadata
    /**
     * Bit index of the registered parent for the type at this index.
     * <p>
     * Used to perform skips across different families.
     */
    private final int[] parentBitIndices;

    /**
     * Unique bit index for the type at this index.
     * <p>
     * Used to mark success in the {@code passBits} bitset.
     */
    private final int[] selfBitIndices;

    /**
     * Number of long slots required to represent all unique registered types.
     */
    private final int bitSetSize; // (numUniqueTypes + 63) / 64 <-- int ceil

    public DispatchTable(EventProcessor<? extends E>[] flat, Class<? extends E>[] flatTypes,
                  int[] segmentOffsets, int[] segmentLengths,
                  int[] parentBitIndices, int[] selfBitIndices, int bitSetSize) {
        this.flat = flat;
        this.flatTypes = flatTypes;
        this.segmentOffsets = segmentOffsets;
        this.segmentLengths = segmentLengths;
        this.parentBitIndices = parentBitIndices;
        this.selfBitIndices = selfBitIndices;
        this.bitSetSize = bitSetSize;
    }

    @SuppressWarnings("unchecked")
    public static <T extends Event> @NotNull DispatchTable<T> empty() {
        return (DispatchTable<T>) EmptyHolder.EMPTY;
    }

    /**
     * Dispatches the given event to all compatible processors in the table.
     * <p>
     * This method uses a dual-skip strategy to optimise event processing.
     * <ul>
     *   <li><b>Horizontal Skip:</b> Uses a bitset to skip entire branches when a common parent has failed elsewhere.</li>
     *   <li><b>Vertical Skip:</b> Uses the linear family structure to skip children if a parent fails in-place.</li>
     * </ul>
     *
     * @param event The event to post.
     */
    public void dispatch(@NotNull E event) {
        if (flat.length == 0) return;
        long[] passBits = new long[bitSetSize];

        for (int s = 0; s < segmentOffsets.length; s++) {
            int start = segmentOffsets[s];
            int end = start + segmentLengths[s];

            for (int i = start; i < end; i++) {
                // if parent didn't pass isInstance check (in any family), skip this one.
                final int pIdx = parentBitIndices[i];
                if (pIdx != -1 && (passBits[pIdx >> 6] & (1L << (pIdx & 63))) == 0) break;

                if (!flatTypes[i].isInstance(event)) break; // not an instance; skip rest of family

                // mark success
                final int selfIdx = selfBitIndices[i];
                passBits[selfIdx >> 6] |= (1L << (selfIdx & 63));

                // fire listeners
                @SuppressWarnings("unchecked") // safe as type is known to be a supertype of E or match exactly.
                EventProcessor<? super E> processor = ((EventProcessor<? super E>) flat[i]);
                processor.process(event);
            }
        }
    }

    private static class EmptyHolder {
        @SuppressWarnings("unchecked")
        private static final DispatchTable<?> EMPTY =
                new DispatchTable<>(new EventProcessor<?>[0], (Class<? extends Event>[]) new Class<?>[0], new int[0], new int[0], new int[0], new int[0], 0);
    }
}

