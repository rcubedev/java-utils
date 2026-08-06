package com.github.rcubedev.utils.build;

import java.io.Serial;
import java.io.Serializable;

public abstract class ClassTransformer implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Transform the provided class. If no transformations are applied,
     * return the input {@code classBytes}.
     * @param classBytes the class
     * @return the transformed class, or the input if no transformations were applied.
     */
    public abstract byte[] transform(byte[] classBytes);
}