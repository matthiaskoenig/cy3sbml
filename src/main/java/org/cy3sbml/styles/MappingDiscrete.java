package org.cy3sbml.styles;

import java.util.Map;

/**
 * Information storage for DiscreteMapping
 */
public class MappingDiscrete extends Mapping{
    private Map map;

    public MappingDiscrete(DataType dataType,
                           VisualPropertyKey property,
                           String attributeName,
                           String defaultValue,
                           Map map){
        super(MappingType.DISCRETE, dataType, property, attributeName, defaultValue);
        this.map = map;
    }

}