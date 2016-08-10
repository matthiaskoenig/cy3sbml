package org.cy3sbml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;

import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.CyFileFilter;
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
	 * Constructor.
	 */
	public SBMLFileFilter(StreamUtil streamUtil) {
		super(
				new String[] { "xml", "sbml", ""},
				new String[] { "text/xml", "application/rdf+xml", "application/xml", "text/plain", "text/sbml", "text/sbml+xml" },
				"SBML network reader (cy3sbml)",
				DataCategory.NETWORK,
				streamUtil
		);
	}

	/**
	 * Indicates which URI the SBMLFileFilter accepts.
	 */
	@Override
	public boolean accepts(URI uri, DataCategory category) {
		if (!category.equals(DataCategory.NETWORK)) {
			return false;
		}

		try {
			// check for extension
			// String ext = FilenameUtils.getExtension(uri.toString());
			// extensions.contains(ext)
			return accepts(streamUtil.getInputStream(uri.toURL()), category);
		} catch (IOException e){
            logger.error("Error while creating stream from uri", e);
			return false;
		}
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
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
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
