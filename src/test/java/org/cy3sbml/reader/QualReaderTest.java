package org.cy3sbml.reader;

import static org.cy3sbml.reader.ReaderTestSupport.nodeById;
import static org.cy3sbml.reader.ReaderTestSupport.nodesOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.cy3sbml.SBML;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.junit.jupiter.api.Test;

class QualReaderTest {

    @Test
    void readsQualitativeSpeciesAndTransitions() throws Exception {
        ConversionContext context = ReaderTestSupport.read("qual_01.xml", new CoreReader(), new QualReader());
        CyNetwork network = context.network();

        assertEquals(28, nodesOfType(network, SBML.NODETYPE_QUAL_SPECIES).size());
        assertEquals(26, nodesOfType(network, SBML.NODETYPE_QUAL_TRANSITION).size());
    }

    @Test
    void connectsTransitionToInputsAndOutputs() throws Exception {
        ConversionContext context = ReaderTestSupport.read("qual_01.xml", new CoreReader(), new QualReader());
        CyNetwork network = context.network();

        // <qual:transition qual:id="t1">
        //   <qual:input qual:thresholdLevel="1" qual:transitionEffect="none" qual:sign="positive"
        //       qual:qualitativeSpecies="nik" qual:id="theta_t1_nik"/>
        //   <qual:output qual:transitionEffect="assignmentLevel" qual:qualitativeSpecies="ikk"/>
        CyNode transition = nodeById(context, "t1");
        List<CyEdge> inputs = network.getConnectingEdgeList(transition, nodeById(context, "nik"), CyEdge.Type.DIRECTED);
        assertEquals(1, inputs.size());
        CyEdge input = inputs.get(0);
        assertEquals(transition, input.getSource());
        assertEquals(
                SBML.INTERACTION_QUAL_TRANSITION_INPUT, network.getRow(input).get(SBML.INTERACTION_ATTR, String.class));
        assertEquals("positive", network.getRow(input).get(SBML.ATTR_QUAL_SIGN, String.class));
        assertEquals(1, network.getRow(input).get(SBML.ATTR_QUAL_THRESHOLD_LEVEL, Integer.class));

        List<CyEdge> outputs =
                network.getConnectingEdgeList(transition, nodeById(context, "ikk"), CyEdge.Type.DIRECTED);
        assertEquals(1, outputs.size());
        assertEquals(
                SBML.INTERACTION_QUAL_TRANSITION_OUTPUT,
                network.getRow(outputs.get(0)).get(SBML.INTERACTION_ATTR, String.class));
        assertEquals(List.of(0, 1), network.getRow(transition).getList(SBML.ATTR_QUAL_RESULT_LEVELS, Integer.class));
    }
}
