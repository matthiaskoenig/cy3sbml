package org.cy3sbml.miriam;

import org.junit.Test;

import java.text.MessageFormat;
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
        logger.info(MessageFormat.format("\n=== Testing Namespace: {0} ===", targetPrefix));

        Namespace ns = registry.get(targetPrefix);
        if (ns == null) {
            logger.info(MessageFormat.format("Namespace ''{0}'' not found!", targetPrefix));
            logger.info(MessageFormat.format("Available prefixes: {0}", registry.keySet()));
            return;
        }

        // Print all fields in a readable format
        logger.info(MessageFormat.format("ID: {0}", ns.getId()));
        logger.info(MessageFormat.format("Name: {0}", ns.getName()));
        logger.info(MessageFormat.format("Pattern: {0}", ns.getPattern()));
        logger.info(MessageFormat.format("Description: {0}", ns.getDescription()));
        logger.info(MessageFormat.format("Deprecated: {0}", ns.getDeprecated()));
        logger.info(MessageFormat.format("Resources: {0}", ns.getResources()));

        // Print additional fields if they exist
        if (ns.getMirId() != null && !ns.getMirId().isEmpty()) {
            logger.info(MessageFormat.format("MIR ID: {0}", ns.getMirId()));
        }
        if (ns.getSampleId() != null && !ns.getSampleId().isEmpty()) {
            logger.info(MessageFormat.format("Sample ID: {0}", ns.getSampleId()));
        }

        logger.info(MessageFormat.format("Class: {0}", ns.getClass().getSimpleName()));
    }

    public static void testResourcesOutput(Map<String, Namespace> registry, String targetPrefix) {
        logger.info(MessageFormat.format("\n=== RESOURCES TEST FOR: {0} ===", targetPrefix));

        Namespace ns = registry.get(targetPrefix);
        if (ns == null) {
            logger.info("Namespace not found!");
            return;
        }

        // Print the namespace's resources
        logger.info(MessageFormat.format("Resources Type: {0}",
                ns.getResources() == null ? "NULL" : ns.getResources().getClass().getSimpleName()));
        logger.info(MessageFormat.format("Resources Count: {0}",
                ns.getResources() == null ? 0 : ns.getResources().size()));

        // Detailed output
        if (ns.getResources() != null) {
            logger.info("\nResources Content:");

            ns.getResources().forEach(res ->
                    logger.info(MessageFormat.format("  {0}, {1}",
                            res.getResourceHomeUrl(),
                            res.getDescription()))
            );
        }}
}

