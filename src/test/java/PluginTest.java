import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.hl7.fhir.contrib.CodeGenPlugin;

import java.io.File;

public class PluginTest extends AbstractMojoTestCase {


    /**
     * @see junit.framework.TestCase#setUp()
     */

    protected void setUp() throws Exception
    {
        // required for mojo lookups to work
        super.setUp();
    }

    /**
     * @throws Exception
     */

    public void testMojoGoal() throws Exception
    {
        File testPom = new File( getBasedir(),
                "src/test/resources/pom.xml" );

        CodeGenPlugin mojo = (CodeGenPlugin) lookupMojo( "generate", testPom );

        assertNotNull( mojo );

        mojo.execute();
    }
}
