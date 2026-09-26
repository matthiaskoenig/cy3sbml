package org.cy3sbml.reader;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.layout.LayoutConstants;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the layout package. Layouts are not supported yet, so a warning is logged.
 */
final class LayoutReader implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(LayoutReader.class);

    /**
     * Creates the layouts stored in the layout extension.
     * TODO: implement
     */
    @Override
    public void read(ConversionContext context, Model model) {
        logger.debug("<layout>");

        LayoutModelPlugin layoutModel = (LayoutModelPlugin) model.getExtension(LayoutConstants.namespaceURI);

        if (layoutModel != null) {
            logger.warn("Layouts found, but not yet supported.");
        }
    }
}
