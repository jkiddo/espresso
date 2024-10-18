package org.hl7.fhir.contrib;


import ca.uhn.fhir.jpa.packages.loader.PackageLoaderSvc;
import org.hl7.fhir.utilities.npm.FilesystemPackageCacheManager;
import org.hl7.fhir.utilities.npm.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class})
public class Application implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Value("${outputFolder:target/generated-sources/java}")
    String outputFolder;
    @Value("${packageName:org.hl7.fhir.example.generated}")
    String packageName;
    //@Value("#{'${profiles:http://hl7.dk/fhir/core/StructureDefinition/dk-core-cpr-identifier,http://hl7.dk/fhir/core/StructureDefinition/dk-core-gln-identifier}'.split(',')}")
    @Value("${profiles:}")
    private List<String> profiles;
    //@Value("${package:file:/Users/jkiddo/work/espresso/src/main/resources/package.tgz}")
    @Value("${package:https://hl7.dk/fhir/core/package.tgz}")
    private String packagePath;

    public static void main(String[] args) throws Exception {

        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {

        NpmPackage npmPackage = validatePackage();
        CodeGeneratorFactory.PECodeGenerator generator = new CodeGeneratorFactory(npmPackage, outputFolder, packageName, profiles).produceCodeGenerator();
        generator.generate();

    }

    private NpmPackage validatePackage() throws IOException {

        var packageManager = new FilesystemPackageCacheManager.Builder().build();
        var npmAsBytes = new PackageLoaderSvc().loadPackageUrlContents(packagePath);
        var npmPackage = NpmPackage.fromPackage(new ByteArrayInputStream(npmAsBytes));
        packageManager.addPackageToCache(npmPackage.id(), npmPackage.version(), new ByteArrayInputStream(npmAsBytes), npmPackage.description());
        var packageId = npmPackage.id() + "#" + npmPackage.version();
        return packageManager.loadPackage(packageId);
    }
}


