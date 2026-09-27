package org.cy3sbml.uniprot;

import java.util.List;

/**
 * A UniProt entry, parsed from the UniProtKB REST JSON
 * ({@code https://rest.uniprot.org/uniprotkb/<accession>.json}).
 */
public record UniprotEntry(
        String primaryAccession,
        String uniProtId,
        String fullName,
        List<String> ecNumbers,
        List<String> alternativeNames,
        String scientificName,
        String commonName,
        List<String> geneNames,
        List<String> functionComments,
        List<String> catalyticActivities,
        List<String> pathways) {}
