package org.cy3sbml.comp;

import java.util.Optional;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBase;

/**
 * The comp references of the open documents: the resolver of the document of an element,
 * and the network collection of a model, to link a target to its node.
 */
public interface CompTargets {

    /** The resolver of the comp references of the document of the element, null if there is none. */
    SBaseRefResolver resolver(SBase sbase);

    /** The SUID of the root network created from the model, empty if there is none. */
    Optional<Long> rootNetwork(Model model);
}
