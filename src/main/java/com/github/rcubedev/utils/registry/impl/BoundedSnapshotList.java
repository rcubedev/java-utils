package com.github.rcubedev.utils.registry.impl;

import java.util.*;

public final class BoundedSnapshotList<E> extends AbstractList<E> implements RandomAccess {

    private final Map<Integer, ? extends E> map;
    private final int size;

    public BoundedSnapshotList(Map<Integer, ? extends E> map, int size) {
        this.map = map;
        this.size = size;
    }

    @Override
    public E get(int index) {
        Objects.checkIndex(index, size);
        return map.get(index);
    }

    @Override
    public int size() {
        return size;
    }
}