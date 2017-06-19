package org.cy3sbml.archive;


import org.apache.taverna.robundle.Bundle;
import org.apache.taverna.robundle.Bundles;
import org.apache.taverna.robundle.manifest.Manifest;
import org.apache.taverna.robundle.manifest.PathAnnotation;
import org.apache.taverna.robundle.manifest.PathMetadata;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Working with research bundles.
 *
 * This is working outside of Cytoscape, but fails to work within the Cytoscape OSGI app.
 * http://stackoverflow.com/questions/11677572/dealing-with-xerces-hell-in-java-maven
 *
 * Uses the taverna robundle implementation to read the bundle.
 *
 *
 */
public class ROBundle {


    public static void roBundleTest() throws IOException {
        // Create a new (temporary) RO bundle
        Bundle bundle = Bundles.createBundle();

        // Get the inputs
        Path inputs = bundle.getRoot().resolve("inputs");
        Files.createDirectory(inputs);

        // Get an input port:
        Path in1 = inputs.resolve("in1");

        // Setting a string value for the input port:
        Bundles.setStringValue(in1, "Hello");

        // And retrieving it
        if (Bundles.isValue(in1)) {
            System.out.println(Bundles.getStringValue(in1));
        }

        // Or just use the regular Files methods:
        for (String line : Files.readAllLines(in1, Charset.forName("UTF-8"))) {
            System.out.println(line);
        }

        // Binaries and large files are done through the Files API
        try (OutputStream out = Files.newOutputStream(in1,
                StandardOpenOption.APPEND)) {
            out.write(32);
        }
        // Or Java 7 style
        Path localFile = Files.createTempFile("", ".txt");
        Files.copy(in1, localFile, StandardCopyOption.REPLACE_EXISTING);
        System.out.println("Written to: " + localFile);

        Files.copy(localFile, bundle.getRoot().resolve("out1"));

        // Representing references
        URI ref = URI.create("http://example.com/external.txt");
        Path out3 = bundle.getRoot().resolve("out3");
        System.out.println(Bundles.setReference(out3, ref));
        if (Bundles.isReference(out3)) {
            URI resolved = Bundles.getReference(out3);
            System.out.println(resolved);
        }

        // Saving a bundle:
        Path zip = Files.createTempFile("bundle", ".zip");
        Bundles.closeAndSaveBundle(bundle, zip);
        // NOTE: From now "bundle" and its Path's are CLOSED
        // and can no longer be accessed

        System.out.println("Saved to " + zip);

        // Loading a bundle back from disk
        try (Bundle bundle2 = Bundles.openBundle(zip)) {
            assertEquals(zip, bundle2.getSource());
        }
    }

    /**
     * Read the given resource bundle.
     * @param zipPath path to ro.zip resource.
     */
    public static Bundle readBundle(Path zipPath){
        Bundle bundle = null;
        try {
            bundle = Bundles.openBundle(zipPath);

            System.out.println(bundle);

            // Read information from the manifest file
            Manifest manifest = bundle.getManifest();
            System.out.println(manifest);


            System.out.println("CreatedBy: " + manifest.getCreatedBy());
            System.out.println("CreatedOn: " + manifest.getCreatedOn());

            System.out.println("<manifest>");
            List<Path> pathList = manifest.getManifest();
            for (Path p: pathList){
                System.out.println(p);
            }

            System.out.println("<aggregates>");
            List<PathMetadata> aggregates = manifest.getAggregates();
            for (PathMetadata metaData: aggregates){
                System.out.println(metaData);
            }
            System.out.println("<annotations>");
            for (PathAnnotation a: manifest.getAnnotations()){
                System.out.println(a);

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return bundle;
    }


    /**
     * First test of Research objects.
     */
    public static void main(String[] args) throws URISyntaxException, IOException {

        // research object
        System.out.println("--------------------------------------");
        System.out.println("Research Object");
        System.out.println("--------------------------------------");
        URL url = ROBundle.class.getResource("/ro/investigation-96-2.ro.zip");
        Path roPath = Paths.get(url.toURI());
        readBundle(roPath);

        // omex Combine archive
        System.out.println("--------------------------------------");
        System.out.println("OMEX");
        System.out.println("--------------------------------------");
        Path omexPath = Paths.get(ROBundle.class.getResource("/omex/CombineArchiveShowCase.omex").toURI());
        readBundle(omexPath);
    }

}
