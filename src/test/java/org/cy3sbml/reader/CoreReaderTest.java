package org.cy3sbml.reader;

import static org.cy3sbml.reader.ReaderTestSupport.edgesOfType;
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

    @Test
    void eventDelayAndPriorityMathGetTheirOwnInteractionTypes() throws Exception {
        String sbml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core" level="3" version="1">
                  <model id="events">
                    <listOfParameters>
                      <parameter id="d" value="1" constant="true"/>
                      <parameter id="p" value="2" constant="true"/>
                      <parameter id="x" value="0" constant="false"/>
                    </listOfParameters>
                    <listOfEvents>
                      <event id="e1" useValuesFromTriggerTime="true">
                        <trigger initialValue="false" persistent="true">
                          <math xmlns="http://www.w3.org/1998/Math/MathML">
                            <apply><gt/><csymbol encoding="text"
                              definitionURL="http://www.sbml.org/sbml/symbols/time">t</csymbol><cn>1</cn></apply>
                          </math>
                        </trigger>
                        <priority>
                          <math xmlns="http://www.w3.org/1998/Math/MathML"><ci>p</ci></math>
                        </priority>
                        <delay>
                          <math xmlns="http://www.w3.org/1998/Math/MathML"><ci>d</ci></math>
                        </delay>
                        <listOfEventAssignments>
                          <eventAssignment variable="x">
                            <math xmlns="http://www.w3.org/1998/Math/MathML"><cn>1</cn></math>
                          </eventAssignment>
                        </listOfEventAssignments>
                      </event>
                    </listOfEvents>
                  </model>
                </sbml>
                """;
        ConversionContext context = ReaderTestSupport.readString(sbml, new CoreReader());
        CyNetwork network = context.network();

        assertEquals(1, edgesOfType(network, SBML.INTERACTION_DELAY_EVENT).size());
        assertEquals(1, edgesOfType(network, SBML.INTERACTION_PRIORITY_EVENT).size());
    }
}
