package com.github.rcubedev.utils.event.impl.apt.generator;

import com.github.rcubedev.utils.event.generated.SubscriberInvokerFactory;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.StandardLocation;
import java.util.Set;

public final class ServiceWriter extends Generator {

    public ServiceWriter(ProcessingEnvironment processingEnv) {
        super(processingEnv);
    }

    // always write for named modules as fallback if not run in modularity
    public boolean writeService(Set<String> generatedFactories) {
        if (generatedFactories.isEmpty()) return true;
        String servicePath = "META-INF/services/" + SubscriberInvokerFactory.class.getCanonicalName();

        return writeResourceFile(StandardLocation.CLASS_OUTPUT, servicePath, writer -> {
            for (String factoryFqcn : generatedFactories) {
                writer.write(factoryFqcn);
                writer.newLine();
            }
        });
    }
}