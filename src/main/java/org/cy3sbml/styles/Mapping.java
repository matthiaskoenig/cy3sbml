package org.cy3sbml.styles;

import org.cytoscape.view.model.VisualProperty;


/**
 * Parent class of mappings.
 */
public class Mapping {
    private MappingType mappingType;
    private DataType dataType;
    private VisualPropertyKey property;
    private String attributeName;
    private String defaultValue;


    public enum MappingType {
        DISCRETE, PASSTHROUGH, CONTINOUS
    }

    public enum DataType {
        integer, string
    }

    public Mapping(MappingType mappingType,
                   DataType dataType,
                   VisualPropertyKey property,
                   String attributeName,
                   String defaultValue){
        this.mappingType = mappingType;
        this.dataType = dataType;
        this.property = property;
        this.attributeName = attributeName;
        this.defaultValue = defaultValue;
    }


    public MappingType getMappingType(){
        return mappingType;
    }

    public DataType getDataType(){
        return dataType;
    }

    public VisualPropertyKey getVisualProperty(){
        return property;
    }

    public String getAttributeName(){
        return attributeName;
    }

    public String getDefaultValue(){
        return defaultValue;
    }
}

