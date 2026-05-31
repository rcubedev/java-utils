package com.github.rcubedev.example.build;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Files;

public abstract class PatchClasses extends DefaultTask {

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract ConfigurableFileCollection getSourceDirs();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Input
    public abstract Property<ClassTransformer> getTransformer();

    @TaskAction
    public void patch() {
        File outputDir = getOutputDir().get().getAsFile();

        getFileSystemOperations().delete(spec -> spec.delete(outputDir));

        ClassTransformer transformer = getTransformer().get();

        for (File dir : getSourceDirs()) {
            if (!dir.exists()) continue;

            getObjectFactory().fileTree().from(dir).matching(p -> p.include("**/*.class")).forEach(classFile -> {
                String relative = dir.toPath().relativize(classFile.toPath()).toString();
                File outputFile = new File(outputDir, relative);

                byte[] original = readBytes(classFile);
                byte[] transformed = transformer.transform(original);
                writeBytes(outputFile, transformed);

                if (transformed != original) getLogger().lifecycle("Patched class: {}", classFile.getName());
            });
        }
    }

    private byte[] readBytes(File f) {
        try {
            return Files.readAllBytes(f.toPath());
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to read class file: " + f.getAbsolutePath(), e);
        }
    }

    private void writeBytes(File f, byte[] bytes) {
        try {
            Files.createDirectories(f.toPath().getParent());
            Files.write(f.toPath(), bytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write patched class file: " + f.getAbsolutePath(), e);
        }
    }
}