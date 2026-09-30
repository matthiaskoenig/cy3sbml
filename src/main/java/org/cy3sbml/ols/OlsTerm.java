package org.cy3sbml.ols;

import java.util.List;

/** An OLS4 ontology term: its IRI, label, ontology, synonyms and descriptions. */
public record OlsTerm(
        String iri, String label, String ontologyName, List<String> synonyms, List<String> descriptions) {}
