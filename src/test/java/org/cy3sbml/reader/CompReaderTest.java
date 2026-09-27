package org.cy3sbml.reader;

import static org.cy3sbml.reader.ReaderTestSupport.edgesOfType;
import static org.cy3sbml.reader.ReaderTestSupport.nodesOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.junit.jupiter.api.Test;

class CompReaderTest {

    @Test
    void createsPortNodesWithEdgesToReferencedElements() throws Exception {
        ConversionContext context = ReaderTestSupport.read("comp_01.xml", new CoreReader(), new CompReader());
        CyNetwork network = context.network();

        // <comp:port comp:idRef="GFP" sboTerm="SBO:0000600" comp:id="input__GFP"/>
        // <comp:port comp:idRef="Degradation_GFP" sboTerm="SBO:0000601" comp:id="reaction__Degradation_GFP"/>
        assertEquals(2, nodesOfType(network, SBML.NODETYPE_COMP_PORT).size());
        List<CyEdge> portEdges = edgesOfType(network, SBML.INTERACTION_COMP_SBASEREF_ID);
        assertEquals(2, portEdges.size());

        CyNode port = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_PORT_SID, "input__GFP");
        assertEquals("GFP", ReaderTestSupport.attribute(network, port, SBML.ATTR_COMP_IDREF));
        CyEdge edge = network.getAdjacentEdgeList(port, CyEdge.Type.OUTGOING).get(0);
        assertEquals(ReaderTestSupport.nodeById(context, "GFP"), edge.getTarget());
    }

    @Test
    void skipsSBaseRefWithUnknownTarget() throws Exception {
        // <comp:port comp:idRef="C5" comp:id="input__GFP__C5"/>, but there is no C5 in the model
        ConversionContext context = ReaderTestSupport.readResource(
                "/models/comp/Watanabe2014/test_replacement_4.xml", new CoreReader(), new CompReader());
        CyNetwork network = context.network();

        CyNode port = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_PORT_SID, "input__GFP__C5");
        assertEquals("C5", ReaderTestSupport.attribute(network, port, SBML.ATTR_COMP_IDREF));
        assertTrue(network.getAdjacentEdgeList(port, CyEdge.Type.OUTGOING).isEmpty());
    }
}
