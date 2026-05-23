package com.github.rcubedev.example.build;

import org.objectweb.asm.*;

import static com.github.rcubedev.example.build.UnitTestIgnoredTransformer.GENERATED;
import static com.github.rcubedev.example.build.UnitTestIgnoredTransformer.UNIT_TEST_IGNORED;

public class GeneratedAnnotationClassVisitor extends ClassVisitor {

    private boolean classNeedsGenerated = false;
    private boolean modified            = false;

    public GeneratedAnnotationClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    public boolean isModified() {
        return modified;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        if (desc.equals(UNIT_TEST_IGNORED)) classNeedsGenerated = true;
        return super.visitAnnotation(desc, visible);
    }

    @Override
    public void visitEnd() {
        if (classNeedsGenerated) {
            cv.visitAnnotation(GENERATED, false).visitEnd();
            modified = true;
        }
        super.visitEnd();
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor,
                                   String signature, Object value) {
        GeneratedAnnotationFieldVisitor fv = new GeneratedAnnotationFieldVisitor(
                super.visitField(access, name, descriptor, signature, value)) {
            @Override
            public void visitEnd() {
                super.visitEnd();
                if (isModified()) modified = true;
            }
        };
        return fv;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
                                     String signature, String[] exceptions) {
        return new GeneratedAnnotationMethodVisitor(
                super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public void visitEnd() {
                super.visitEnd();
                if (isModified()) modified = true;
            }
        };
    }
}