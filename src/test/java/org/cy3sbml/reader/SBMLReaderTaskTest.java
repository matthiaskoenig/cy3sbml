package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLReaderError;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Test;

class SBMLReaderTaskTest {

    @Test
    void readerReportsInvalidSbml() {
        InputStream stream = new ByteArrayInputStream("<sbml>broken".getBytes(StandardCharsets.UTF_8));
        SBMLReaderTask task = new SBMLReaderTask(
                stream,
                "broken.xml",
                new NetworkTestSupport().getNetworkFactory(),
                new GroupTestSupport().getGroupFactory());
        TaskMonitor taskMonitor = mock(TaskMonitor.class);

        assertThrows(SBMLReaderError.class, () -> task.run(taskMonitor));

        assertTrue(task.getError());
        verify(taskMonitor).showMessage(eq(TaskMonitor.Level.ERROR), anyString());
        assertEquals(0, task.getNetworks().length);
    }

    @Test
    void readerHonorsTheXmlEncoding() throws Exception {
        String sbml = """
                <?xml version="1.0" encoding="ISO-8859-1"?>
                <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core" level="3" version="1">
                  <model id="m">
                    <listOfCompartments>
                      <compartment id="c" constant="true"/>
                    </listOfCompartments>
                    <listOfSpecies>
                      <species id="s1" name="M\u00e4hren \u00b5M" compartment="c" hasOnlySubstanceUnits="false"
                          boundaryCondition="false" constant="false"/>
                    </listOfSpecies>
                  </model>
                </sbml>
                """;
        InputStream stream = new ByteArrayInputStream(sbml.strip().getBytes(StandardCharsets.ISO_8859_1));
        SBMLReaderTask task = new SBMLReaderTask(
                stream,
                "latin1.xml",
                new NetworkTestSupport().getNetworkFactory(),
                new GroupTestSupport().getGroupFactory());

        task.run(mock(TaskMonitor.class));

        CyNetwork network = task.getNetworks()[0];
        CyNode species = AttributeUtil.getNodeByAttribute(network, SBML.ATTR_ID, "s1");
        assertEquals("M\u00e4hren \u00b5M", network.getRow(species).get(SBML.ATTR_NAME, String.class));
    }

    @Test
    void readerReadsCompModelWithReplacementsInSubmodels() throws Exception {
        String resource = "/models/comp/Watanabe2014/test_replacement_4.xml";
        TaskMonitor taskMonitor = mock(TaskMonitor.class);
        SBMLReaderTask task;
        try (InputStream stream = getClass().getResourceAsStream(resource)) {
            task = new SBMLReaderTask(
                    stream,
                    "test_replacement_4.xml",
                    new NetworkTestSupport().getNetworkFactory(),
                    new GroupTestSupport().getGroupFactory());
            task.run(taskMonitor);
        }

        assertFalse(task.getError());
        assertTrue(task.getNetworks().length > 0);
    }
}
