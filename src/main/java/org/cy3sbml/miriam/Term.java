package org.cy3sbml.miriam;

/**
 * Single term in ontology
 */
public class Term {

    public String iri;
    public String label;
    public String[] description;
    public String[] synonyms;
    public String ontologyName;

    public Term(String iri, String label, String[] description, String[] synonyms, String ontologyName) {
        this.iri = iri;
        this.label = label;
        this.description = description;
        this.synonyms = synonyms;
        this.ontologyName = ontologyName;
    }

    public String toString(){
        String info = getClass().toString() + "<" + iri + ">";
        info += "\n" + label;
        info += "\n" + description;
        info += "\n" + synonyms;
        return info;
    }
}
