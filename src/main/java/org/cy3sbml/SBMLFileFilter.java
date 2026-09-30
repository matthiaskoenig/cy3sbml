package org.cy3sbml;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.util.SBMLUtil;
import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SBML Filter class.
 * Extends CyFileFilter for integration into the Cytoscape ImportHandler framework.
 */
public class SBMLFileFilter extends BasicCyFileFilter {
    private static final Logger logger = LoggerFactory.getLogger(SBMLFileFilter.class);
    /** The start of the namespaces of all SBML levels and versions. */
    private static final String SBML_NAMESPACE = "http://www.sbml.org/sbml/";
    /** The bytes of a stream that are checked, enough for a long header before the root element. */
    private static final int HEADER_BYTES = 64 * 1024;

    /**
     * The URI this filter accepted last on the thread. Cytoscape passes a reader
     * only the stream and the file name, but checks the file with
     * {@link #accepts(URI, DataCategory)} on the same thread just before, see
     * {@link #takeAcceptedUri(String)}.
     */
    // per filter: the reader factory takes the URI from the filter it was created with
    @SuppressWarnings("ThreadLocalUsage")
    private final ThreadLocal<URI> acceptedUri = new ThreadLocal<>();

    /**
     * Constructor.
     */
    public SBMLFileFilter(StreamUtil streamUtil) {
        super(
                new String[] {"xml", "sbml", ""},
                new String[] {
                    "text/xml", "application/rdf+xml", "application/xml", "text/plain", "text/sbml", "text/sbml+xml"
                },
                "SBML network reader (cy3sbml)",
                DataCategory.NETWORK,
                streamUtil);
    }

    /**
     * Indicates which URI the SBMLFileFilter accepts.
     */
    @Override
    public boolean accepts(URI uri, DataCategory category) {
        if (!category.equals(DataCategory.NETWORK)) {
            return false;
        }

        try (InputStream stream = streamUtil.getInputStream(uri.toURL())) {
            boolean accepted = accepts(stream, category);
            if (accepted) {
                acceptedUri.set(uri);
            }
            return accepted;
        } catch (IOException e) {
            logger.error("Error while creating stream from uri", e);
            return false;
        }
    }

    /**
     * Returns the URI this filter accepted last on the calling thread if its file
     * name is the given input name, and forgets it.
     * <p>
     * Cytoscape's reader manager calls {@link #accepts(URI, DataCategory)} and then
     * creates the reader for the stream of the URI on the same thread, with the file
     * name (file import) or the URL (URL import) as input name. The reader needs the location of the file to resolve the
     * relative sources of external model definitions.
     */
    public Optional<URI> takeAcceptedUri(String inputName) {
        URI uri = acceptedUri.get();
        acceptedUri.remove();
        if (uri == null || inputName == null || uri.getPath() == null) {
            return Optional.empty();
        }
        // the file name for a file, the URL for an import from a URL
        String path = uri.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        return inputName.equals(fileName) || inputName.equals(uri.toString()) ? Optional.of(uri) : Optional.empty();
    }

    /**
     * Indicates which streams the SBMLFileFilter accepts.
     */
    @Override
    public boolean accepts(InputStream stream, DataCategory category) {
        if (!category.equals(DataCategory.NETWORK)) {
            return false;
        }
        byte[] header;
        try {
            header = stream.readNBytes(HEADER_BYTES);
        } catch (IOException e) {
            logger.error("Error while checking header", e);
            return false;
        }
        try {
            return SBMLUtil.isSBML(new ByteArrayInputStream(header));
        } catch (XMLStreamException e) {
            // Cytoscape passes a stream filter a copy of the first kilobyte only, which can
            // end inside the root element: then the SBML namespace in the text decides
            return new String(header, StandardCharsets.UTF_8).contains(SBML_NAMESPACE);
        }
    }
}
