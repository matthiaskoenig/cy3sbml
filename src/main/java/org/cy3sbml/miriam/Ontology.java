package org.cy3sbml.miriam;

/**
 * Class for parsing ontologies from OLS Json
 */
public class Ontology {

    public String ontologyId;
    public String loaded;
    public String updated;

    public Ontology(String ontologyId, String loaded, String updated) {
        this.ontologyId = ontologyId;
        this.loaded = loaded;
        this.updated = updated;
    }
}
