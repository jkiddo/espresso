import ca.uhn.fhir.jpa.packages.loader.PackageLoaderSvc;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.hl7.fhir.contrib.CodeGenPlugin;

import org.hl7.fhir.contrib.ContextBuilder;

import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;

import java.io.*;
import java.nio.file.Path;

public class PluginTest extends AbstractMojoTestCase {


    /**
     * @see junit.framework.TestCase#setUp()
     */

    protected void setUp() throws Exception {
        // required for mojo lookups to work
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        FileUtils.deleteDirectory(Path.of(getBasedir(), "target/generated-sources/java").toFile());
    }

    /**
     * @throws Exception
     */

    public void testDefaultR4MojoGoal() throws Exception {
        run(new File(getBasedir(), "src/test/resources/default.r4.pom.xml"));

        //var dkCorePatient = new org.hl7.fhir.example.generated.DkCorePatient().setCpr(new org.hl7.fhir.example.generated.DkCoreCprIdentifier().setValue(UUID.randomUUID().toString()));
        //var patient = dkCorePatient.build(createWorkerContextR4Example());


    }

    public void testProfilesR4MojoGoal() throws Exception {
        run(new File(getBasedir(), "src/test/resources/profiles.r4.pom.xml"));
    }

    public void testFullurlR4MojoGoal() throws Exception {
        run(new File(getBasedir(), "src/test/resources/fullurl.r4.pom.xml"));
    }

    public void testFullurlR5MojoGoal() throws Exception {
        run(new File(getBasedir(), "src/test/resources/fullurl.r5.pom.xml"));

        //var indication = new ClinicalUseDefinitionIndicationUvEpi().build(createWorkerContextR5Example());
    }

    private void run(File pomFile) throws Exception {
        ((CodeGenPlugin) lookupMojo("generate", pomFile)).execute();
    }

    private org.hl7.fhir.r4.context.SimpleWorkerContext createWorkerContextR4Example() throws IOException {
        return ContextBuilder.usingR4(new FilesystemPackageCacheManager.Builder().build().loadPackage("hl7.fhir.dk.core", "3.2.0")).build();
    }

    private org.hl7.fhir.r5.context.SimpleWorkerContext createWorkerContextR5Example() throws IOException {
        return ContextBuilder.usingR5(NpmPackage.fromPackage(new ByteArrayInputStream(new PackageLoaderSvc().loadPackageUrlContents("https://hl7.org/fhir/uv/emedicinal-product-info/STU1/package.tgz")))).build();
    }
}
