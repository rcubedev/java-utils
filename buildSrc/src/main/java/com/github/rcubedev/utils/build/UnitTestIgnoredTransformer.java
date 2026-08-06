package com.github.rcubedev.utils.build;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

public class UnitTestIgnoredTransformer extends ClassTransformer {

    public static final String UNIT_TEST_IGNORED = "Lcom/github/rcubedev/utils/test/UnitTestIgnored;";
    public static final String GENERATED         = "Lcom/github/rcubedev/utils/test/Generated;";

    @Override
    public byte[] transform(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(cr, 0);

        GeneratedAnnotationClassVisitor visitor = new GeneratedAnnotationClassVisitor(cw);
        cr.accept(visitor, 0);

        return visitor.isModified() ? cw.toByteArray() : classBytes;
    }
}