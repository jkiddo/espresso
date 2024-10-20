# Project espresso

This project is a proof of concept for generating Java classes from FHIR profiles using the HAPI FHIR library.

## Getting started

TBD

#### Build the core projects on https://github.com/hapifhir/org.hl7.fhir.core

This project relies heavily on the core projects from the HAPI FHIR library. The core projects are available in the
Maven Central Repository, but from time to time `edge`-builds are needed. Do that locally by running the following on
the core project:
`mvn wrapper:wrapper -Dmaven=3.6.3 clean install`

#### Test use of the core projects in e.g. a Spring Boot application

```java
package org.hl7.fhir.contrib;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import java.util.List;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class})
public class Application implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Value("${outputFolder:target/generated-sources/java}")
    String outputFolder;
    @Value("${packageName:org.hl7.fhir.example.generated}")
    String packageName;
    @Value("#{'${profiles:http://hl7.dk/fhir/core/StructureDefinition/dk-core-cpr-identifier,http://hl7.dk/fhir/core/StructureDefinition/dk-core-gln-identifier}'.split(',')}")
    private List<String> profiles;
    @Value("${package:https://hl7.dk/fhir/core/package.tgz}")
    private String packagePath;

    public static void main(String[] args) throws Exception {

        SpringApplication.run(Application.class, args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        CodeGeneratorFactory.PECodeGenerator generator = new CodeGeneratorFactory(packagePath, outputFolder, packageName, profiles).produceCodeGenerator();
        generator.generate();
    }
}
```