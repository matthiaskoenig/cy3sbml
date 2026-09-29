package org.cy3sbml.reader;

import static org.cy3sbml.reader.ReaderTestSupport.nodeById;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.cy3sbml.SBML;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.distrib.DistribConstants;

class DistribReaderTest {
    private static final String MODEL = "/models/distrib/distrib_uncertainties.xml";

    private static String summary(CyNetwork network, CyNode node) {
        return network.getRow(node).get(SBML.ATTR_DISTRIB_UNCERTAINTY, String.class);
    }

    private static Integer count(CyNetwork network, CyNode node) {
        return network.getRow(node).get(SBML.ATTR_DISTRIB_UNCERTAINTY_COUNT, Integer.class);
    }

    @Test
    void writesTheUncertaintiesOfNodes() throws Exception {
        SBMLDocument document = ReaderTestSupport.readDocument(MODEL);
        ConversionContext context = ReaderTestSupport.read(document, new CoreReader(), new DistribReader());
        CyNetwork network = context.network();
        Model model = document.getModel();

        CyNode k1 = nodeById(context, "k1");
        assertEquals(
                "standardDeviation=sd_k1; range=[sd_k1, 10.0]; distribution=normal(1, sd_k1) (skew=0.1)",
                summary(network, k1));
        assertEquals(1, count(network, k1));
        assertEquals(2, count(network, nodeById(context, "S1")));
        assertEquals("median=1.5", summary(network, nodeById(context, "J0")));

        // elements without id: the kinetic law, the initial assignment
        CyNode law = context.nodeByMetaId(
                        model.getReaction("J0").getKineticLaw().getMetaId())
                .orElseThrow();
        assertEquals("externalParameter=http://www.uncertml.org/distributions/normal", summary(network, law));
        CyNode assignment =
                context.nodeByMetaId(model.getInitialAssignment(0).getMetaId()).orElseThrow();
        assertEquals("coefficientOfVariation=0.05", summary(network, assignment));

        // no uncertainty
        assertNull(summary(network, nodeById(context, "S2")));
        assertNull(count(network, nodeById(context, "S2")));
    }

    @Test
    void writesTheUncertaintiesOfSpeciesReferencesOnTheirEdges() throws Exception {
        SBMLDocument document = ReaderTestSupport.readDocument(MODEL);
        ConversionContext context = ReaderTestSupport.read(document, new CoreReader(), new DistribReader());
        CyNetwork network = context.network();
        Reaction reaction = document.getModel().getReaction("J0");

        CyEdge reactant = context.edgeOf(reaction.getReactant(0)).orElseThrow();
        CyEdge product = context.edgeOf(reaction.getProduct(0)).orElseThrow();
        assertEquals(
                "standardDeviation=0.5", network.getRow(reactant).get(SBML.ATTR_DISTRIB_UNCERTAINTY, String.class));
        assertEquals("mean=1.0", network.getRow(product).get(SBML.ATTR_DISTRIB_UNCERTAINTY, String.class));
        assertEquals(1, network.getRow(product).get(SBML.ATTR_DISTRIB_UNCERTAINTY_COUNT, Integer.class));
    }

    @Test
    void modelWithoutDistribGetsNoColumnsAndIsNotChanged() throws Exception {
        SBMLDocument document = ReaderTestSupport.readDocument("/models/unittests/core_01.xml");
        ConversionContext context = ReaderTestSupport.read(document, new CoreReader(), new DistribReader());

        assertNull(context.network().getDefaultNodeTable().getColumn(SBML.ATTR_DISTRIB_UNCERTAINTY));
        assertNull(context.network().getDefaultEdgeTable().getColumn(SBML.ATTR_DISTRIB_UNCERTAINTY));
        assertNull(document.getModel().getSpecies(0).getExtension(DistribConstants.shortLabel));
    }
}
