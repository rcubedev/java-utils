package com.github.rcubedev.utils.event.impl.apt.validation;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;

public abstract class Validator {

    private final ProcessingEnvironment processingEnv;

    protected Validator(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    public ProcessingEnvironment getProcessingEnv() {
        return this.processingEnv;
    }

    public boolean throwError(String error, Element e) {
        this.processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, error, e);
        return false;
    }

    public boolean throwError(Exception ex, Element e) {
        return throwError(ex.getMessage(), e);
    }
}
