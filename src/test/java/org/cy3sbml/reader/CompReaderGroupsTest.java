package org.cy3sbml.reader;

import static org.cy3sbml.reader.ReaderTestSupport.attribute;
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
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.LoggerFactory;

/** Replacements of groups; isolated, since it changes the level of the CompReader logger. */
@Isolated
class CompReaderGroupsTest {

    /**
     * The replacements of a group have no edge from the group: a group is a Cytoscape group,
     * whose node has no edges. The replaced element is read without warning.
     */
    @Test
    void readsReplacementsOfGroupWithoutWarning() throws Exception {
        String sbml = """
                <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core" level="3" version="1"
                    xmlns:comp="http://www.sbml.org/sbml/level3/version1/comp/version1" comp:required="true"
                    xmlns:groups="http://www.sbml.org/sbml/level3/version1/groups/version1" groups:required="false">
                  <model id="top">
                    <listOfCompartments>
                      <compartment id="c" constant="true"/>
                    </listOfCompartments>
                    <groups:listOfGroups>
                      <groups:group groups:id="g" groups:kind="partonomy">
                        <groups:listOfMembers>
                          <groups:member groups:idRef="c"/>
                        </groups:listOfMembers>
                        <comp:listOfReplacedElements>
                          <comp:replacedElement comp:submodelRef="A" comp:idRef="g_inner"/>
                        </comp:listOfReplacedElements>
                      </groups:group>
                    </groups:listOfGroups>
                    <comp:listOfSubmodels>
                      <comp:submodel comp:id="A" comp:modelRef="mdA"/>
                    </comp:listOfSubmodels>
                  </model>
                  <comp:listOfModelDefinitions>
                    <comp:modelDefinition id="mdA">
                      <listOfCompartments>
                        <compartment id="c_inner" constant="true"/>
                      </listOfCompartments>
                      <groups:listOfGroups>
                        <groups:group groups:id="g_inner" groups:kind="partonomy">
                          <groups:listOfMembers>
                            <groups:member groups:idRef="c_inner"/>
                          </groups:listOfMembers>
                        </groups:group>
                      </groups:listOfGroups>
                    </comp:modelDefinition>
                  </comp:listOfModelDefinitions>
                </sbml>
                """;
        Logger logger = (Logger) LoggerFactory.getLogger(CompReader.class);
        // logback-test.xml logs errors of the CompReader only
        Level level = logger.getLevel();
        logger.setLevel(Level.WARN);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        // test classes run in parallel, the logger also records the warnings of other threads
        String thread = Thread.currentThread().getName();
        ConversionContext context;
        try {
            context =
                    ReaderTestSupport.readString(sbml.strip(), new CoreReader(), new CompReader(), new GroupsReader());
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(level);
        }

        assertEquals(
                List.of(),
                appender.list.stream()
                        .filter(e ->
                                e.getThreadName().equals(thread) && e.getLevel().isGreaterOrEqual(Level.WARN))
                        .map(ILoggingEvent::getFormattedMessage)
                        .toList());
        CyNetwork network = context.network();
        CyNode replacedElement =
                nodesOfType(network, SBML.NODETYPE_COMP_REPLACED_ELEMENT).get(0);
        assertEquals("g_inner", attribute(network, replacedElement, SBML.ATTR_COMP_IDREF));
        // the edge to the submodel
        assertEquals(
                1, network.getAdjacentEdgeList(replacedElement, CyEdge.Type.ANY).size());
    }
}
