package com.github.rcubedev.utils.registry.impl;

public record EntryHolder<K, V>(int id, K key, V value) {}
