package org.cy3sbml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
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
    private static final String SBML_XML_NAMESPACE = "http://www.sbml.org/sbml/";
    private static final int DEFAULT_LINES_TO_CHECK = 20;

    /**
     * The URI this filter accepted last on the thread. Cytoscape passes a reader
     * only the stream and the file name, but checks the file with
     * {@link #accepts(URI, DataCategory)} on the same thread just before, see
     * {@link #takeAcceptedUri(String)}.
     */
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
     * name as input name. The reader needs the location of the file to resolve the
     * relative sources of external model definitions.
     */
    public Optional<URI> takeAcceptedUri(String inputName) {
        URI uri = acceptedUri.get();
        acceptedUri.remove();
        if (uri == null || inputName == null || uri.getPath() == null) {
            return Optional.empty();
        }
        String path = uri.getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        return inputName.equals(fileName) ? Optional.of(uri) : Optional.empty();
    }

    /**
     * Indicates which streams the SBMLFileFilter accepts.
     */
    @Override
    public boolean accepts(InputStream stream, DataCategory category) {
        if (!category.equals(DataCategory.NETWORK)) {
            return false;
        }
        try {
            return checkHeader(stream);
        } catch (IOException e) {
            logger.error("Error while checking header", e);
            return false;
        }
    }

    /**
     * Checks if the header contains the SBML namespace definition.
     */
    private boolean checkHeader(InputStream stream) throws IOException {
        // the stream belongs to the caller, so the reader is not closed
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        int linesToCheck = DEFAULT_LINES_TO_CHECK;
        while (linesToCheck > 0) {
            String line = reader.readLine();
            if (line != null && line.contains(SBML_XML_NAMESPACE)) {
                return true;
            }
            linesToCheck--;
        }
        return false;
    }
}
