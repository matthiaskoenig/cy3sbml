package org.cy3sbml.styles;

/**
 * Information storage for PassthroughMapping.
 */
public class MappingPassthrough extends Mapping{

    public MappingPassthrough(DataType dataType,
                              VisualPropertyKey property,
                              String attributeName,
                              String defaultValue){
        super(MappingType.PASSTHROUGH, dataType, property, attributeName, defaultValue);
    }

}