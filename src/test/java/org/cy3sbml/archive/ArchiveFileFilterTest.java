package org.cy3sbml.archive;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.atomic.AtomicBoolean;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.util.StreamUtil;
import org.junit.jupiter.api.Test;

class ArchiveFileFilterTest {

    @Test
    void acceptsUriClosesTheStreamItOpens() throws Exception {
        AtomicBoolean closed = new AtomicBoolean();
        ByteArrayInputStream zip = new ByteArrayInputStream(new byte[] {'P', 'K', 0x3, 0x4, 0, 0}) {
            @Override
            public void close() throws IOException {
                closed.set(true);
                super.close();
            }
        };
        URLConnection connection = mock(URLConnection.class);
        when(connection.getInputStream()).thenReturn(zip);
        StreamUtil streamUtil = mock(StreamUtil.class);
        when(streamUtil.getURLConnection(any(URL.class))).thenReturn(connection);

        ArchiveFileFilter filter = new ArchiveFileFilter(streamUtil);

        assertTrue(filter.accepts(URI.create("file:///tmp/model.omex"), DataCategory.NETWORK));
        assertTrue(closed.get(), "the stream opened for the check is closed");
    }
}
