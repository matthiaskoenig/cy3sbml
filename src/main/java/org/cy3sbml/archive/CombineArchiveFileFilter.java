package org.cy3sbml.archive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The COMBINE archives (OMEX) the archive reader imports: zip files with one of the file
 * extensions of the COMBINE specification. Other zip files, e.g. Cytoscape sessions, are
 * not claimed.
 */
public class CombineArchiveFileFilter extends BasicCyFileFilter {
    private static final Logger logger = LoggerFactory.getLogger(CombineArchiveFileFilter.class);

    /** The file extensions of COMBINE archives. */
    public static final List<String> EXTENSIONS = List.of("omex", "sedx", "sbex", "cmex", "sbox", "neux", "phex");

    private static final byte[] ZIP_SIGNATURE = {'P', 'K', 0x3, 0x4};

    public CombineArchiveFileFilter(StreamUtil streamUtil) {
        super(
                EXTENSIONS.toArray(new String[0]),
                new String[] {"application/zip", "application/x-zip-compressed", "application/octet-stream"},
                "COMBINE archive reader (cy3sbml)",
                DataCategory.NETWORK,
                streamUtil);
    }

    /** An archive file: a COMBINE extension and the content of a zip file. */
    @Override
    public boolean accepts(URI uri, DataCategory category) {
        if (!category.equals(DataCategory.NETWORK) || !hasCombineExtension(uri)) {
            return false;
        }
        // StreamUtil.getInputStream unpacks zipped content, so the raw stream is read
        try (InputStream stream = streamUtil.getURLConnection(uri.toURL()).getInputStream()) {
            return accepts(stream, category);
        } catch (IOException e) {
            logger.error("Error while creating stream from uri", e);
            return false;
        }
    }

    /**
     * A stream that starts like a zip file. Cytoscape passes only the first bytes, so the
     * content of the zip cannot be checked.
     */
    @Override
    public boolean accepts(InputStream stream, DataCategory category) {
        if (!category.equals(DataCategory.NETWORK)) {
            return false;
        }
        try {
            return Arrays.equals(stream.readNBytes(ZIP_SIGNATURE.length), ZIP_SIGNATURE);
        } catch (IOException e) {
            logger.error("Error while checking the zip signature", e);
            return false;
        }
    }

    private static boolean hasCombineExtension(URI uri) {
        String path = uri.getPath();
        if (path == null) {
            return false;
        }
        String extension = path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return EXTENSIONS.contains(extension);
    }
}
