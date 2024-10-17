package org.hl7.fhir.contrib;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.profilemodel.gen.PECodeGenerator;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

public class CodeGenerator {

    private static final Logger logger = LoggerFactory.getLogger(CodeGenerator.class);
    private final String packageName;
    private final List<String> profilesWhitelist;
    private final Path path;
    private final SimpleWorkerContext workerContext;

    public CodeGenerator(NpmPackage npmPackage, String outputFolder, String packageName, List<String> profiles) throws Exception {
        var fhirContext = new FhirContext(FhirVersionEnum.forVersionString(npmPackage.fhirVersion()));

        this.packageName = packageName;
        this.profilesWhitelist = profiles;

        if (profilesWhitelist.isEmpty()) {
            var fhirPath = fhirContext.newFhirPath();
            var parsedExpression = fhirPath.parse("url");
            var folder = npmPackage.getFolders().get("package");
            var structureDefs = folder.getTypes().get("StructureDefinition");
            var allProfiles = structureDefs.stream().map(sd -> {
                try {
                    return folder.fetchFile(sd);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).map(bytes -> fhirContext.newJsonParser().setSuppressNarratives(true).parseResource(new String(bytes))).map(b -> (String) fhirPath.evaluate(b, parsedExpression, IPrimitiveType.class).get(0).getValue()).toList();
            profilesWhitelist.addAll(allProfiles);
        }

        FhirVersionEnum fhirVersion = fhirContext.getVersion().getVersion();
        Path path = Path.of(outputFolder, packageName.replaceAll("\\.", "/"));

        if (!Files.exists(path)) Files.createDirectories(path);
        this.path = path;

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
        String date = new Date().toString();
        for (var p : profilesWhitelist) {

            PECodeGenerator codeGenerator = new PECodeGenerator(workerContext);
            codeGenerator.setFolder(path.toString());
            codeGenerator.setCanonical(p);
            codeGenerator.setPkgName(packageName);
            codeGenerator.setExtensionPolicy(PECodeGenerator.ExtensionPolicy.Complexes);
            codeGenerator.setNarrative(true);
            codeGenerator.setMeta(true);
            codeGenerator.setLanguage(null);
            codeGenerator.setKeyElementsOnly(true);
            codeGenerator.setGenDate(date);

            logger.info("Generating code for profile: {}", p);
            try {
                codeGenerator.execute();
            } catch (Exception e) {
                logger.error("Error generating code for profile: {}", p, e);
            }
        }
        logger.info("Code generation completed.");

        System.exit(0);
    }
}
