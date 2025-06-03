import ca.uhn.fhir.jpa.packages.loader.PackageLoaderSvc;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.hl7.fhir.contrib.CodeGenPlugin;

import org.hl7.fhir.contrib.ContextBuilder;

import org.hl7.fhir.example.generated.EhealthCommunication;
import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;

import java.io.*;

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
        //FileUtils.deleteDirectory(Path.of(getBasedir(), "target/generated-sources/java").toFile());
    }

    /**
     * @throws Exception
     */

    public void testDefaultR4MojoGoal() throws Exception {
        run(new File(getBasedir(), "src/test/resources/fut.r4.pom.xml"));

        //var dkCorePatient = new org.hl7.fhir.example.generated.DkCorePatient().setCpr(new org.hl7.fhir.example.generated.DkCoreCprIdentifier().setValue(UUID.randomUUID().toString()));
        //var patient = dkCorePatient.build(createWorkerContextR4Example());
    }

    public void testProfilesR4MojoGoal() throws Exception {
        run(new File(getBasedir(), "src/test/resources/profiles.r4.pom.xml"));
    }

    public void testFullurlR4MojoGoal() throws Exception {
        run(new File(getBasedir(), "src/test/resources/fullurl.r4.pom.xml"));
    }

    public void testFutUrlR4MojoGoal() throws Exception {
        //run(new File(getBasedir(), "src/test/resources/fut.r4.pom.xml"));
        SimpleWorkerContext workerContext = createWorkerContextR4Example();
        Patient p = new Patient();
        p.setId("1");
        p.setActive(true);

        EhealthCommunication customModel = new EhealthCommunication(workerContext).setEpisodeOfCare(new Reference(p)).setId("23").setStatus("stopped");
        Communication convertedOrigin = customModel.build();
        EhealthCommunication convertedCustomModel = EhealthCommunication.fromSource(workerContext, convertedOrigin);


        assertEquals(customModel.getId(), convertedCustomModel.getId());
    }

    public void testFullurlR5MojoGoal() throws Exception {
        run(new File(getBasedir(), "src/test/resources/fullurl.r5.pom.xml"));

        //var indication = new ClinicalUseDefinitionIndicationUvEpi().build(createWorkerContextR5Example());
    }

    private void run(File pomFile) throws Exception {
        ((CodeGenPlugin) lookupMojo("generate", pomFile)).execute();
    }

    private org.hl7.fhir.r4.context.SimpleWorkerContext createWorkerContextR4Example() throws IOException {
        return ContextBuilder.usingR4(new FilesystemPackageCacheManager.Builder().build().loadPackage("dk.ehealth.sundhed.fhir.ig.core", "3.4.0")).build();
    }

    private org.hl7.fhir.r5.context.SimpleWorkerContext createWorkerContextR5Example() throws IOException {
        return ContextBuilder.usingR5(NpmPackage.fromPackage(new ByteArrayInputStream(new PackageLoaderSvc().loadPackageUrlContents("https://hl7.org/fhir/uv/emedicinal-product-info/STU1/package.tgz")))).build();
    }
}
