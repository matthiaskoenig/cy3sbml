package org.cy3sbml.reader;

import static org.cy3sbml.reader.ReaderTestSupport.nodeById;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import java.util.stream.Collectors;
import org.cy3sbml.SBML;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.junit.jupiter.api.Test;

class DerivedAttributesTest {

    private static final String SBML_MODEL = """
            <?xml version="1.0" encoding="UTF-8"?>
            <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core" level="3" version="1">
              <model id="m">
                <listOfCompartments>
                  <compartment id="c1" constant="true"/>
                  <compartment id="c2" constant="true"/>
                </listOfCompartments>
                <listOfSpecies>
                  <species id="A" compartment="c1" hasOnlySubstanceUnits="false" boundaryCondition="false" constant="false"/>
                  <species id="B" compartment="c2" hasOnlySubstanceUnits="false" boundaryCondition="false" constant="false"/>
                  <species id="E" compartment="c1" hasOnlySubstanceUnits="false" boundaryCondition="false" constant="false"/>
                  <species id="I" compartment="c1" hasOnlySubstanceUnits="false" boundaryCondition="false" constant="false"/>
                </listOfSpecies>
                <listOfReactions>
                  <reaction id="r1" reversible="true" fast="false">
                    <listOfReactants>
                      <speciesReference species="A" stoichiometry="1" constant="true"/>
                    </listOfReactants>
                    <listOfModifiers>
                      <modifierSpeciesReference species="E" sboTerm="SBO:0000459"/>
                    </listOfModifiers>
                  </reaction>
                  <reaction id="r2" reversible="false" fast="false">
                    <listOfProducts>
                      <speciesReference species="B" stoichiometry="1" constant="true"/>
                    </listOfProducts>
                    <listOfModifiers>
                      <modifierSpeciesReference species="I" sboTerm="SBO:0000020"/>
                      <modifierSpeciesReference species="E" sboTerm="SBO:0000019"/>
                    </listOfModifiers>
                  </reaction>
                </listOfReactions>
              </model>
            </sbml>
            """;

    @Test
    void addsCompartmentCodesInCompartmentOrder() throws Exception {
        ConversionContext context =
                ReaderTestSupport.readString(SBML_MODEL.strip(), new CoreReader(), new DerivedAttributes());
        CyNetwork network = context.network();

        assertEquals(1, compartmentCode(network, context, "A"));
        assertEquals(2, compartmentCode(network, context, "B"));
        assertEquals(1, compartmentCode(network, context, "E"));
        // reactions have no compartment
        assertNull(compartmentCode(network, context, "r1"));
    }

    @Test
    void addsExtendedNodeTypes() throws Exception {
        ConversionContext context =
                ReaderTestSupport.readString(SBML_MODEL.strip(), new CoreReader(), new DerivedAttributes());
        CyNetwork network = context.network();

        assertEquals(SBML.NODETYPE_REACTION_REVERSIBLE, extendedType(network, context, "r1"));
        assertEquals(SBML.NODETYPE_REACTION_IRREVERSIBLE, extendedType(network, context, "r2"));
        assertEquals(SBML.NODETYPE_SPECIES, extendedType(network, context, "A"));
    }

    @Test
    void addsExtendedInteractionTypesForModifiers() throws Exception {
        ConversionContext context =
                ReaderTestSupport.readString(SBML_MODEL.strip(), new CoreReader(), new DerivedAttributes());
        CyNetwork network = context.network();

        Map<String, String> modifierTypes =
                ReaderTestSupport.edgesOfType(network, SBML.INTERACTION_REACTION_MODIFIER).stream()
                        .collect(Collectors.toMap(
                                e -> edgeName(network, e),
                                e -> network.getRow(e).get(SBML.INTERACTION_ATTR_EXTENDED, String.class)));

        assertEquals(
                Map.of(
                        "r1->E", SBML.INTERACTION_REACTION_ACTIVATOR,
                        "r2->I", SBML.INTERACTION_REACTION_INHIBITOR,
                        "r2->E", SBML.INTERACTION_REACTION_MODIFIER),
                modifierTypes);
    }

    private static Integer compartmentCode(CyNetwork network, ConversionContext context, String id) {
        return network.getRow(nodeById(context, id)).get(SBML.ATTR_COMPARTMENT_CODE, Integer.class);
    }

    private static String extendedType(CyNetwork network, ConversionContext context, String id) {
        return network.getRow(nodeById(context, id)).get(SBML.NODETYPE_ATTR_EXTENDED, String.class);
    }

    private static String edgeName(CyNetwork network, CyEdge edge) {
        return network.getRow(edge.getSource()).get(SBML.ATTR_ID, String.class)
                + "->"
                + network.getRow(edge.getTarget()).get(SBML.ATTR_ID, String.class);
    }
}
