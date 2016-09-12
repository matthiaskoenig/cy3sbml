package org.cy3sbml.util.filter;

import org.sbml.jsbml.SBase;
import org.sbml.jsbml.util.filters.Filter;


/**
 * Filter testing if object is SBase.
 */
public class SBaseFilter implements Filter {
    @Override
    public boolean accepts(Object o) {
        if (o instanceof SBase) {
            return true;
        }
        return false;
    }
}
