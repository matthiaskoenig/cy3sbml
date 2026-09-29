package org.cy3sbml.comp;

import java.util.List;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.comp.Submodel;

/**
 * The result of resolving an {@link org.sbml.jsbml.ext.comp.SBaseRef}: the element it
 * points to, or why it could not be resolved.
 */
public sealed interface SBaseRefResolution {

    /**
     * The target element in its model.
     *
     * @param path the submodels the nested references go through, outermost first
     */
    record Resolved(Model model, SBase target, List<Submodel> path) implements SBaseRefResolution {}

    /** The reference could not be resolved, with the reason for the user. */
    record Unresolved(String reason) implements SBaseRefResolution {}
}
