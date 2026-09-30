package org.cy3sbml.biomodel;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * The details of a BioModels model from the model endpoint of the BioModels REST API.
 *
 * @param id the curated BioModels id ({@code publicationId}, e.g. {@code BIOMD0000000012}),
 *     empty for a non-curated model
 * @param submissionIdentifier the submission id, e.g. {@code MODEL1234}
 * @param publicationIdentifier the accession of the publication (a PubMed id or a DOI),
 *     empty if unknown
 * @param name the model name, empty if unknown
 * @param description the description of the model (HTML), empty if unknown
 * @param authors the authors of the publication, comma separated
 */
public record Biomodel(
        String id,
        String submissionIdentifier,
        String publicationIdentifier,
        String name,
        String description,
        String authors) {
    private static final String SUBMISSION_ID = "submissionId";
    private static final String PUBLICATION = "publication";
    private static final String PUBLICATION_ID = "publicationId";
    private static final String NAME = "name";
    private static final String ACCESSION = "accession";
    private static final String DESCRIPTION = "description";
    private static final String AUTHORS = "authors";

    /**
     * Creates the biomodel from the JSON of the BioModels model endpoint.
     *
     * @throws IllegalArgumentException if the submission id or the publication is missing
     */
    public static Biomodel fromJson(JsonNode jsonObject) {
        String submissionIdentifier = requiredText(jsonObject, SUBMISSION_ID);
        JsonNode publicationObject = requiredObject(jsonObject, PUBLICATION);

        // not all fields exist
        String id = optionalText(jsonObject, PUBLICATION_ID, "");
        String name = optionalText(jsonObject, NAME, "");
        String publicationIdentifier = optionalText(publicationObject, ACCESSION, "");
        String description = optionalText(jsonObject, DESCRIPTION, "");

        List<String> authorsList = new ArrayList<>();
        JsonNode authorsArray = publicationObject.get(AUTHORS);
        if (authorsArray != null && authorsArray.isArray()) {
            for (JsonNode author : authorsArray) {
                String authorName = optionalText(author, NAME, null);
                if (authorName != null) {
                    authorsList.add(authorName);
                }
            }
        }
        return new Biomodel(
                id, submissionIdentifier, publicationIdentifier, name, description, String.join(", ", authorsList));
    }

    /**
     * Returns the text value of the given required field, throwing if it is missing.
     */
    private static String requiredText(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value.asText();
    }

    /**
     * Returns the given required object field, throwing if it is missing.
     */
    private static JsonNode requiredObject(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isObject()) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value;
    }

    /**
     * Returns the text value of the given field, or {@code defaultValue} if it is missing
     * or not a text value.
     */
    private static String optionalText(JsonNode node, String field, String defaultValue) {
        JsonNode value = node.get(field);
        return (value == null || !value.isTextual()) ? defaultValue : value.asText();
    }
}
