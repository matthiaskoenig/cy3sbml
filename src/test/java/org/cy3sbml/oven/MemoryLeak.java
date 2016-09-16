package org.cy3sbml.oven;

import java.io.InputStream;
import org.mockito.MockitoAnnotations;

import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;

import org.cy3sbml.SBMLReaderTask;


/**
 * Testing the memory leak which occurs when creating multiple networks.
 *
 * This is related to
 * https://code.cytoscape.org/redmine/issues/3507#change-12785
 * and should be fixed in Cytoscape 3.5.
 *
 * No memory increase in the process.
 */
public class MemoryLeak {
    public static final String TEST_MODEL = "/models/unittests/core_01.xml";

    /**
     * Read the CyNetworks from given SBML file resource.
     */
    private void readNetwork(String resource) throws Exception {

        MockitoAnnotations.initMocks(this);
        final CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        final CyGroupFactory groupFactory = new GroupTestSupport().getGroupFactory();

        // read SBML
        InputStream instream = MemoryLeak.class.getResourceAsStream(resource);
        String[] tokens = resource.split("/");
        String fileName = tokens[tokens.length-1];

        // run the reader
        SBMLReaderTask readerTask = new SBMLReaderTask(instream, fileName, networkFactory, groupFactory);
        readerTask.run(null);
        
    }

    /**
     * Log the memory usage.
     */
    private static void logMemory(String info){
        System.gc();
        Runtime rt = Runtime.getRuntime();
        long usedMB = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        System.out.println(String.format("<%s> memory usage: %s MB", info, usedMB));
    }

    public static void main(String[] args) throws Exception {
        System.out.println("*** Memory Testing on NetworkReader ***");
        Integer N = 1000;
        MemoryLeak memLeak = new MemoryLeak();
        for (int k=0; k<N; k++){
            memLeak.readNetwork(TEST_MODEL);
            String info = String.format("R %s", k);
            logMemory(info);
        }
    }

}
