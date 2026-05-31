package com.github.rcubedev.example.event.test;

import com.github.rcubedev.example.event.api.EventProcessor;
import org.jetbrains.annotations.NotNull;

@Deprecated
public interface TestableDispatchTable {

    @NotNull EventProcessor<?>[] getFlatEventProcessors();
    @NotNull Class<?>[] getFlatTypes();
    int @NotNull [] getSegmentOffsets();
    int @NotNull [] getSegmentLengths();
    int @NotNull [] getParentBitIndices();
    int @NotNull [] getSelfBitIndices();
    int getBitSetSize();
}
