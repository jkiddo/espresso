package org.hl7.fhir.contrib;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.packages.loader.PackageLoaderSvc;
import com.google.common.base.Strings;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class CodeGeneratorFactory {

    private static final Logger logger = LoggerFactory.getLogger(CodeGeneratorFactory.class);
    private final String packageName;
    private final Set<String> profilesWhitelist;
    private final Path path;
    private final FhirVersionEnum fhirVersion;
    private final NpmPackage npmPackage;

    /**
     * @param packageId    The package id to generate code from
     * @param outputFolder The output folder for the generated code
     * @param packageName  The package name for the generated code
     * @param profiles     The profiles to generate code for
     * @throws Exception if any
     */
    public CodeGeneratorFactory(@NotNull String packageId, @NotNull String outputFolder, @NotNull String packageName, @Nullable Set<String> profiles) throws Exception {

        this.npmPackage = validatePackage(packageId);
        var fhirContext = new FhirContext(FhirVersionEnum.forVersionString(npmPackage.fhirVersion()));

        this.packageName = packageName;

        if (profiles == null || profiles.isEmpty()) {

            var fhirPath = fhirContext.newFhirPath();
            var parsedExpression = fhirPath.parse("url");
            var folder = npmPackage.getFolders().get("package");
            var structureDefs = folder.getTypes().get("StructureDefinition");
            this.profilesWhitelist = structureDefs.stream().map(sd -> {
                try {
                    return folder.fetchFile(sd);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).map(bytes -> fhirContext.newJsonParser().setSuppressNarratives(true).parseResource(new String(bytes))).map(b -> (String) fhirPath.evaluate(b, parsedExpression, IPrimitiveType.class).get(0).getValue()).collect(Collectors.toUnmodifiableSet());

        } else {
            this.profilesWhitelist = Set.copyOf(profiles);
            ;
        }

        fhirVersion = fhirContext.getVersion().getVersion();
        Path path = Path.of(outputFolder, packageName.replaceAll("\\.", "/"));

        if (!Files.exists(path)) Files.createDirectories(path);
        this.path = path;

    }

    public static NpmPackage validatePackage(String packagePath) throws IOException {

        var packageManager = new FilesystemPackageCacheManager.Builder().build();
        if (!Strings.isNullOrEmpty(packagePath) && !packagePath.startsWith("http:") && !packagePath.startsWith("https:") && !packagePath.startsWith("classpath:") && !packagePath.startsWith("file:") && !packagePath.startsWith("/"))
            return packageManager.loadPackage(packagePath);

        var npmAsBytes = new PackageLoaderSvc().loadPackageUrlContents(packagePath);
        var npmPackage = NpmPackage.fromPackage(new ByteArrayInputStream(npmAsBytes));
        packageManager.addPackageToCache(npmPackage.id(), npmPackage.version(), new ByteArrayInputStream(npmAsBytes), npmPackage.description());
        return packageManager.loadPackage(npmPackage.id() + "#" + npmPackage.version());
    }

    public PECodeGenerator produceCodeGenerator() throws Exception {

        return switch (fhirVersion) {
            case R4 -> new R4PECodeGenerator(npmPackage, profilesWhitelist);
            case R5 -> new R5PECodeGenerator(npmPackage, profilesWhitelist);
            default -> throw new IllegalArgumentException("Unsupported FHIR version: " + fhirVersion);
        };
    }

    public abstract static class PECodeGenerator {
        private final Set<String> profilesWhitelist;
        private final String date;

        public PECodeGenerator(Set<String> canonicals) {
            this.profilesWhitelist = canonicals;
            this.date = new Date().toString();
        }

        /**
         * Generate code for the profiles in the whitelist
         */
        public void generate() {
            logger.info("Starting code generation on {} profiles ...", profilesWhitelist.size());

            for (var canonicalUrl : profilesWhitelist) {
                logger.info("Generating code for profile: {}", canonicalUrl);
                try {
                    generateCode(canonicalUrl, date);
                } catch (Exception e) {
                    logger.error("Error generating code for profile: {}", canonicalUrl, e);
                }
            }

            logger.info("Code generation completed.");
        }

        abstract protected void generateCode(String canonicalUrl, String date) throws IOException;
    }

    class R4PECodeGenerator extends PECodeGenerator {

        private final org.hl7.fhir.r4.context.SimpleWorkerContext workerContext;

        R4PECodeGenerator(NpmPackage npmPackage, Set<String> profilesWhitelist) throws Exception {
            super(profilesWhitelist);
            this.workerContext = org.hl7.fhir.r4.context.SimpleWorkerContext.fromPackage(npmPackage);

            //workerContext.loadFromFolder("src/main/resources/r4/definitions.json");
            loadDefinitions();
            workerContext.setExpansionProfile(new org.hl7.fhir.r4.model.Parameters());
        }

        private void loadDefinitions() throws IOException {

            var parser = FhirContext.forR4().newJsonParser();
            var zis = new ZipInputStream(new DefaultResourceLoader().getResource("classpath:/r4/definitions.json.zip").getInputStream());

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".json")) {
                    var resource = (org.hl7.fhir.r4.model.Resource) parser.parseResource(new String(zis.readAllBytes()));
                    try {
                        if (resource.getResourceType() == org.hl7.fhir.r4.model.ResourceType.Bundle) {
                            for (org.hl7.fhir.r4.model.Bundle.BundleEntryComponent e : ((org.hl7.fhir.r4.model.Bundle) resource).getEntry()) {
                                workerContext.cacheResource(e.getResource());
                            }
                        } else {
                            workerContext.cacheResource(resource);
                        }
                    } catch (Exception e) {
                        logger.debug("Error loading definitions", e);
                    }
                }
            }
        }

        @Override
        public void generateCode(String canonicalUrl, String date) throws IOException {
            produceR4PeCodeGenerator(canonicalUrl, date, workerContext).execute();
        }
    }

    class R5PECodeGenerator extends PECodeGenerator {

        private final org.hl7.fhir.r5.context.SimpleWorkerContext workerContext;

        R5PECodeGenerator(NpmPackage npmPackage, Set<String> profilesWhitelist) throws IOException {
            super(profilesWhitelist);
            this.workerContext = new org.hl7.fhir.r5.context.SimpleWorkerContext.SimpleWorkerContextBuilder().fromPackage(npmPackage);
            //workerContext.loadFromFolder("src/main/resources/r5/definitions.json");
            loadDefinitions();
        }

        private void loadDefinitions() throws IOException {
            var parser = FhirContext.forR4().newJsonParser();
            var zis = new ZipInputStream(new DefaultResourceLoader().getResource("classpath:/r5/definitions.json.zip").getInputStream());

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".json")) {
                    var resource = (org.hl7.fhir.r5.model.Resource) parser.parseResource(new String(zis.readAllBytes()));
                    try {
                        if (resource.getResourceType() == org.hl7.fhir.r5.model.ResourceType.Bundle) {
                            for (org.hl7.fhir.r5.model.Bundle.BundleEntryComponent e : ((org.hl7.fhir.r5.model.Bundle) resource).getEntry()) {
                                workerContext.cacheResource(e.getResource());
                            }
                        } else {
                            workerContext.cacheResource(resource);
                        }
                    } catch (Exception e) {
                        logger.debug("Error loading definitions", e);
                    }
                }
            }
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
        codeGenerator.setVersion("r5");
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
        codeGenerator.setVersion("r4");
        return codeGenerator;
    }
}
