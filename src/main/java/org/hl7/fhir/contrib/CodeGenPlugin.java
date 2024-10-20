package org.hl7.fhir.contrib;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class CodeGenPlugin extends AbstractMojo {

    public static final String PLUGIN_NAME = "fhir-codegen-maven-plugin";

    @Parameter(property = "generate.packageId", required = true)
    private String packageId;

    @Parameter(property = "generate.outputFolder", defaultValue = "target/generated-sources/java" )
    private String outputFolder = "target/generated-sources/java";

    @Parameter(property = "generate.packageName", defaultValue = "org.hl7.fhir.example.generated" )
    private String packageName = "org.hl7.fhir.example.generated";

    @Parameter(property = "generate.profiles")
    public void setProfiles(String[] profiles) {

        this.profiles = Arrays.stream(profiles).collect(Collectors.toUnmodifiableSet());
    }
    private Set<String> profiles;

    @Override
    public void execute() throws MojoExecutionException {
        try {
            CodeGeneratorFactory.PECodeGenerator generator = new CodeGeneratorFactory(packageId, outputFolder, packageName, profiles).produceCodeGenerator();
            generator.generate();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to generate code", e);
        }
    }

}
