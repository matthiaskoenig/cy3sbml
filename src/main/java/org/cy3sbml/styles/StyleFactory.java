package org.cy3sbml.styles;

import java.util.LinkedList;
import java.util.List;

/**
 * Factory for creating visual styles depending on the current
 * SBML attributes and values.
 * This allows simple update of the styles with changed attributes.
 *
 * A template engine is used to fill in the styles with the given
 * information.
 *
 * Template only defines the default values.
 * The additional mappings are added.
 */
public abstract class StyleFactory {
    private String name;
    private String template;
    private List<Mapping> mappings;

    /** Constructor. */
    public StyleFactory(String name, String template){
        this.name = name;
        this.template = template;
        this.mappings = new LinkedList<>();
    }

    public void setMappings(List<Mapping> mappings){
        this.mappings = mappings;
    }

    /**
     * Creates the style in resource.
     * TODO: implement
     */
    public void createStyle(){
        // read template

        // modify the template with information
        // - set name
        // - add mappings

        // save the template
        System.out.println(String.format("Create style: <{}> with template <{}>", name, template));
        System.out.println("NOT IMPLEMENTED.");
        for (Mapping m: mappings){
            System.out.println(m);
        }
    }

    public abstract List<Mapping> createMappings();

    /**
     * Create all styles.
     * This creates/updates the styles based on the current settings in SBML.java.
     */
    public static void main(String[] args){
        List<StyleFactory> factories = new LinkedList<StyleFactory>();
        factories.add(new StyleFactory01());  // cy3sbml
        for (StyleFactory f: factories){
            f.createStyle();
        }
    }


}
