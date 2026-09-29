package org.cy3sbml.comp;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

/**
 * The result of resolving a model reference of the comp package: the model, or why
 * it could not be resolved.
 */
public sealed interface ModelResolution {

    /**
     * The referenced model and the document it is defined in, which is another
     * document for an external model definition.
     */
    record Resolved(Model model, SBMLDocument document) implements ModelResolution {}

    /** The model could not be resolved, with the reason for the user. */
    record Failed(String reason) implements ModelResolution {}
}
