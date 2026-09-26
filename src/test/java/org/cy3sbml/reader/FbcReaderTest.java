package org.cy3sbml.reader;

import static org.cy3sbml.reader.ReaderTestSupport.edgesOfType;
import static org.cy3sbml.reader.ReaderTestSupport.nodeById;
import static org.cy3sbml.reader.ReaderTestSupport.nodesOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.cy3sbml.SBML;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.junit.jupiter.api.Test;

class FbcReaderTest {

    @Test
    void createsGeneProductAssociationNetwork() throws Exception {
        ConversionContext context = ReaderTestSupport.read("fbc_01.xml", new CoreReader(), new FbcReader());
        CyNetwork network = context.network();

        assertEquals(137, nodesOfType(network, SBML.NODETYPE_FBC_GENEPRODUCT).size());

        // <reaction id="R_ACALD"> with
        // <fbc:or><fbc:geneProductRef fbc:geneProduct="G_b0351"/><fbc:geneProductRef fbc:geneProduct="G_b1241"/>
        CyNode reaction = nodeById(context, "R_ACALD");
        List<CyEdge> reactionEdges = network.getAdjacentEdgeList(reaction, CyEdge.Type.INCOMING).stream()
                .filter(e -> SBML.INTERACTION_FBC_ASSOCIATION_REACTION.equals(
                        network.getRow(e).get(SBML.INTERACTION_ATTR, String.class)))
                .toList();
        assertEquals(1, reactionEdges.size());
        CyNode orNode = reactionEdges.get(0).getSource();
        assertEquals(SBML.NODETYPE_FBC_OR, ReaderTestSupport.attribute(network, orNode, SBML.NODETYPE_ATTR));

        Set<CyNode> geneProducts = network.getAdjacentEdgeList(orNode, CyEdge.Type.INCOMING).stream()
                .filter(e -> SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION.equals(
                        network.getRow(e).get(SBML.INTERACTION_ATTR, String.class)))
                .map(CyEdge::getSource)
                .collect(Collectors.toSet());
        assertEquals(Set.of(nodeById(context, "G_b0351"), nodeById(context, "G_b1241")), geneProducts);
    }

    @Test
    void readsFluxBoundsOfReactions() throws Exception {
        ConversionContext context = ReaderTestSupport.read("fbc_01.xml", new CoreReader(), new FbcReader());
        CyNetwork network = context.network();

        CyNode reaction = nodeById(context, "R_ACALD");
        assertEquals(
                "cobra_default_lb", ReaderTestSupport.attribute(network, reaction, SBML.ATTR_FBC_LOWER_FLUX_BOUND));
        assertEquals(
                "cobra_default_ub", ReaderTestSupport.attribute(network, reaction, SBML.ATTR_FBC_UPPER_FLUX_BOUND));
        // every reaction has an edge from its lower and its upper bound parameter
        int reactions = nodesOfType(network, SBML.NODETYPE_REACTION).size();
        assertEquals(
                2 * reactions,
                edgesOfType(network, SBML.INTERACTION_PARAMETER_REACTION).size());
    }
}
