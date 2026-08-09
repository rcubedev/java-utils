package com.github.rcubedev.utils.event.impl.apt;

import com.github.rcubedev.utils.event.api.annotation.CompiledEventHandlers;
import com.github.rcubedev.utils.event.impl.apt.generator.InvokerSourceGenerator;
import com.github.rcubedev.utils.event.impl.apt.generator.ServiceWriter;
import com.github.rcubedev.utils.event.impl.apt.scanner.DiscoveredListener;
import com.github.rcubedev.utils.event.impl.apt.scanner.ProcessorScanner;
import com.github.rcubedev.utils.event.impl.apt.validation.ClassValidator;
import com.github.rcubedev.utils.event.impl.apt.validation.MethodValidator;
import com.github.rcubedev.utils.event.impl.apt.validation.module.ModuleValidator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;
import java.util.*;

public final class SubscribeProcessor extends AbstractProcessor {

    private final Set<String> generatedFactories = new LinkedHashSet<>();
    private InvokerSourceGenerator generator;
    private ProcessorScanner scanner;
    private ServiceWriter serviceWriter;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        ClassValidator classValidator = new ClassValidator(processingEnv);
        MethodValidator methodValidator = new MethodValidator(processingEnv);
        ModuleValidator moduleValidator = new ModuleValidator(processingEnv);

        this.generator = new InvokerSourceGenerator(processingEnv, moduleValidator);
        this.scanner = new ProcessorScanner(processingEnv, classValidator, methodValidator);
        this.serviceWriter = new ServiceWriter(processingEnv);
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(CompiledEventHandlers.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.RELEASE_21;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            this.serviceWriter.writeService(this.generatedFactories);
            return true;
        }

        for (TypeElement typeElement : annotations) {
            Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(typeElement);
            for (Element element : elements) {
                if (element instanceof TypeElement type) {
                    DiscoveredListener listener = this.scanner.scanContainer(type);
                    if (listener == null) continue; // keep going to try report as many errors

                    this.generator.generate(listener).ifPresent(this.generatedFactories::add);
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@CompiledEventHandlers can only be applied to class declarations.", element);
                }
            }
        }
        return true;
    }
}
