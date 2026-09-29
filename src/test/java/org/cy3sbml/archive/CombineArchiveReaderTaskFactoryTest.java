package org.cy3sbml.archive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.nio.file.Path;
import org.cy3sbml.ServiceAdapter;
import org.cytoscape.group.GroupTestSupport;
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
                null,
                new NetworkTestSupport().getNetworkFactory(),
                new GroupTestSupport().getGroupFactory(),
                null,
                null,
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
}
