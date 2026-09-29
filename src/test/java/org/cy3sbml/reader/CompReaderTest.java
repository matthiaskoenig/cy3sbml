package org.cy3sbml.reader;

import static org.cy3sbml.reader.ReaderTestSupport.attribute;
import static org.cy3sbml.reader.ReaderTestSupport.edgesOfType;
import static org.cy3sbml.reader.ReaderTestSupport.nodesOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompSBMLDocumentPlugin;

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

    /** The main model of references.xml: submodel A (mdA) with deletions, replacements, ports. */
    private static ConversionContext readReferences() throws Exception {
        return ReaderTestSupport.readResource("/models/comp/unit/references.xml", new CoreReader(), new CompReader());
    }

    private static CyNode node(CyNetwork network, String attribute, String value) {
        return AttributeUtil.getNodeByAttribute(network, attribute, value);
    }

    private static List<CyNode> targets(CyNetwork network, CyNode source, String interaction) {
        return network.getAdjacentEdgeList(source, CyEdge.Type.OUTGOING).stream()
                .filter(e -> interaction.equals(network.getRow(e).get(SBML.INTERACTION_ATTR, String.class)))
                .map(CyEdge::getTarget)
                .toList();
    }

    @Test
    void submodelHasEdgesToItsDeletions() throws Exception {
        ConversionContext context = readReferences();
        CyNetwork network = context.network();

        CyNode submodel = ReaderTestSupport.nodeById(context, "A");
        CyNode deletion = ReaderTestSupport.nodeById(context, "del_meta");

        assertEquals(
                7,
                targets(network, submodel, SBML.INTERACTION_COMP_SBASE_DELETION).size());
        assertTrue(
                targets(network, submodel, SBML.INTERACTION_COMP_SBASE_DELETION).contains(deletion));
    }

    @Test
    void deletionHasTheResolvedTarget() throws Exception {
        ConversionContext context = readReferences();
        CyNetwork network = context.network();

        CyNode deletion = ReaderTestSupport.nodeById(context, "del_nested");

        assertEquals("mdB", attribute(network, deletion, SBML.ATTR_COMP_TARGET_MODEL));
        assertEquals("y", attribute(network, deletion, SBML.ATTR_COMP_TARGET_ID));
        assertEquals("parameter", attribute(network, deletion, SBML.ATTR_COMP_TARGET_TYPE));
        assertNotNull(attribute(network, deletion, SBML.ATTR_COMP_TARGET_METAID));
        assertEquals(SBML.COMP_RESOLVED, attribute(network, deletion, SBML.ATTR_COMP_RESOLUTION));
        assertEquals("idRef=B > idRef=y", attribute(network, deletion, SBML.ATTR_COMP_SBASEREF));
    }

    @Test
    void unresolvedDeletionHasTheReason() throws Exception {
        ConversionContext context = readReferences();
        CyNetwork network = context.network();

        CyNode deletion = ReaderTestSupport.nodeById(context, "del_missing");

        assertTrue(attribute(network, deletion, SBML.ATTR_COMP_RESOLUTION).contains("missing"));
        assertNull(attribute(network, deletion, SBML.ATTR_COMP_TARGET_ID));
    }

    @Test
    void replacedElementHasConversionFactorAndEdgeToItsSubmodel() throws Exception {
        ConversionContext context = readReferences();
        CyNetwork network = context.network();

        CyNode species = ReaderTestSupport.nodeById(context, "S2");
        List<CyNode> replaced = targets(network, species, SBML.INTERACTION_COMP_SBASE_REPLACED_ELEMENT);
        assertEquals(1, replaced.size());
        CyNode replacedElement = replaced.get(0);

        assertEquals("cf", attribute(network, replacedElement, SBML.ATTR_COMP_CONVERSION_FACTOR));
        assertEquals("A", attribute(network, replacedElement, SBML.ATTR_COMP_SUBMODELREF));
        assertEquals("x", attribute(network, replacedElement, SBML.ATTR_COMP_TARGET_ID));
        assertEquals("mdA", attribute(network, replacedElement, SBML.ATTR_COMP_TARGET_MODEL));
        assertEquals(
                List.of(ReaderTestSupport.nodeById(context, "A")),
                targets(network, replacedElement, SBML.INTERACTION_COMP_SBASEREF_SUBMODEL));
    }

    @Test
    void replacedElementOfDeletionPointsToTheDeletion() throws Exception {
        ConversionContext context = readReferences();
        CyNetwork network = context.network();

        CyNode species = ReaderTestSupport.nodeById(context, "S3");
        CyNode replacedElement = targets(network, species, SBML.INTERACTION_COMP_SBASE_REPLACED_ELEMENT)
                .get(0);

        assertEquals("del_unit", attribute(network, replacedElement, SBML.ATTR_COMP_DELETION));
        assertEquals(
                List.of(ReaderTestSupport.nodeById(context, "del_unit")),
                targets(network, replacedElement, SBML.INTERACTION_COMP_SBASE_DELETION));
    }

    @Test
    void replacedByFollowsNestedReference() throws Exception {
        ConversionContext context = readReferences();
        CyNetwork network = context.network();

        CyNode parameter = ReaderTestSupport.nodeById(context, "q");
        CyNode replacedBy = targets(network, parameter, SBML.INTERACTION_COMP_SBASE_REPLACED_BY)
                .get(0);

        assertEquals("submodelRef=A > idRef=B > idRef=y", attribute(network, replacedBy, SBML.ATTR_COMP_SBASEREF));
        assertEquals("mdB", attribute(network, replacedBy, SBML.ATTR_COMP_TARGET_MODEL));
        assertEquals("y", attribute(network, replacedBy, SBML.ATTR_COMP_TARGET_ID));
    }

    /** Ports and unit definitions have their own namespaces and do not hide elements with the same id. */
    @Test
    void portIdDoesNotHideSpeciesWithTheSameId() throws Exception {
        ConversionContext context = readReferences();

        CyNode species = ReaderTestSupport.nodeById(context, "S");

        assertEquals(SBML.NODETYPE_SPECIES, attribute(context.network(), species, SBML.NODETYPE_ATTR));
    }

    @Test
    void portInModelDefinitionLinksItsTarget() throws Exception {
        SBMLDocument document = ReaderTestSupport.readDocument("/models/comp/unit/references.xml");
        Model mdA =
                ((CompSBMLDocumentPlugin) document.getExtension(CompConstants.shortLabel)).getModelDefinition("mdA");
        ConversionContext context = ReaderTestSupport.read(document, mdA, new CoreReader(), new CompReader());
        CyNetwork network = context.network();

        CyNode port = node(network, SBML.ATTR_PORT_SID, "pa_x");

        assertEquals(
                List.of(ReaderTestSupport.nodeById(context, "x")),
                targets(network, port, SBML.INTERACTION_COMP_SBASEREF_ID));
        assertEquals(SBML.COMP_RESOLVED, attribute(network, port, SBML.ATTR_COMP_RESOLUTION));
    }
}
