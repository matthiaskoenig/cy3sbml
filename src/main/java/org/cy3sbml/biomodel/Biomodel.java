package org.cy3sbml.biomodel;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores information for a given biomodel.
 */
public class Biomodel {
    public static final String SUBMISSION_ID = "submissionId";
    public static final String PUBLICATION = "publication";
    public static final String PUBLICATION_ID = "publicationId";
    public static final String NAME = "name";
    public static final String ACCESSION = "accession";
    public static final String DESCRIPTION = "description";
    public static final String AUTHORS = "authors";
    // Fetch information about a given model at a particular revision.

    private String id;
    private String submissionIdentifier;
    private String publicationIdentifier;
    private String name;
    private String description;
    private String authors;

    public Biomodel(JsonNode jsonObject) {

        submissionIdentifier = requiredText(jsonObject, SUBMISSION_ID);
        JsonNode publicationObject = requiredObject(jsonObject, PUBLICATION);

        // not all fields exist
        id = optionalText(jsonObject, PUBLICATION_ID, "");
        name = optionalText(jsonObject, NAME, "");
        publicationIdentifier = optionalText(publicationObject, ACCESSION, "");
        description = optionalText(jsonObject, DESCRIPTION, "");

        List<String> authorsList = new ArrayList<String>();
        JsonNode authorsArray = publicationObject.get(AUTHORS);
        if (authorsArray != null && authorsArray.isArray()) {
            for (JsonNode author : authorsArray) {
                String authorName = optionalText(author, NAME, null);
                if (authorName != null) {
                    authorsList.add(authorName);
                }
            }
        }
        authors = String.join(", ", authorsList);
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

    public String getId() {
        return id;
    }

    public String getPublicationIdentifier() {
        return publicationIdentifier;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthors() {
        return authors;
    }

    public String getInfo() {
        String text = getPublicationIdentifier();
        return text;
    }

    public String getSubmissionIdentifier() {
        return submissionIdentifier;
    }


}
