package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.util.StreamUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Testing the SBML file filter.
 */
public class SBMLFileFilterTest {
    private SBMLFileFilter filter;

    @BeforeEach
    public void setUp() {
        filter = new SBMLFileFilter(null);
    }

    @AfterEach
    public void tearDown() {
        filter = null;
    }

    @Test
    public void accept() throws Exception {
        InputStream instream = getClass().getResourceAsStream(SBMLCoreTest.TEST_MODEL_CORE_01);
        boolean accepted = filter.accepts(instream, DataCategory.NETWORK);
        assertTrue(accepted);
        accepted = filter.accepts(instream, DataCategory.TABLE);
        assertFalse(accepted);
    }

    @Test
    public void getExtensions() throws Exception {
        Set<String> extensions = filter.getExtensions();
        assertTrue(extensions.contains("sbml"));
        assertTrue(extensions.contains("xml"));
        assertTrue(extensions.contains(""));
    }

    @Test
    public void getContentTypes() throws Exception {
        Set<String> contentTypes = filter.getContentTypes();
        assertTrue(contentTypes.contains("text/xml"));
        assertTrue(contentTypes.contains("application/xml"));
    }

    @Test
    public void getDescription() throws Exception {
        String description = filter.getDescription();
        assertNotNull(description);
    }

    @Test
    public void getDataCategory() throws Exception {
        DataCategory category = filter.getDataCategory();
        assertEquals(DataCategory.NETWORK, category);
    }

    private static final URI MODEL_URI = URI.create("file:/models/comp/toy_top_level.xml");

    /** A filter whose stream util returns the content for every URL. */
    private static SBMLFileFilter filterReading(String content) throws Exception {
        StreamUtil streamUtil = mock(StreamUtil.class);
        when(streamUtil.getInputStream(any(URL.class)))
                .thenAnswer(invocation -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        return new SBMLFileFilter(streamUtil);
    }

    private static SBMLFileFilter sbmlFilter() throws Exception {
        return filterReading(
                "<sbml xmlns=\"http://www.sbml.org/sbml/level3/version1/core\" level=\"3\" version=\"1\"/>");
    }

    @Test
    public void acceptedUriIsTakenOnceForItsFileName() throws Exception {
        SBMLFileFilter filter = sbmlFilter();
        assertTrue(filter.accepts(MODEL_URI, DataCategory.NETWORK));

        assertEquals(Optional.of(MODEL_URI), filter.takeAcceptedUri("toy_top_level.xml"));
        assertEquals(Optional.empty(), filter.takeAcceptedUri("toy_top_level.xml"));
    }

    @Test
    public void acceptedUriIsNotTakenForAnotherFileName() throws Exception {
        SBMLFileFilter filter = sbmlFilter();
        assertTrue(filter.accepts(MODEL_URI, DataCategory.NETWORK));

        assertEquals(Optional.empty(), filter.takeAcceptedUri("other.xml"));
        // the URI is cleared, also if it did not match
        assertEquals(Optional.empty(), filter.takeAcceptedUri("toy_top_level.xml"));
    }

    @Test
    public void notAcceptedUriIsNotStored() throws Exception {
        SBMLFileFilter filter = filterReading("<html/>");
        assertFalse(filter.accepts(MODEL_URI, DataCategory.NETWORK));

        assertEquals(Optional.empty(), filter.takeAcceptedUri("toy_top_level.xml"));
    }

    @Test
    public void acceptedUriIsOnlySeenByTheAcceptingThread() throws Exception {
        SBMLFileFilter filter = sbmlFilter();
        assertTrue(filter.accepts(MODEL_URI, DataCategory.NETWORK));

        AtomicReference<Optional<URI>> taken = new AtomicReference<>();
        Thread thread = new Thread(() -> taken.set(filter.takeAcceptedUri("toy_top_level.xml")));
        thread.start();
        thread.join();
        assertEquals(Optional.empty(), taken.get());
        assertEquals(Optional.of(MODEL_URI), filter.takeAcceptedUri("toy_top_level.xml"));
    }
}
