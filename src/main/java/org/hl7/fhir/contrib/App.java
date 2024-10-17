package org.hl7.fhir.contrib;

import org.hl7.fhir.r4.context.SimpleWorkerContext;
import org.hl7.fhir.r4.profilemodel.gen.PECodeGenerator;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

public class App {
    public static void main(String[] args) {
        try {
            var outputFolder = "target/generated-sources/java";
            var packageName = "org.example.fhir.generated";
            var profiles = List.of("http://hl7.dk/fhir/core/StructureDefinition/dk-core-cpr-identifier",
                    "http://hl7.dk/fhir/core/StructureDefinition/dk-core-gln-identifier");

            var npmPackage = NpmPackage.fromPackage(new ClassPathResource("package.tgz").getInputStream());


            // Step 1: Load core FHIR definitions
            SimpleWorkerContext workerContext = SimpleWorkerContext.fromPackage(npmPackage);
            workerContext.loadFromFolder("src/main/resources/definitions.json");


            Path path = Path.of(outputFolder, packageName.replaceAll("\\.", "/"));
            if (!Files.exists(path)) Files.createDirectories(path);

            for (var p : profiles) {
                // Step 3: Instantiate and configure the code generator
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

                // Step 4: Execute code generation
                codeGenerator.execute();
            }

            System.out.println("Code generation completed.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

