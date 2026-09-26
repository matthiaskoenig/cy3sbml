package org.cy3sbml.reader;

import static org.cy3sbml.reader.ReaderTestSupport.nodeById;
import static org.cy3sbml.reader.ReaderTestSupport.nodesOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.cy3sbml.SBML;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.junit.jupiter.api.Test;

class CoreReaderTest {

    @Test
    void readsSpeciesAndReactionNodes() throws Exception {
        ConversionContext context = ReaderTestSupport.read("core_01.xml", new CoreReader());
        CyNetwork network = context.network();

        assertEquals(12, nodesOfType(network, SBML.NODETYPE_SPECIES).size());
        assertEquals(17, nodesOfType(network, SBML.NODETYPE_REACTION).size());

        // <species id="BLL" initialAmount="0" name="BasalACh2" ... compartment="comp1">
        CyNode species = nodeById(context, "BLL");
        assertEquals("comp1", ReaderTestSupport.attribute(network, species, SBML.ATTR_COMPARTMENT));
        assertEquals("BasalACh2", ReaderTestSupport.attribute(network, species, SBML.ATTR_NAME));
        assertEquals(0.0, network.getRow(species).get(SBML.ATTR_INITIAL_AMOUNT, Double.class));
    }

    @Test
    void readsModelAttributesIntoNetworkTable() throws Exception {
        ConversionContext context = ReaderTestSupport.read("core_01.xml", new CoreReader());
        CyNetwork network = context.network();

        assertEquals(SBML.NETWORKTYPE_SBML, network.getRow(network).get(SBML.NETWORKTYPE_ATTR, String.class));
        assertEquals("L2 V4", network.getRow(network).get(SBML.LEVEL_VERSION, String.class));
    }
}
