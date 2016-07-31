package org.cy3sbml;

import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskMonitor;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * Test SBMLReaderTask
 */
public class SBMLReaderTaskTest {

    @Mock
    TaskMonitor taskMonitor;
    private SBMLReaderTask readerTask;
    private SBMLReaderTask readerTaskWithViewSupport;

    @Before
    public void setUp(){
        MockitoAnnotations.initMocks(this);
        final CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        final CyNetworkViewFactory networkViewFactory = new NetworkViewTestSupport().getNetworkViewFactory();

        String resource = SBMLCoreTest.TEST_MODEL_CORE_01;
        InputStream instream = TestUtils.class.getResourceAsStream(resource);
        String[] tokens = resource.split("/");
        String fileName = tokens[tokens.length-1];
        readerTask = new SBMLReaderTask(instream, fileName, networkFactory);
        readerTaskWithViewSupport = new SBMLReaderTask(instream, fileName, networkFactory, networkViewFactory, null, null);
    }


    @Test
    public void getError() throws Exception {
        readerTask.run(taskMonitor);
        Boolean error = readerTask.getError();
        assertFalse(error);
    }

    @Test
    public void getNetworks() throws Exception {
        readerTask.run(taskMonitor);
        CyNetwork [] networks = readerTask.getNetworks();
        assertNotNull(networks);
        assertEquals(2, networks.length);
    }

    @Test
    public void buildCyNetworkView() throws Exception {
        // create the network view from the factory
        readerTaskWithViewSupport.run(taskMonitor);
        CyNetwork [] networks = readerTaskWithViewSupport.getNetworks();
        CyNetwork network = networks[0];
        readerTaskWithViewSupport.buildCyNetworkView(network);
    }

    @Test
    public void cancel() throws Exception {
        readerTask.run(taskMonitor);
        readerTask.cancel();
    }

    @Test
    public void run() throws Exception {
        readerTask.run(taskMonitor);
    }

}