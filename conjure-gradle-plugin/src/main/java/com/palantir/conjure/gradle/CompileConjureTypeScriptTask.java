/*
 * (c) Copyright 2016 Palantir Technologies Inc. All rights reserved.
 */

package com.palantir.conjure.gradle;

import static com.google.common.base.Preconditions.checkState;

import com.palantir.conjure.defs.Conjure;
import com.palantir.conjure.defs.ConjureDefinition;
import com.palantir.conjure.gen.typescript.ConjureTypeScriptClientGenerator;
import com.palantir.conjure.gen.typescript.ExperimentalFeatures;
import com.palantir.conjure.gen.typescript.errors.ErrorGenerator;
import com.palantir.conjure.gen.typescript.services.ServiceGenerator;
import com.palantir.conjure.gen.typescript.types.TypeGenerator;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.SourceTask;
import org.gradle.api.tasks.TaskAction;
import org.gradle.util.GFileUtils;

public class CompileConjureTypeScriptTask extends SourceTask {

    private File outputDirectory;
    private File nodeModulesOutputDirectory;
    private ServiceGenerator serviceGenerator;
    private TypeGenerator typeGenerator;
    private ErrorGenerator errorGenerator;

    private Supplier<Set<ExperimentalFeatures>> experimentalFeatures;

    public final void setErrorGenerator(ErrorGenerator errorGenerator) {
        this.errorGenerator = errorGenerator;
    }

    public final void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    @OutputDirectory
    public final File getOutputDirectory() {
        return outputDirectory;
    }

    public final void setNodeModulesOutputDirectory(File nodeModulesOutputDirectory) {
        this.nodeModulesOutputDirectory = nodeModulesOutputDirectory;
    }

    @OutputDirectory
    public final File getNodeModulesOutputDirectory() {
        return nodeModulesOutputDirectory;
    }

    public final void setServiceGenerator(ServiceGenerator serviceGenerator) {
        this.serviceGenerator = serviceGenerator;
    }

    public final void setTypeGenerator(TypeGenerator typeGenerator) {
        this.typeGenerator = typeGenerator;
    }

    public final void setExperimentalFeatures(Supplier<Set<ExperimentalFeatures>> experimentalFeatures) {
        this.experimentalFeatures = experimentalFeatures;
    }

    @Input
    public final Set<ExperimentalFeatures> getExperimentalFeatures() {
        return experimentalFeatures.get();
    }

    @TaskAction
    public final void compileFiles() throws IOException {
        checkState(outputDirectory.exists() || outputDirectory.mkdirs(),
                "Unable to make directory tree %s", outputDirectory);
        checkState(nodeModulesOutputDirectory.exists() || nodeModulesOutputDirectory.mkdirs(),
                "Unable to make directory tree %s", nodeModulesOutputDirectory);
        GFileUtils.cleanDirectory(outputDirectory);

        compileFiles(ConjurePlugin.excludeExternalImports(getSource().getFiles()), outputDirectory);

        // make all generated code available for later compilation
        compileFiles(getSource().getFiles(), nodeModulesOutputDirectory);
    }

    private void compileFiles(Collection<File> files, File outputDir) {
        ConjureTypeScriptClientGenerator generator = new ConjureTypeScriptClientGenerator(
                serviceGenerator, typeGenerator, errorGenerator, experimentalFeatures.get());

        List<ConjureDefinition> conjureDefinitions = files.stream().map(Conjure::parse).collect(Collectors.toList());
        generator.emit(conjureDefinitions, getProject().getVersion().toString(), outputDir);
    }
}
