package org.hl7.fhir.contrib;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

public class CodeGeneratorFactory {

    private static final Logger logger = LoggerFactory.getLogger(CodeGeneratorFactory.class);
    private final String packageName;
    private final List<String> profilesWhitelist;
    private final Path path;
    private final FhirVersionEnum fhirVersion;
    private final NpmPackage npmPackage;

    public CodeGeneratorFactory(NpmPackage npmPackage, String outputFolder, String packageName, List<String> profiles) throws Exception {
        this.npmPackage = npmPackage;
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

        fhirVersion = fhirContext.getVersion().getVersion();
        Path path = Path.of(outputFolder, packageName.replaceAll("\\.", "/"));

        if (!Files.exists(path)) Files.createDirectories(path);
        this.path = path;

    }

    public PECodeGenerator produceCodeGenerator() throws Exception {

        switch (fhirVersion) {
            case R4:
                return new R4PECodeGenerator(npmPackage, profilesWhitelist);
            case R5:
                return new R5PECodeGenerator(npmPackage, profilesWhitelist);
            default:
                throw new IllegalArgumentException("Unsupported FHIR version: " + fhirVersion);
        }
    }

    abstract class PECodeGenerator {
        private final List<String> profilesWhitelist;
        private final String date;

        public PECodeGenerator(List<String> canonicals) {
            this.profilesWhitelist = canonicals;
            this.date = new Date().toString();
        }
        void generate() {
            logger.info("Starting code generation on " + profilesWhitelist.size() + " profiles ...");

            for (var canonicalUrl : profilesWhitelist) {
                logger.info("Generating code for profile: {}", canonicalUrl);
                try {
                    generateCode(canonicalUrl, date);
                } catch (Exception e) {
                    logger.error("Error generating code for profile: {}", canonicalUrl, e);
                }
            }

            logger.info("Code generation completed.");

            System.exit(0);
        }
        abstract protected void generateCode(String canonicalUrl, String date) throws IOException;
    }

    class R4PECodeGenerator extends PECodeGenerator {

        private final org.hl7.fhir.r4.context.SimpleWorkerContext workerContext;

        R4PECodeGenerator(NpmPackage npmPackage, List<String> profilesWhitelist) throws Exception {
            super(profilesWhitelist);
            this.workerContext = org.hl7.fhir.r4.context.SimpleWorkerContext.fromPackage(npmPackage);
            workerContext.loadFromFolder("src/main/resources/r4/definitions.json");
        }

        @Override
        public void generateCode(String canonicalUrl, String date) throws IOException {
            produceR4PeCodeGenerator(canonicalUrl, date, workerContext).execute();
        }
    }

    class R5PECodeGenerator extends PECodeGenerator {

        private final org.hl7.fhir.r5.context.SimpleWorkerContext workerContext;

        R5PECodeGenerator(NpmPackage npmPackage, List<String> profilesWhitelist) throws IOException {
            super(profilesWhitelist);
            this.workerContext = new org.hl7.fhir.r5.context.SimpleWorkerContext.SimpleWorkerContextBuilder().fromPackage(npmPackage);
            workerContext.loadFromFolder("src/main/resources/r5/definitions.json");
        }


        @Override
        public void generateCode(String canonicalUrl, String date) throws IOException {
            produceR5PeCodeGenerator(canonicalUrl, date, workerContext).execute();
        }
    }

    private @NotNull org.hl7.fhir.r5.profilemodel.gen.PECodeGenerator produceR5PeCodeGenerator(String canonicalUrl, String date, org.hl7.fhir.r5.context.IWorkerContext workerContext) {
        org.hl7.fhir.r5.profilemodel.gen.PECodeGenerator codeGenerator = new org.hl7.fhir.r5.profilemodel.gen.PECodeGenerator(workerContext);
        codeGenerator.setFolder(path.toString());
        codeGenerator.setCanonical(canonicalUrl);
        codeGenerator.setPkgName(packageName);
        codeGenerator.setExtensionPolicy(org.hl7.fhir.r5.profilemodel.gen.PECodeGenerator.ExtensionPolicy.Complexes);
        codeGenerator.setNarrative(true);
        codeGenerator.setMeta(true);
        codeGenerator.setLanguage(null);
        codeGenerator.setKeyElementsOnly(true);
        codeGenerator.setGenDate(date);
        return codeGenerator;
    }

    private @NotNull org.hl7.fhir.r4.profilemodel.gen.PECodeGenerator produceR4PeCodeGenerator(String p, String date, org.hl7.fhir.r4.context.IWorkerContext workerContext) {
        org.hl7.fhir.r4.profilemodel.gen.PECodeGenerator codeGenerator = new org.hl7.fhir.r4.profilemodel.gen.PECodeGenerator(workerContext);
        codeGenerator.setFolder(path.toString());
        codeGenerator.setCanonical(p);
        codeGenerator.setPkgName(packageName);
        codeGenerator.setExtensionPolicy(org.hl7.fhir.r4.profilemodel.gen.PECodeGenerator.ExtensionPolicy.Complexes);
        codeGenerator.setNarrative(true);
        codeGenerator.setMeta(true);
        codeGenerator.setLanguage(null);
        codeGenerator.setKeyElementsOnly(true);
        codeGenerator.setGenDate(date);
        return codeGenerator;
    }
}
