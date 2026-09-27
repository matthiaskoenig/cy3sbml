package org.cy3sbml.biomodel;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores information for a given biomodel.
 */
public record Biomodel(
        String id,
        String submissionIdentifier,
        String publicationIdentifier,
        String name,
        String description,
        String authors) {
    public static final String SUBMISSION_ID = "submissionId";
    public static final String PUBLICATION = "publication";
    public static final String PUBLICATION_ID = "publicationId";
    public static final String NAME = "name";
    public static final String ACCESSION = "accession";
    public static final String DESCRIPTION = "description";
    public static final String AUTHORS = "authors";

    /**
     * Creates the biomodel from the JSON of the BioModels model endpoint.
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
