package org.cy3sbml.sbml4humans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Sbml4HumansTaskTest {
    private static final URI REPORT = URI.create("https://sbml4humans.de/report?upload=abc");

    private final SBMLManager sbmlManager = new SBMLManager(mock(CyApplicationManager.class));
    private final Sbml4HumansClient client = mock(Sbml4HumansClient.class);
    private final List<String> opened = new ArrayList<>();
    private CyNetwork[] networks;

    /** Imports the comp model with a main model and three model definitions. */
    @BeforeEach
    void importModel() throws Exception {
        URL url = getClass().getResource("/models/unittests/01134-sbml-l3v1.xml");
        try (InputStream stream = url.openStream()) {
            SBMLReaderTask task = new SBMLReaderTask(
                    stream,
                    "01134-sbml-l3v1.xml",
                    url.toURI(),
                    new NetworkTestSupport().getNetworkFactory(),
                    new GroupTestSupport().getGroupFactory(),
                    new NetworkViewTestSupport().getNetworkViewFactory(),
                    null,
                    null,
                    null,
                    sbmlManager);
            task.run(mock(TaskMonitor.class));
            networks = task.getNetworks();
        }
        when(client.upload(any(), anyString(), any())).thenReturn(REPORT);
    }

    /** The base network of the collection of the model. */
    private CyNetwork network(String name) {
        return Arrays.stream(networks)
                .filter(n -> name.equals(n.getRow(n).get(CyNetwork.NAME, String.class)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no network " + name));
    }

    private Sbml4HumansTask task(CyNetwork network) {
        return new Sbml4HumansTask(sbmlManager, network, client, opened::add);
    }

    @Test
    void opensTheReportOfTheMainModel() throws Exception {
        task(network("case01134")).run(mock(TaskMonitor.class));

        verify(client).upload(any(), eq("./01134-sbml-l3v1.xml"), eq("case01134"));
        assertEquals(List.of(REPORT.toString()), opened);
    }

    /** The network of a comp model definition opens the report at the model definition. */
    @Test
    void opensTheReportOfAModelDefinition() throws Exception {
        CyNetwork definition = Arrays.stream(networks)
                .filter(n -> "moddef1"
                        .equals(sbmlManager
                                .getModel(((org.cytoscape.model.subnetwork.CySubNetwork) n)
                                        .getRootNetwork()
                                        .getSUID())
                                .getId()))
                .findFirst()
                .orElseThrow();

        task(definition).run(mock(TaskMonitor.class));

        verify(client).upload(any(), eq("./01134-sbml-l3v1.xml"), eq("moddef1"));
    }

    @Test
    void aFailedUploadIsTheErrorOfTheTask() throws Exception {
        when(client.upload(any(), anyString(), any())).thenThrow(new IOException("sbml4humans is down"));

        IOException error =
                assertThrows(IOException.class, () -> task(network("case01134")).run(mock(TaskMonitor.class)));

        assertEquals("sbml4humans is down", error.getMessage());
        assertTrue(opened.isEmpty());
    }

    @Test
    void aCancelledTaskOpensNoReport() throws Exception {
        Sbml4HumansTask task = task(network("case01134"));
        task.cancel();

        task.run(mock(TaskMonitor.class));

        verify(client, never()).upload(any(), anyString(), any());
        assertTrue(opened.isEmpty());
    }

    @Test
    void aNetworkWithoutSbmlIsAnError() {
        CyNetwork other = new NetworkTestSupport().getNetwork();

        IllegalStateException error =
                assertThrows(IllegalStateException.class, () -> task(other).run(mock(TaskMonitor.class)));

        assertEquals("The network has no SBML document.", error.getMessage());
    }
}
