package org.cy3sbml.biomodel;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A model found by a BioModels search, with the information of the search response.
 * The details of the model (description, authors, publication) need a request per model,
 * see {@link BiomodelsQuery#performBiomodelQuery(String)}.
 *
 * @param id the BioModels id, e.g. {@code BIOMD0000000070} or {@code MODEL1204270001}
 * @param name the model name, empty if unknown
 * @param submissionDate the submission date (ISO 8601), empty if unknown
 * @param lastModified the date of the last modification (ISO 8601), empty if unknown
 */
public record BiomodelSummary(String id, String name, String submissionDate, String lastModified) {

    /**
     * Creates the summary from a model of the BioModels search response, null if the model
     * has no id.
     */
    static BiomodelSummary fromJson(JsonNode model) {
        String id = text(model, "id");
        if (id.isEmpty()) {
            return null;
        }
        return new BiomodelSummary(id, text(model, "name"), text(model, "submissionDate"), text(model, "lastModified"));
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return (value == null || !value.isTextual()) ? "" : value.asText();
    }
}
