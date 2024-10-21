import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.hl7.fhir.contrib.CodeGenPlugin;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

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

    public void testDefaultMojoGoal() throws Exception {
        run(new File(getBasedir(), "src/test/resources/default.pom.xml"));
    }

    public void testProfilesMojoGoal() throws Exception {
        run(new File(getBasedir(), "src/test/resources/profiles.pom.xml"));
    }

    public void testFullurlMojoGoal() throws Exception {
        run(new File(getBasedir(), "src/test/resources/fullurl.pom.xml"));
    }

    private void run(File pomFile) throws Exception {
        ((CodeGenPlugin) lookupMojo("generate", pomFile)).execute();
    }
}
