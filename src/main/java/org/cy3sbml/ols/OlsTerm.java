package org.cy3sbml.ols;

import java.util.List;

/** An OLS4 ontology term. */
public record OlsTerm(
        String iri,
        String label,
        String ontologyName,
        String oboId,
        List<String> synonyms,
        List<String> descriptions) {
}
