package org.cy3sbml.miriam;

import org.junit.Test;
import java.util.logging.Logger;
import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * Testing RegistryUtils.
 */
public class RegistryUtilTest {
    Resource resource;
    static Logger logger = Logger.getLogger("miriam");
    @Test
    public void updateMiriamJSON() throws Exception {
        
        File f = File.createTempFile("test", ".json");
        RegistryUtil.updateMiriamJSON(f);
        assertNotNull(f);
        Map<String, Namespace> result = RegistryUtil.loadRegistry(f);
        logger.info("=== Registry Contents ===");
        logger.info(String.valueOf(result.size()));
        result.forEach((k, v) -> logger.info(k + " -> " + v));

        testNamespaceContents(result, "chebi");
        testNamespaceContents(result, "uniprot");
        testResourcesOutput(result, "chebi");
        // testResourcesOutput(result, "uniprot");

        // Edge case: Namespace with no resources
        //testResourcesOutput(result, "pubmed");
    }

    public static void testNamespaceContents(Map<String, Namespace> registry, String targetPrefix) {
        logger.info("\n=== Testing Namespace: " + targetPrefix + " ===");

        Namespace ns = registry.get(targetPrefix);
        if (ns == null) {
            logger.info("Namespace '" + targetPrefix + "' not found!");
            logger.info("Available prefixes: " + registry.keySet());
            return;
        }

        // Print all fields in a readable format
        logger.info("ID: " + ns.getId());
        logger.info("Name: " + ns.getName());
        logger.info("Pattern: " + ns.getPattern());
        logger.info("Description: " + ns.getDescription());
        logger.info("Deprecated: " + ns.getDeprecated());
        logger.info("Resources: " + ns.getResources());

        // Print additional fields if they exist
        if (ns.getMirId() != null && !ns.getMirId().isEmpty()) {
            logger.info("MIR ID: " + ns.getMirId());
        }
        if (ns.getSampleId() != null && !ns.getSampleId().isEmpty()) {
            logger.info("Sample ID: " + ns.getSampleId());
        }

        logger.info("Class: " + ns.getClass().getSimpleName());
    }

    public static void testResourcesOutput(Map<String, Namespace> registry, String targetPrefix) {
        logger.info("\n=== RESOURCES TEST FOR: " + targetPrefix + " ===");

        Namespace ns = registry.get(targetPrefix);
        if (ns == null) {
            logger.info("Namespace not found!");
            return;
        }

        // Print the namespace's resources
        logger.info("Resources Type: " + (ns.getResources() == null ? "NULL" : ns.getResources().getClass().getSimpleName()));
        logger.info("Resources Count: " + (ns.getResources() == null ? 0 : ns.getResources().size()));

        // Detailed output
        if (ns.getResources() != null) {
            logger.info("\nResources Content:");

            ns.getResources().forEach(res ->
                    logger.info("  " + res.getResourceHomeUrl() + ", "+res.getDescription())

            );
        }}
}

