package org.cy3sbml.styles;

import java.util.LinkedList;
import java.util.List;

/**
 * Parent class of all StyleInfo.
 */
public abstract class StyleInfo {
    private String name;
    private String template;
    private List<Mapping> mappings;

    /** Constructor. */
    public StyleInfo(String name, String template){
        this.name = name;
        this.template = template;
        this.mappings = new LinkedList<>();
    }

    public String getTemplate() { return template; }

    public List<Mapping> getMappings() { return mappings; }

    public String getName() { return name; }

    public void setMappings(List<Mapping> mappings){
        this.mappings = mappings;
    }

    public abstract List<Mapping> createMappings();

}
