package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.*;
import org.cytoscape.work.TaskMonitor;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper functions to test SBML models.
 */
public class TestUtils {
    public static String BIOMODELS_RESOURCE_PATH = "/models/biomodels";
    public static String BIGGMODELS_RESOURCE_PATH = "/models/bigg_models";
    public static String SBMLTESTCASES_RESOURCE_PATH = "/models/sbml-test-suite";
    public static String UNITTESTS_RESOURCE_PATH = "/models/unittests";

    private static final Logger logger = LoggerFactory.getLogger(TestUtils.class);

    /**
     * Reads the system proxy variables and sets the
     * java system variables so that the tests use these.
     */
    public static void setSystemProxyForTests() {
        Map<String, String> env = System.getenv();
        String key = "HTTP_PROXY";
        if (env.containsKey(key)) {
            String value = env.get(key);
            if (value.startsWith("http://")) {
                value = value.substring(7, value.length());
            }
            String[] tokens = value.split(":", -1);
            // we found the proxy settings
            if (tokens.length == 2 && !tokens[1].isEmpty()) {
                String host = tokens[0];
                String port = tokens[1];
                logger.info(String.format("Set test proxy: %s:%s", host, port));
                System.setProperty("http.proxyHost", host);
                System.setProperty("http.proxyPort", port);
            }
        }
    }

    /**
     * Get an iteratable over the resources in the resourcePath.
     * <p>
     * Resources in the skip set are skipped.
     * If a filter string is given only the resources matching the filter are returned.
     *
     * @param where the resource root: "main" (src/main/resources), "corpora" (the large model
     *     corpora in src/test/corpora) or "test" (src/test/resources)
     */
    public static Iterable<Object[]> findResources(
            String where, String resourcePath, String extension, String filter, Set<String> skip) {

        File currentDir = new File(System.getProperty("user.dir"));
        // String rootPath = new File(currentDir, resourcePath).getPath();
        String rootPath;
        if (where.equals("main")) {
            rootPath = currentDir.getAbsolutePath() + "/src/main/resources" + resourcePath;
        } else if (where.equals("corpora")) {
            rootPath = currentDir.getAbsolutePath() + "/src/test/corpora" + resourcePath;
        } else {
            rootPath = currentDir.getAbsolutePath() + "/src/test/resources" + resourcePath;
        }
        // Get SBML files for passed tests
        List<String> sbmlPaths = TestUtils.findFiles(rootPath, extension, filter, skip);
        Collections.sort(sbmlPaths);

        int N = sbmlPaths.size();

        Object[][] resources = new String[N][1];
        for (int k = 0; k < N; k++) {
            String path = sbmlPaths.get(k);
            // create the resource
            String[] items = path.split("/", -1);
            int mindex = -1;
            for (int i = 0; i < items.length; i++) {
                if (items[i].equals("models")) {
                    mindex = i;
                    break;
                }
            }
            String resource = String.join("/", Arrays.copyOfRange(items, mindex, items.length));
            resources[k][0] = "/" + resource;
        }
        return Arrays.asList(resources);
    }

    /**
     * Search recursively for all SBML files in given path.
     * SBML files have to end in ".xml" and pass the filter expression
     * and is not in the skip set.
     */
    public static List<String> findFiles(String path, String extension, String filter, Set<String> skip) {
        List<String> fileList = new ArrayList<>();

        File root = new File(path);
        File[] list = root.listFiles();

        if (list == null) {
            return fileList;
        }
        if (skip == null) {
            skip = new HashSet<>();
        }

        for (File f : list) {
            String fpath = f.getAbsolutePath();
            // recursively search directories
            if (f.isDirectory()) {
                fileList.addAll(findFiles(fpath, extension, filter, skip));
            } else {
                String fname = f.getName();
                if (fname.endsWith(extension) && !skip.contains(fname)) {
                    // no filter add
                    if (filter == null) {
                        fileList.add(fpath);
                    } else {
                        // filter matches add
                        Pattern pattern = Pattern.compile(filter);
                        Matcher m = pattern.matcher(fname);
                        if (m.find()) {
                            fileList.add(fpath);
                        }
                    }
                }
            }
        }
        return fileList;
    }

    public static List<String> findFiles(String path, String extension) {
        return findFiles(path, extension, null, null);
    }

    /**
     * Read the CyNetworks from given SBML file resource.
     * <p>
     * Failures of the reader propagate so that they fail the calling test.
     */
    public static CyNetwork[] readNetwork(String resource) throws Exception {
        final CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        final CyGroupFactory groupFactory = new GroupTestSupport().getGroupFactory();

        String[] tokens = resource.split("/", -1);
        String fileName = tokens[tokens.length - 1];
        try (InputStream instream = TestUtils.class.getResourceAsStream(resource)) {
            assertNotNull(instream, "Resource not found: " + resource);
            // Reader can be tested without service adapter; with the location, as for a file
            // imported in Cytoscape, so that external model definitions are found
            SBMLReaderTask readerTask =
                    new SBMLReaderTask(instream, fileName, location(resource), networkFactory, groupFactory);
            readerTask.run(mock(TaskMonitor.class));
            return readerTask.getNetworks();
        }
    }

    /**
     * Log the memory usage.
     */
    private static void logMemory(String info) {
        System.gc();
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        logger.warn(String.format("<%s> memory usage: %s MB", info, usedMB));
    }

    public static CyNode findNodeById(String sbmlId, CyNetwork network) {
        for (CyNode node : network.getNodeList()) {
            CyRow attributes = network.getRow(node);
            String id = attributes.get(SBML.ATTR_ID, String.class);
            if (id != null && id.equals(sbmlId)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Reads the given SBML resource with the SBMLReaderTask.
     * <p>
     * Failures of the reader propagate so that they fail the calling test.
     */
    public static void testNetwork(TaskMonitor taskMonitor, String testType, String resource) throws Exception {
        logger.info("--------------------------------------------------------");
        logger.info(String.format("%s : %s", testType, resource));

        final CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        final CyGroupFactory groupFactory = new GroupTestSupport().getGroupFactory();

        String[] tokens = resource.split("[/\\\\]", -1);
        String fileName = tokens[tokens.length - 1];
        try (InputStream instream = openModel(resource)) {
            // Reader can be tested without service adapter
            SBMLReaderTask readerTask =
                    new SBMLReaderTask(instream, fileName, location(resource), networkFactory, groupFactory);
            readerTask.run(taskMonitor);
            assertFalse(readerTask.getError());
            assertTrue(readerTask.getNetworks().length >= 1);
        }

        // Display memory usage
        logMemory(fileName);
    }

    /** The location of the classpath resource. */
    private static URI location(String resource) throws URISyntaxException {
        return TestUtils.class.getResource(resource).toURI();
    }

    /**
     * Opens the classpath resource of a model found by {@link #findResources}.
     */
    private static InputStream openModel(String resource) {
        InputStream instream = TestUtils.class.getResourceAsStream(resource);
        assertNotNull(instream, "Resource not found: " + resource);
        return instream;
    }

    /**
     * Perform the network test for a given SBML resource.
     * <p>
     * There is a memory leak in the network creation, probably the following issue
     * http://code.cytoscape.org/redmine/issues/3507
     * <p>
     * See also:
     * This aborts the travis build.
     */
    public static void testNetworkSerialization(String testType, String resource)
            throws IOException, XMLStreamException, ClassNotFoundException {
        logger.info("--------------------------------------------------------");
        logger.info(String.format("%s : %s", testType, resource));

        SBMLDocument doc;
        try (InputStream instream = openModel(resource)) {
            doc = SBMLReader.read(instream);
        }
        assertNotNull(doc);

        // Serialize SBMLDocument
        File tempFile = File.createTempFile("sbml", ".ser");
        tempFile.deleteOnExit();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(tempFile))) {
            out.writeObject(doc);
        }

        // Deserialize
        try (ObjectInput input = new ObjectInputStream(new BufferedInputStream(new FileInputStream(tempFile)))) {
            SBMLDocument docSerialized = (SBMLDocument) input.readObject();
            assertNotNull(docSerialized);
        }
    }
}
