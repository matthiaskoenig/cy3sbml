package org.cy3sbml.reader;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

/**
 * A model of an SBML file that becomes one network collection, with the document it is
 * defined in.
 */
record ModelSource(Model model, SBMLDocument document, Kind kind) {

    /** Where the model comes from. */
    enum Kind {
        /** The model of the file. */
        MAIN,
        /** A model definition of the comp package in the file. */
        MODEL_DEFINITION,
        /** The model of an external model definition, from another file. */
        EXTERNAL,
        /** The flattened hierarchical model. */
        FLAT
    }
}
