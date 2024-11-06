# Project espresso

This project is a proof of concept for generating Java classes from FHIR profiles using the HAPI FHIR library as a Maven plugin. Generated classes are based on the HAPI FHIR core data models.

## Usage as Maven plugin

Add the following plugin to your existing Maven setup and generated sources with `mvn generate-sources`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.hl7.fhir.contrib</groupId>
            <artifactId>fhir-codegen-maven-plugin</artifactId>
            <version>1.0-SNAPSHOT</version>
            <configuration>
                <packageName>test.packages</packageName>
                <packageId>https://hl7.dk/fhir/core/package.tgz</packageId>
            </configuration>
            <executions>
                <execution>
                    <id>generate</id>
                    <goals>
                        <goal>generate</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

#### Dependency on HAPI core projects

This project relies heavily on the core projects from the [HAPI FHIR library](https://github.com/hapifhir/org.hl7.fhir.core). The core projects are available in the
Maven Central Repository, but from time to time `edge`-builds are needed. Do that locally by running the following on
the core project:
`mvn wrapper:wrapper -Dmaven=3.6.3 -Dmaven.test.skip clean install`


```

