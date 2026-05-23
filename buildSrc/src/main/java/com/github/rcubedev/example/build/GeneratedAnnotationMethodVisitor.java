package com.github.rcubedev.example.build;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.HashSet;
import java.util.Set;

import static com.github.rcubedev.example.build.UnitTestIgnoredTransformer.GENERATED;
import static com.github.rcubedev.example.build.UnitTestIgnoredTransformer.UNIT_TEST_IGNORED;

public class GeneratedAnnotationMethodVisitor extends MethodVisitor {

    private boolean hasIgnored   = false;
    private boolean hasGenerated = false;
    private boolean modified     = false;
    private final Set<Integer> ignoredParams   = new HashSet<>();
    private final Set<Integer> generatedParams = new HashSet<>();

    public GeneratedAnnotationMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM9, mv);
    }

    public boolean isModified() {
        return modified;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(UNIT_TEST_IGNORED)) hasIgnored   = true;
        if (desc.equals(GENERATED))         hasGenerated = true;
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        if (desc.equals(UNIT_TEST_IGNORED)) ignoredParams.add(parameter);
        if (desc.equals(GENERATED))         generatedParams.add(parameter);
        return super.visitParameterAnnotation(parameter, desc, visible);
    }

    @Override
    public void visitEnd() {
        if (hasIgnored && !hasGenerated) {
            mv.visitAnnotation(GENERATED, false).visitEnd();
            modified = true;
        }
        ignoredParams.forEach(param -> {
            if (!generatedParams.contains(param)) {
                mv.visitParameterAnnotation(param, GENERATED, false).visitEnd();
                modified = true;
            }
        });
        super.visitEnd();
    }
}