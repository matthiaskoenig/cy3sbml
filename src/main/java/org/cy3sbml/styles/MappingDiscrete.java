package org.cy3sbml.styles;

import java.util.Map;

/**
 * Information storage for DiscreteMapping
 */
public class MappingDiscrete extends Mapping {

    private Map<String, String> map;

    public MappingDiscrete(
            DataType dataType,
            VisualPropertyKey property,
            String attributeName,
            String defaultValue,
            Map<String, String> map) {
        super(MappingType.DISCRETE, dataType, property, attributeName, defaultValue);
        this.map = map;
    }

    public Map<String, String> getMap() {
        return map;
    }
}
