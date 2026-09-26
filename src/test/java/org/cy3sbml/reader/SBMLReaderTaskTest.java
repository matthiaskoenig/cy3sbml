package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.cy3sbml.SBMLReaderError;
import org.cytoscape.group.GroupTestSupport;
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
}
