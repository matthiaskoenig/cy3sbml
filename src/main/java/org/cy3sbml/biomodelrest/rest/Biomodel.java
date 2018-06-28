package org.cy3sbml.biomodelrest.rest;

import org.json.JSONObject;

/**
 * Stores information for a given biomodel.
 */
public class Biomodel {
    // Fetch information about a given model at a particular revision.

    private JSONObject json;
    private String submissionIdentifier;
    private String publicationIdentifier;
    private String name;
    private String description;

    public Biomodel(JSONObject jsonObject){

        json = jsonObject;
        submissionIdentifier = json.getString("submissionIdentifier");
        publicationIdentifier = json.getString("publicationIdentifier");
        name = json.getString("name");
        description = json.getString("description");
    }

    public String getSubmissionIdentifier() {
        return submissionIdentifier;
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

    // TODO: handle format, files, history


}
