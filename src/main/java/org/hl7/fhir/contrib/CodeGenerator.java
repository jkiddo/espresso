package org.hl7.fhir.contrib;

import ca.uhn.fhir.context.FhirVersionEnum;


import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.profilemodel.gen.PECodeGenerator;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

public class CodeGenerator {

    private static Logger logger = LoggerFactory.getLogger(CodeGenerator.class);
    private final InputStream packageAsInputStream;
    private final String packageName;
    private final FhirVersionEnum fhirVersion;
    private final List<String> profiles;
    private final Path path;
    private final SimpleWorkerContext workerContext;

    public static void main(String[] args) throws Exception {

        //var pathToPackage = "src/main/resources/package.tgz";
        var packageAsInputStream = new ClassPathResource("package.tgz").getInputStream();
        var outputFolder = "target/generated-sources/java";
        var packageName = "org.hl7.fhir.example.generated";
        var fhirVersion = FhirVersionEnum.R4;
        var profiles = List.of("http://hl7.dk/fhir/core/StructureDefinition/dk-core-cpr-identifier", "http://hl7.dk/fhir/core/StructureDefinition/dk-core-gln-identifier");
        new CodeGenerator(packageAsInputStream, outputFolder, packageName, fhirVersion, profiles).generateCode();
    }

    public CodeGenerator(InputStream packageAsInputStream, String outputFolder, String packageName, FhirVersionEnum fhirVersion, List<String> profiles) throws Exception {
        this.packageAsInputStream = packageAsInputStream;
        this.packageName = packageName;
        this.fhirVersion = fhirVersion;
        this.profiles = profiles;

        Path path = Path.of(outputFolder, packageName.replaceAll("\\.", "/"));
        if (!Files.exists(path)) Files.createDirectories(path);
        this.path = path;

        var npmPackage = NpmPackage.fromPackage(packageAsInputStream);
        this.workerContext = SimpleWorkerContext.fromPackage(npmPackage);

        switch (fhirVersion) {
            case R4:
                workerContext.loadFromFolder("src/main/resources/r4/definitions.json");
                break;
            case R5:
                workerContext.loadFromFolder("src/main/resources/r5/definitions.json");
                break;
            default:
                throw new IllegalArgumentException("Unsupported FHIR version: " + fhirVersion);
        }

    }

    public void generateCode() {

        logger.info("Starting code generation...");
        for (var p : profiles) {

            PECodeGenerator codeGenerator = new PECodeGenerator(workerContext);
            codeGenerator.setFolder(path.toString());
            codeGenerator.setCanonical(p);
            codeGenerator.setPkgName(packageName);
            codeGenerator.setExtensionPolicy(PECodeGenerator.ExtensionPolicy.Complexes);
            codeGenerator.setNarrative(true);
            codeGenerator.setMeta(true);
            codeGenerator.setLanguage(null);
            codeGenerator.setKeyElementsOnly(true);
            codeGenerator.setGenDate(new Date().toString());

            logger.info("Generating code for profile: {}", p);
            try {
                codeGenerator.execute();
            } catch (Exception e) {
                logger.error("Error generating code for profile: {}", p, e);
            }
        }
        logger.info("Code generation completed.");
    }
}


