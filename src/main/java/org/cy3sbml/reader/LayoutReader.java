package org.cy3sbml.reader;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.LayoutConstants;
import org.sbml.jsbml.ext.layout.LayoutModelPlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads the layout package: registers the layouts of the model in the context. Every layout
 * becomes a layout network, created by {@link LayoutNetworkBuilder} after the subnetworks,
 * when all attributes of the nodes it copies are written.
 */
final class LayoutReader implements PackageReader {
    private static final Logger logger = LoggerFactory.getLogger(LayoutReader.class);

    @Override
    public void read(ConversionContext context, Model model) {
        logger.debug("<layout>");

        if (!(model.getExtension(LayoutConstants.shortLabel) instanceof LayoutModelPlugin layoutModel)
                || !layoutModel.isSetListOfLayouts()) {
            return;
        }
        for (Layout layout : layoutModel.getListOfLayouts()) {
            context.addLayout(layout);
        }
    }
}
