package org.cy3sbml.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.nio.file.Path;
import org.cy3sbml.SBMLReaderError;
import org.cy3sbml.SBMLReaderTaskFactoryTaskTest;
import org.cy3sbml.ServiceAdapter;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.io.read.CyNetworkReader;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CombineArchiveReaderTaskFactoryTest {
    @TempDir
    Path temp;

    @Test
    void taskReadsTheArchiveWithTheServicesOfTheApp() throws Exception {
        ServiceAdapter adapter = new ServiceAdapter(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new NetworkTestSupport().getNetworkFactory(),
                new GroupTestSupport().getGroupFactory(),
                null,
                null,
                null,
                null,
                null,
                null);
        CombineArchiveReaderTaskFactory factory = new CombineArchiveReaderTaskFactory(
                new CombineArchiveFileFilter(null), adapter, null, new ArchiveDirectories(temp));

        try (InputStream stream =
                CombineArchiveReaderTaskFactoryTest.class.getResourceAsStream("/models/omex/single.omex")) {
            TaskIterator iterator = factory.createTaskIterator(stream, "single.omex");
            CombineArchiveReaderTask task = (CombineArchiveReaderTask) iterator.next();
            task.run(mock(TaskMonitor.class));

            assertEquals(3, task.getNetworks().length);
        }
    }

    /** An archive that cannot be read gives a reader that reports the error, not no reader. */
    @Test
    void unreadableArchiveGivesAReaderThatReportsTheError() {
        CombineArchiveReaderTaskFactory factory = new CombineArchiveReaderTaskFactory(
                new CombineArchiveFileFilter(null), null, null, new ArchiveDirectories(temp));
        InputStream failing = SBMLReaderTaskFactoryTaskTest.failingStream();

        CyNetworkReader reader = (CyNetworkReader)
                factory.createTaskIterator(failing, "model.omex").next();

        SBMLReaderError error = assertThrows(SBMLReaderError.class, () -> reader.run(mock(TaskMonitor.class)));
        assertEquals("cy3sbml could not read 'model.omex': Connection reset", error.getMessage());
    }
}
