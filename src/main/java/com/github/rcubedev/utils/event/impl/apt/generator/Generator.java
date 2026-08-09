package com.github.rcubedev.utils.event.impl.apt.generator;

import org.jetbrains.annotations.Nullable;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.BufferedWriter;
import java.io.Writer;

public abstract class Generator {

    private final ProcessingEnvironment processingEnv;

    protected Generator(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public ProcessingEnvironment getProcessingEnv() {
        return this.processingEnv;
    }

    protected boolean writeSourceFile(String fqcn, Element originatingElement, BufferedWriterConsumer consumer) {
        try {
            JavaFileObject file = this.processingEnv.getFiler().createSourceFile(fqcn, originatingElement);
            try (BufferedWriter writer = new BufferedWriter(file.openWriter())) {
                consumer.accept(writer);
            }
            return true;
        } catch (Exception e) {
            throwError("Failed to generate source file [" + fqcn + "]: " + e.getMessage(), originatingElement);
            return false;
        }
    }

    protected boolean writeResourceFile(StandardLocation location, String relativePath, BufferedWriterConsumer consumer) {
        try {
            FileObject resource = this.processingEnv.getFiler().createResource(location, "", relativePath);
            try (BufferedWriter writer = new BufferedWriter(resource.openWriter())) {
                consumer.accept(writer);
            }
            return true;
        } catch (Exception e) {
            throwError("Failed to write resource file [" + relativePath + "]: " + e.getMessage());
            return false;
        }
    }

    public <T> @Nullable T throwError(String error) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error);
        return null;
    }

    public <T> @Nullable T throwError(Exception ex) {
        return throwError(ex.getMessage());
    }


    public <T> @Nullable T throwError(String error, Element e) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error, e);
        return null;
    }

    public <T> @Nullable T throwError(Exception ex, Element e) {
        return throwError(ex.getMessage(), e);
    }

    public <T> @Nullable T throwError(String error, Element e, AnnotationMirror am) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error, e, am);
        return null;
    }

    public <T> @Nullable T throwError(Exception ex, Element e, AnnotationMirror am) {
        return throwError(ex.getMessage(), e, am);
    }
}
