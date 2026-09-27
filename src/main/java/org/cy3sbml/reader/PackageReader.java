package org.cy3sbml.reader;

import org.sbml.jsbml.Model;

/**
 * Converts the objects of one SBML package of a model into nodes, edges and attributes.
 */
interface PackageReader {

    /**
     * Reads the package content of the model into the network of the context.
     * Does nothing if the model does not use the package.
     */
    void read(ConversionContext context, Model model);
}
