package org.cy3sbml;

import org.cytoscape.property.AbstractConfigDirPropsReader;
import org.cytoscape.property.CyProperty;

/**
 * The properties of cy3sbml, read from and saved to a file in the Cytoscape configuration
 * directory.
 */
class PropsReader extends AbstractConfigDirPropsReader {
    /**
     * @param name the name of the properties
     * @param fileName the name of the properties file in the configuration directory
     */
    PropsReader(String name, String fileName) {
        super(name, fileName, CyProperty.SavePolicy.CONFIG_DIR);
    }
}
