package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Optional;
import org.cy3sbml.reader.SBMLReaderTask;
import org.cytoscape.ding.NetworkViewTestSupport;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Test SBMLReaderTask
 */
@ExtendWith(MockitoExtension.class)
// Cytoscape's NetworkTestSupport stubs mocks it does not always use
@MockitoSettings(strictness = Strictness.LENIENT)
public class SBMLReaderTaskFactoryTaskTest {

    @Mock
    TaskMonitor taskMonitor;

    private SBMLReaderTask readerTask;
    private SBMLReaderTask readerTaskWithViewSupport;

    @BeforeEach
    public void setUp() {
        final CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        final CyNetworkViewFactory networkViewFactory = new NetworkViewTestSupport().getNetworkViewFactory();
        final CyGroupFactory groupFactory = new GroupTestSupport().getGroupFactory();

        String resource = SBMLCoreTest.TEST_MODEL_CORE_01;
        InputStream instream = TestUtils.class.getResourceAsStream(resource);
        String[] tokens = resource.split("/", -1);
        String fileName = tokens[tokens.length - 1];
        readerTask = new SBMLReaderTask(instream, fileName, networkFactory, groupFactory);
        readerTaskWithViewSupport = new SBMLReaderTask(
                instream, fileName, null, networkFactory, groupFactory, networkViewFactory, null, null, null, null);
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
        CyNetwork[] networks = readerTask.getNetworks();
        assertNotNull(networks);
        assertEquals(3, networks.length);
    }

    @Test
    public void buildCyNetworkView() throws Exception {
        // create the network view from the factory
        readerTaskWithViewSupport.run(taskMonitor);
        CyNetwork[] networks = readerTaskWithViewSupport.getNetworks();
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

    /**
     * Cytoscape passes the reader only the stream and the file name: the location
     * of the file comes from the file filter, which accepted it just before.
     */
    /** A stream that fails while it is read, e.g. of a URL whose connection drops. */
    static InputStream failingStream() {
        return new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("Connection reset");
            }
        };
    }

    /** An input that cannot be read gives a reader that reports the error, not no reader. */
    @Test
    public void unreadableInputGivesAReaderThatReportsTheError() {
        SBMLReaderTaskFactory factory =
                new SBMLReaderTaskFactory(new SBMLFileFilter(mock(StreamUtil.class)), null, null);

        TaskIterator iterator = factory.createTaskIterator(failingStream(), "model.xml");

        CyNetworkReader reader = (CyNetworkReader) iterator.next();
        SBMLReaderError error = assertThrows(SBMLReaderError.class, () -> reader.run(mock(TaskMonitor.class)));
        assertEquals("cy3sbml could not read 'model.xml': Connection reset", error.getMessage());
        assertEquals(0, reader.getNetworks().length);
    }

    @Test
    public void taskGetsTheLocationAcceptedByTheFilter() throws Exception {
        URI uri = URI.create("file:/models/comp/toy_top_level.xml");
        StreamUtil streamUtil = mock(StreamUtil.class);
        when(streamUtil.getInputStream(any(URL.class)))
                .thenAnswer(invocation -> TestUtils.class.getResourceAsStream(SBMLCoreTest.TEST_MODEL_CORE_01));
        SBMLFileFilter filter = new SBMLFileFilter(streamUtil);
        ServiceAdapter adapter = new ServiceAdapter(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new NetworkTestSupport().getNetworkFactory(),
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        SBMLReaderTaskFactory factory = new SBMLReaderTaskFactory(filter, adapter, null);

        assertTrue(filter.accepts(uri, DataCategory.NETWORK));
        TaskIterator iterator = factory.createTaskIterator(
                TestUtils.class.getResourceAsStream(SBMLCoreTest.TEST_MODEL_CORE_01), "toy_top_level.xml");

        SBMLReaderTask task = (SBMLReaderTask) iterator.next();
        assertEquals(Optional.of(uri), task.getLocation());
    }

    @Test
    public void taskHasNoLocationWithoutAcceptedUri() throws Exception {
        assertEquals(Optional.empty(), readerTask.getLocation());
    }
}
