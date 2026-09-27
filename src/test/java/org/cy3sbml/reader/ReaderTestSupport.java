package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.util.List;
import org.cy3sbml.SBML;
import org.cy3sbml.TestUtils;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

/**
 * Runs package readers on the unit test models and queries the resulting network.
 */
final class ReaderTestSupport {

    private ReaderTestSupport() {}

    /**
     * Reads the unit test model with the given file name with the readers in the given order.
     */
    static ConversionContext read(String fileName, PackageReader... readers) throws Exception {
        return readResource(TestUtils.UNITTESTS_RESOURCE_PATH + "/" + fileName, readers);
    }

    /**
     * Reads the model resource with the readers in the given order.
     */
    static ConversionContext readResource(String resource, PackageReader... readers) throws Exception {
        SBMLDocument document;
        try (InputStream stream = ReaderTestSupport.class.getResourceAsStream(resource)) {
            assertNotNull(stream, "Resource not found: " + resource);
            document = new SBMLReader().readSBMLFromStream(stream);
        }
        return read(document, readers);
    }

    /**
     * Reads the given SBML string with the readers in the given order.
     */
    static ConversionContext readString(String sbml, PackageReader... readers) throws Exception {
        return read(new SBMLReader().readSBMLFromString(sbml), readers);
    }

    /**
     * Runs the given readers on an already-built {@link SBMLDocument}, e.g. one built
     * programmatically in a test rather than parsed from an XML resource or string.
     */
    static ConversionContext read(SBMLDocument document, PackageReader... readers) {
        CyNetwork network = new NetworkTestSupport().getNetworkFactory().createNetwork();
        ConversionContext context = new ConversionContext(document, network, new GroupTestSupport().getGroupFactory());
        Model model = document.getModel();
        for (PackageReader reader : readers) {
            reader.read(context, model);
        }
        return context;
    }

    static List<CyNode> nodesOfType(CyNetwork network, String nodeType) {
        return network.getNodeList().stream()
                .filter(n -> nodeType.equals(network.getRow(n).get(SBML.NODETYPE_ATTR, String.class)))
                .toList();
    }

    static List<CyEdge> edgesOfType(CyNetwork network, String interactionType) {
        return network.getEdgeList().stream()
                .filter(e -> interactionType.equals(network.getRow(e).get(SBML.INTERACTION_ATTR, String.class)))
                .toList();
    }

    static CyNode nodeById(ConversionContext context, String id) {
        return context.nodeById(id).orElseThrow(() -> new AssertionError("No node for id: " + id));
    }

    static String attribute(CyNetwork network, CyNode node, String name) {
        return network.getRow(node).get(name, String.class);
    }
}
