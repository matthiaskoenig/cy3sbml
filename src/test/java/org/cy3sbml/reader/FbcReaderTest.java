package org.cy3sbml.reader;

import static org.cy3sbml.reader.ReaderTestSupport.edgesOfType;
import static org.cy3sbml.reader.ReaderTestSupport.nodeById;
import static org.cy3sbml.reader.ReaderTestSupport.nodesOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.cy3sbml.SBML;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.FluxBound;

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

    @Test
    void readsFbcV1FluxBounds() throws Exception {
        ConversionContext context = ReaderTestSupport.readResource(
                "/models/fbc/JSBML_testcase_L3V1_fbcV1.xml", new CoreReader(), new FbcReader());
        CyNetwork network = context.network();

        // <fbc:fluxBound fbc:reaction="R16" fbc:operation="greaterEqual" fbc:value="0"/>
        // <fbc:fluxBound fbc:reaction="R16" fbc:operation="lessEqual" fbc:value="1000"/>
        CyNode reaction = nodeById(context, "R16");
        assertEquals("0.0", ReaderTestSupport.attribute(network, reaction, SBML.ATTR_FBC_LOWER_FLUX_BOUND));
        assertEquals("1000.0", ReaderTestSupport.attribute(network, reaction, SBML.ATTR_FBC_UPPER_FLUX_BOUND));
    }

    /**
     * Builds the model in Java (rather than parsing it from XML), the same way {@code
     * SBMLUtilTest.createSpeciesMapIncludesFbcVersion1ChargeAndFormula} does for a
     * species' plugin, to exercise {@code FbcReader.readFluxBounds} without needing an
     * XML fixture: constructs a {@code FBCModelPlugin} with {@code setPackageVersion(1)},
     * so {@code fbcModel.getVersion() == 1} takes the fbc v1 {@code FluxBound}-list
     * branch, with two {@code FluxBound} objects referencing the reaction directly rather
     * than via the fbc v2 reaction attributes {@link #readsFluxBoundsOfReactions} covers.
     */
    @Test
    @SuppressWarnings("deprecation") // FluxBound is deprecated in JSBML, but needed for fbc v1
    void readsFbcV1FluxBoundsFromAProgrammaticallyBuiltPluginVersion1() {
        SBMLDocument document = new SBMLDocument(3, 1);
        Model model = document.createModel("m1");
        Compartment compartment = model.createCompartment("c1");
        compartment.setConstant(true);
        Species species = model.createSpecies("s1", compartment);
        species.setBoundaryCondition(false);
        species.setHasOnlySubstanceUnits(false);
        species.setConstant(false);
        Reaction reaction = model.createReaction("r1");
        reaction.setReversible(false);
        reaction.setFast(false);
        SpeciesReference reactant = reaction.createReactant(species);
        reactant.setStoichiometry(1d);
        reactant.setConstant(true);

        // the plugin lookup must not depend on the fbc package version of the model
        FBCModelPlugin fbcModel = new FBCModelPlugin(model);
        fbcModel.setPackageVersion(1);
        model.addExtension(FBCConstants.namespaceURI_L3V1V1, fbcModel);

        FluxBound lower = fbcModel.createFluxBound("lb");
        lower.setReaction(reaction);
        lower.setOperation(FluxBound.Operation.GREATER_EQUAL);
        lower.setValue(0);
        FluxBound upper = fbcModel.createFluxBound("ub");
        upper.setReaction(reaction);
        upper.setOperation(FluxBound.Operation.LESS_EQUAL);
        upper.setValue(1000);

        ConversionContext context = ReaderTestSupport.read(document, new CoreReader(), new FbcReader());
        CyNetwork network = context.network();
        CyNode reactionNode = nodeById(context, "r1");

        assertEquals("0.0", ReaderTestSupport.attribute(network, reactionNode, SBML.ATTR_FBC_LOWER_FLUX_BOUND));
        assertEquals("1000.0", ReaderTestSupport.attribute(network, reactionNode, SBML.ATTR_FBC_UPPER_FLUX_BOUND));
    }

    @Test
    void skipsFbcV1FluxBoundsWithoutOperationOrReaction() throws Exception {
        String sbml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core"
                    xmlns:fbc="http://www.sbml.org/sbml/level3/version1/fbc/version1"
                    level="3" version="1" fbc:required="false">
                  <model id="m">
                    <listOfCompartments>
                      <compartment id="c" constant="true"/>
                    </listOfCompartments>
                    <listOfSpecies>
                      <species id="A" compartment="c" hasOnlySubstanceUnits="false" boundaryCondition="false"
                          constant="false"/>
                    </listOfSpecies>
                    <listOfReactions>
                      <reaction id="r1" reversible="false" fast="false">
                        <listOfReactants>
                          <speciesReference species="A" stoichiometry="1" constant="true"/>
                        </listOfReactants>
                      </reaction>
                    </listOfReactions>
                    <fbc:listOfFluxBounds>
                      <fbc:fluxBound fbc:id="noOperation" fbc:reaction="r1" fbc:value="5"/>
                      <fbc:fluxBound fbc:id="noReaction" fbc:reaction="missing" fbc:operation="lessEqual" fbc:value="7"/>
                      <fbc:fluxBound fbc:id="upper" fbc:reaction="r1" fbc:operation="lessEqual" fbc:value="10"/>
                    </fbc:listOfFluxBounds>
                  </model>
                </sbml>
                """;
        ConversionContext context = ReaderTestSupport.readString(sbml.strip(), new CoreReader(), new FbcReader());
        CyNetwork network = context.network();

        CyNode reaction = nodeById(context, "r1");
        assertNull(ReaderTestSupport.attribute(network, reaction, SBML.ATTR_FBC_LOWER_FLUX_BOUND));
        assertEquals("10.0", ReaderTestSupport.attribute(network, reaction, SBML.ATTR_FBC_UPPER_FLUX_BOUND));
    }
}
