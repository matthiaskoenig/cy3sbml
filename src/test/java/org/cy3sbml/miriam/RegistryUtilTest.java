package org.cy3sbml.miriam;

import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;


/**
 * Testing RegistryUtils.
 */
public class RegistryUtilTest {
    Resource resource;
    @Test
    public void updateMiriamJSON() throws Exception {
        File f = File.createTempFile("test", ".json");
        RegistryUtil.updateMiriamJSON(f);
        assertNotNull(f);
        Map<String, Namespace> result = RegistryUtil.loadRegistry(f);
        System.out.println("=== Registry Contents ===");
        System.out.println(result.size());
        result.forEach((k, v) -> System.out.println(k + " -> " + v));

        testNamespaceContents(result, "chebi");
        testNamespaceContents(result, "uniprot");
        //testResourcesOutput(result, "chebi");
       // testResourcesOutput(result, "uniprot");

        // Edge case: Namespace with no resources
        //testResourcesOutput(result, "pubmed");
    }

    public static void testNamespaceContents(Map<String, Namespace> registry, String targetPrefix) {
        System.out.println("\n=== Testing Namespace: " + targetPrefix + " ===");

        Namespace ns = registry.get(targetPrefix);
        if (ns == null) {
            System.out.println("Namespace '" + targetPrefix + "' not found!");
            System.out.println("Available prefixes: " + registry.keySet());
            return;
        }

        // Print all fields in a readable format
        System.out.println("ID: " + ns.getId());
        System.out.println("Name: " + ns.getName());
        System.out.println("Pattern: " + ns.getPattern());
        System.out.println("Description: " + ns.getDescription());
        System.out.println("Deprecated: " + ns.getDeprecated());
        System.out.println("Resources: " + ns.getResources());

        // Print additional fields if they exist
        if (ns.getMirId() != null && !ns.getMirId().isEmpty()) {
            System.out.println("MIR ID: " + ns.getMirId());
        }
        if (ns.getSampleId() != null && !ns.getSampleId().isEmpty()) {
            System.out.println("Sample ID: " + ns.getSampleId());
        }

        System.out.println("Class: " + ns.getClass().getSimpleName());
    }

    public static void testResourcesOutput(Map<String, Namespace> registry, String targetPrefix) {
        System.out.println("\n=== RESOURCES TEST FOR: " + targetPrefix + " ===");

        Namespace ns = registry.get(targetPrefix);
        if (ns == null) {
            System.out.println("Namespace not found!");
            return;
        }

        // Print the namespace's resources
        System.out.println("Resources Type: " + (ns.getResources() == null ? "NULL" : ns.getResources().getClass().getSimpleName()));
        System.out.println("Resources Count: " + (ns.getResources() == null ? 0 : ns.getResources().size()));

        // Detailed output
        if (ns.getResources() != null) {
            List<Resource> resources = ns.getResources();
            System.out.println("\nResources Content:");
            for (Resource resource: ns.getResources()){
                System.out.println("URL: " + resource.getId());
                System.out.println("Type: " + resource.toString());
            }};
        }


}