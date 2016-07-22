package org.cy3sbml.styles;

import org.cytoscape.view.model.VisualProperty;

/**
 * Information storage for PassthroughMapping.
 */
public class MappingPassthrough extends Mapping{

    public MappingPassthrough(DataType dataType,
                              VisualPropertyKey property,
                              String attributeName,
                              String defaultValue){
        super(MappingType.DISCRETE, dataType, property, attributeName, defaultValue);
    }

}