package com.github.rcubedev.utils.event.impl.apt.generator;

import java.io.BufferedWriter;

@FunctionalInterface
public interface BufferedWriterConsumer {
    void accept(BufferedWriter writer) throws Exception;
}