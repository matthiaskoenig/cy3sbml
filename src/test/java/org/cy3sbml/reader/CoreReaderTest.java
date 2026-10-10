package org.cy3sbml.reader;

import static org.cy3sbml.reader.ReaderTestSupport.edgesOfType;
import static org.cy3sbml.reader.ReaderTestSupport.nodeById;
import static org.cy3sbml.reader.ReaderTestSupport.nodesOfType;
import static org.junit.jupiter.api.Assertions.assertEquals;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import org.cy3sbml.SBML;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.LoggerFactory;

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

    /** Rules and assignments may set the stoichiometry of a species reference, which has no node. */
    @Test
    void ruleOfSpeciesReferenceIsNoWarning() throws Exception {
        String sbml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core" level="3" version="1">
                  <model id="m">
                    <listOfCompartments>
                      <compartment id="c" constant="true"/>
                    </listOfCompartments>
                    <listOfSpecies>
                      <species id="S" compartment="c" hasOnlySubstanceUnits="false" boundaryCondition="false"
                               constant="false"/>
                    </listOfSpecies>
                    <listOfInitialAssignments>
                      <initialAssignment symbol="S_stoich">
                        <math xmlns="http://www.w3.org/1998/Math/MathML"><cn> 2 </cn></math>
                      </initialAssignment>
                    </listOfInitialAssignments>
                    <listOfRules>
                      <assignmentRule variable="S_stoich">
                        <math xmlns="http://www.w3.org/1998/Math/MathML"><cn> 2 </cn></math>
                      </assignmentRule>
                    </listOfRules>
                    <listOfReactions>
                      <reaction id="R" reversible="false">
                        <listOfReactants>
                          <speciesReference id="S_stoich" species="S" constant="false"/>
                        </listOfReactants>
                      </reaction>
                    </listOfReactions>
                  </model>
                </sbml>
                """;
        Logger logger = (Logger) LoggerFactory.getLogger(CoreReader.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        // test classes run in parallel, the logger also records the warnings of other threads
        String thread = Thread.currentThread().getName();
        try {
            ReaderTestSupport.readString(sbml.strip(), new CoreReader());
        } finally {
            logger.detachAppender(appender);
        }

        assertEquals(
                List.of(),
                appender.list.stream()
                        .filter(e ->
                                e.getThreadName().equals(thread) && e.getLevel().isGreaterOrEqual(Level.WARN))
                        .map(ILoggingEvent::getFormattedMessage)
                        .toList());
    }

    @Test
    void registersTheEdgesOfSpeciesReferences() throws Exception {
        SBMLDocument document = ReaderTestSupport.readDocument("/models/distrib/distrib_uncertainties.xml");
        ConversionContext context = ReaderTestSupport.read(document, new CoreReader());
        CyNetwork network = context.network();
        Reaction reaction = document.getModel().getReaction("J0");

        // <speciesReference id="sr1" species="S1">, <speciesReference species="S2"> (no id)
        CyEdge reactant = context.edgeOf(reaction.getReactant(0)).orElseThrow();
        CyEdge product = context.edgeOf(reaction.getProduct(0)).orElseThrow();
        assertEquals(
                SBML.INTERACTION_REACTION_REACTANT, network.getRow(reactant).get(SBML.INTERACTION_ATTR, String.class));
        assertEquals(nodeById(context, "S1"), reactant.getTarget());
        assertEquals(
                SBML.INTERACTION_REACTION_PRODUCT, network.getRow(product).get(SBML.INTERACTION_ATTR, String.class));
        assertEquals(nodeById(context, "S2"), product.getTarget());
    }

    @Test
    void speciesReferenceEdgeHasTheIdOfTheSpeciesReference() throws Exception {
        SBMLDocument document = ReaderTestSupport.readDocument("/models/distrib/distrib_uncertainties.xml");
        ConversionContext context = ReaderTestSupport.read(document, new CoreReader());
        CyNetwork network = context.network();
        Reaction reaction = document.getModel().getReaction("J0");

        // <speciesReference id="sr1" species="S1">, <speciesReference species="S2"> (no id)
        CyEdge reactant = context.edgeOf(reaction.getReactant(0)).orElseThrow();
        CyEdge product = context.edgeOf(reaction.getProduct(0)).orElseThrow();
        assertEquals("sr1", network.getRow(reactant).get(SBML.ATTR_ID, String.class));
        assertEquals(null, network.getRow(product).get(SBML.ATTR_ID, String.class));
    }
}
