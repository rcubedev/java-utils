package com.github.rcubedev.utils.event.impl.subscriber.scanner;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;

public record DiscoveredMethod(@Nullable Object instance, Method method) {}
