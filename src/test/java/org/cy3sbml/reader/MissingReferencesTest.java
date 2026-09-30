package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Test;

/**
 * Invalid models whose elements reference missing elements (all named {@code missing_*}).
 * The import skips the reference and reads the rest of the model, as it does for a species
 * with a missing compartment, instead of failing the whole import.
 */
class MissingReferencesTest {

    private static final String CORE = "http://www.sbml.org/sbml/level3/version1/core";

    /** Imports the model like Cytoscape does and returns the network with all nodes of the main model. */
    private static CyNetwork importModel(String sbml) throws Exception {
        SBMLReaderTask task = new SBMLReaderTask(
                new ByteArrayInputStream(sbml.strip().getBytes(StandardCharsets.UTF_8)),
                "missing_references.xml",
                new NetworkTestSupport().getNetworkFactory(),
                new GroupTestSupport().getGroupFactory());
        task.run(mock(TaskMonitor.class));
        assertFalse(task.getError());
        return Arrays.stream(task.getNetworks())
                .filter(n -> SBML.SUBNETWORK_ALL.equals(n.getRow(n).get(SBML.SUBNETWORK_ATTR, String.class)))
                .findFirst()
                .orElseThrow();
    }

    private static CyNode node(CyNetwork network, String id) {
        CyNode node = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, id);
        assertNotNull(node, id);
        return node;
    }

    private static CyNode nodeOfType(CyNetwork network, String nodeType) {
        CyNode node = AttributeUtil.getNodeByAttribute(network, SBML.NODETYPE_ATTR, nodeType);
        assertNotNull(node, nodeType);
        return node;
    }

    @Test
    void parameterWithMissingUnits() throws Exception {
        CyNetwork network = importModel("""
                <sbml xmlns="%s" level="3" version="1">
                  <model id="m">
                    <listOfParameters>
                      <parameter id="p" value="1" units="missing_unit" constant="true"/>
                    </listOfParameters>
                  </model>
                </sbml>
                """.formatted(CORE));

        assertEquals(
                0,
                network.getAdjacentEdgeList(node(network, "p"), CyEdge.Type.ANY).size());
    }

    @Test
    void eventAssignmentWithMissingVariable() throws Exception {
        CyNetwork network = importModel("""
                <sbml xmlns="%s" level="3" version="1">
                  <model id="m">
                    <listOfParameters>
                      <parameter id="p" value="1" constant="false"/>
                    </listOfParameters>
                    <listOfEvents>
                      <event id="e" useValuesFromTriggerTime="true">
                        <listOfEventAssignments>
                          <eventAssignment variable="missing_variable">
                            <math xmlns="http://www.w3.org/1998/Math/MathML"><ci>p</ci></math>
                          </eventAssignment>
                        </listOfEventAssignments>
                      </event>
                    </listOfEvents>
                  </model>
                </sbml>
                """.formatted(CORE));

        CyNode assignment = nodeOfType(network, SBML.NODETYPE_EVENT_ASSIGNMENT);
        assertEquals("missing_variable", network.getRow(assignment).get(SBML.ATTR_VARIABLE, String.class));
    }

    private static final String FBC_MODEL = """
            <sbml xmlns="%s" xmlns:fbc="http://www.sbml.org/sbml/level3/version1/fbc/version2"
                level="3" version="1" fbc:required="false">
              <model id="m" fbc:strict="false">
                <listOfCompartments>
                  <compartment id="c" constant="true"/>
                </listOfCompartments>
                <listOfSpecies>
                  <species id="s" compartment="c" hasOnlySubstanceUnits="false"
                      boundaryCondition="false" constant="false"/>
                </listOfSpecies>
                <listOfReactions>
                  <reaction id="r" reversible="false" fast="false">
                    <listOfReactants>
                      <speciesReference species="s" stoichiometry="1" constant="true"/>
                    </listOfReactants>
                    <fbc:geneProductAssociation>
                      <fbc:or>
                        <fbc:geneProductRef fbc:geneProduct="g"/>
                        <fbc:geneProductRef fbc:geneProduct="missing_gene_product"/>
                      </fbc:or>
                    </fbc:geneProductAssociation>
                  </reaction>
                </listOfReactions>
                <fbc:listOfObjectives fbc:activeObjective="obj">
                  <fbc:objective fbc:id="obj" fbc:type="maximize">
                    <fbc:listOfFluxObjectives>
                      <fbc:fluxObjective fbc:reaction="missing_reaction" fbc:coefficient="1"/>
                      <fbc:fluxObjective fbc:reaction="r" fbc:coefficient="2"/>
                    </fbc:listOfFluxObjectives>
                  </fbc:objective>
                </fbc:listOfObjectives>
                <fbc:listOfGeneProducts>
                  <fbc:geneProduct fbc:id="g" fbc:label="g" fbc:associatedSpecies="missing_species"/>
                </fbc:listOfGeneProducts>
              </model>
            </sbml>
            """.formatted(CORE);

    @Test
    void fluxObjectiveWithMissingReaction() throws Exception {
        CyNetwork network = importModel(FBC_MODEL);

        String column = String.format(SBML.ATTR_FBC_OBJECTIVE_TEMPLATE, "obj");
        assertEquals(2.0, network.getRow(node(network, "r")).get(column, Double.class));
    }

    @Test
    void geneProductWithMissingAssociatedSpecies() throws Exception {
        CyNetwork network = importModel(FBC_MODEL);

        // only the edge to the or node of the association, none to the missing species
        assertEquals(
                1,
                network.getAdjacentEdgeList(node(network, "g"), CyEdge.Type.ANY).size());
    }

    @Test
    void geneProductRefWithMissingGeneProduct() throws Exception {
        CyNetwork network = importModel(FBC_MODEL);

        // the or node links the existing gene product only
        CyNode or = nodeOfType(network, SBML.NODETYPE_FBC_OR);
        assertEquals(2, network.getAdjacentEdgeList(or, CyEdge.Type.ANY).size());
    }

    private static final String QUAL_MODEL = """
            <sbml xmlns="%s" xmlns:qual="http://www.sbml.org/sbml/level3/version1/qual/version1"
                level="3" version="1" qual:required="true">
              <model id="m">
                <listOfCompartments>
                  <compartment id="c" constant="true"/>
                </listOfCompartments>
                <qual:listOfQualitativeSpecies>
                  <qual:qualitativeSpecies qual:id="a" qual:compartment="c" qual:constant="false"/>
                  <qual:qualitativeSpecies qual:id="b" qual:compartment="missing_compartment"
                      qual:constant="false"/>
                </qual:listOfQualitativeSpecies>
                <qual:listOfTransitions>
                  <qual:transition qual:id="t">
                    <qual:listOfInputs>
                      <qual:input qual:qualitativeSpecies="a" qual:transitionEffect="none"/>
                      <qual:input qual:qualitativeSpecies="missing_input" qual:transitionEffect="none"/>
                    </qual:listOfInputs>
                    <qual:listOfOutputs>
                      <qual:output qual:qualitativeSpecies="b" qual:transitionEffect="assignmentLevel"/>
                      <qual:output qual:qualitativeSpecies="missing_output"
                          qual:transitionEffect="assignmentLevel"/>
                    </qual:listOfOutputs>
                  </qual:transition>
                </qual:listOfTransitions>
              </model>
            </sbml>
            """.formatted(CORE);

    @Test
    void qualitativeSpeciesWithMissingCompartment() throws Exception {
        CyNetwork network = importModel(QUAL_MODEL);

        // the compartment edge and the input edge
        assertEquals(
                2,
                network.getAdjacentEdgeList(node(network, "a"), CyEdge.Type.ANY).size());
        CyNode b = node(network, "b");
        assertEquals("missing_compartment", network.getRow(b).get(SBML.ATTR_COMPARTMENT, String.class));
        // only the output edge of the transition
        assertEquals(1, network.getAdjacentEdgeList(b, CyEdge.Type.ANY).size());
    }

    @Test
    void transitionWithMissingInputAndOutput() throws Exception {
        CyNetwork network = importModel(QUAL_MODEL);

        // the input a and the output b
        assertEquals(
                2,
                network.getAdjacentEdgeList(node(network, "t"), CyEdge.Type.ANY).size());
        assertTrue(network.containsEdge(node(network, "t"), node(network, "b")));
    }
}
