package org.cy3sbml.chebi;

/**
 * A ChEBI compound, parsed from the ChEBI public backend REST JSON
 * ({@code https://www.ebi.ac.uk/chebi/backend/api/public/compound/<id>/}).
 */
public record ChebiCompound(
        String chebiId,
        String name,
        String formula,
        String charge,
        String mass) {
}
