package org.hl7.fhir.contrib;


import ca.uhn.fhir.context.FhirContext;

import org.hl7.fhir.utilities.npm.NpmPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ContextBuilder {

    private static final Logger logger = LoggerFactory.getLogger(ContextBuilder.class);

    private ContextBuilder() {

    }

    public static R4ContextBuilder usingR4(NpmPackage npmPackage) {
        try {
            return new R4ContextBuilder(npmPackage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static R5ContextBuilder usingR5(NpmPackage npmPackage) {
        try {
            return new R5ContextBuilder(npmPackage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class R5ContextBuilder {
        private final org.hl7.fhir.r5.context.SimpleWorkerContext workerContext;

        private R5ContextBuilder(NpmPackage npmPackage) throws IOException {
            workerContext = new org.hl7.fhir.r5.context.SimpleWorkerContext.SimpleWorkerContextBuilder().fromPackage(npmPackage);
            workerContext.setExpansionParameters(new org.hl7.fhir.r5.model.Parameters());

            var parser = FhirContext.forR5().newJsonParser();
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

        public org.hl7.fhir.r5.context.SimpleWorkerContext build() {
            return workerContext;
        }
    }

    public static class R4ContextBuilder {
        private final org.hl7.fhir.r4.context.SimpleWorkerContext workerContext;

        private R4ContextBuilder(NpmPackage npmPackage) throws IOException {
            workerContext = org.hl7.fhir.r4.context.SimpleWorkerContext.fromPackage(npmPackage);
            workerContext.setExpansionProfile(new org.hl7.fhir.r4.model.Parameters());

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

        public org.hl7.fhir.r4.context.SimpleWorkerContext build() {
            return workerContext;
        }
    }
}
