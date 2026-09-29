package org.cy3sbml.archive;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.util.StreamUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CombineArchiveFileFilterTest {
    private CombineArchiveFileFilter filter;

    @TempDir
    Path directory;

    @BeforeEach
    void setUp() throws Exception {
        StreamUtil streamUtil = mock(StreamUtil.class);
        when(streamUtil.getURLConnection(any(URL.class)))
                .thenAnswer(invocation -> ((URL) invocation.getArgument(0)).openConnection());
        filter = new CombineArchiveFileFilter(streamUtil);
    }

    private static InputStream archive() {
        return CombineArchiveFileFilterTest.class.getResourceAsStream("/models/omex/single.omex");
    }

    /** A copy of the single archive with the given file name. */
    private URI copy(String fileName) throws Exception {
        Path file = directory.resolve(fileName);
        try (InputStream stream = archive()) {
            Files.copy(stream, file);
        }
        return file.toUri();
    }

    @Test
    void acceptsZipStreams() throws Exception {
        try (InputStream stream = archive()) {
            assertTrue(filter.accepts(stream, DataCategory.NETWORK));
        }
        assertFalse(filter.accepts(
                new ByteArrayInputStream("<sbml/>".getBytes(StandardCharsets.UTF_8)), DataCategory.NETWORK));
        try (InputStream stream = archive()) {
            assertFalse(filter.accepts(stream, DataCategory.TABLE));
        }
    }

    @Test
    void acceptsArchivesWithACombineExtension() throws Exception {
        assertTrue(filter.accepts(copy("model.omex"), DataCategory.NETWORK));
        assertTrue(filter.accepts(copy("model.sedx"), DataCategory.NETWORK));
        assertTrue(filter.accepts(copy("UPPER_CASE.OMEX"), DataCategory.NETWORK));
    }

    @Test
    void doesNotClaimOtherZipFiles() throws Exception {
        assertFalse(filter.accepts(copy("model.zip"), DataCategory.NETWORK));
        assertFalse(filter.accepts(copy("session.cys"), DataCategory.NETWORK));
        assertFalse(filter.accepts(copy("model.xml"), DataCategory.NETWORK));
    }

    @Test
    void doesNotAcceptAnOmexExtensionWithoutZip() throws Exception {
        Path file = directory.resolve("fake.omex");
        Files.writeString(file, "<sbml/>");
        assertFalse(filter.accepts(file.toUri(), DataCategory.NETWORK));
    }
}
