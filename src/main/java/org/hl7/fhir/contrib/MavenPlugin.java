package org.hl7.fhir.contrib;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

// https://maven.apache.org/guides/plugin/guide-java-plugin-development.html

@Mojo(name = "sayhi", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class MavenPlugin extends AbstractMojo {
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Hello, world.");
    }
}
