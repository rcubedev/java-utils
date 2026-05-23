package com.github.rcubedev.example.build;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import static com.github.rcubedev.example.build.UnitTestIgnoredTransformer.GENERATED;
import static com.github.rcubedev.example.build.UnitTestIgnoredTransformer.UNIT_TEST_IGNORED;

public class GeneratedAnnotationFieldVisitor extends FieldVisitor {

    private boolean hasIgnored   = false;
    private boolean hasGenerated = false;
    private boolean modified     = false;

    public GeneratedAnnotationFieldVisitor(FieldVisitor fv) {
        super(Opcodes.ASM9, fv);
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
    public void visitEnd() {
        if (hasIgnored && !hasGenerated) {
            fv.visitAnnotation(GENERATED, false).visitEnd();
            modified = true;
        }
        super.visitEnd();
    }
}