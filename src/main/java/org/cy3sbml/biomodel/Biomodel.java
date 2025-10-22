package org.cy3sbml.biomodel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    public Biomodel(JSONObject jsonObject) {

        submissionIdentifier = jsonObject.getString(SUBMISSION_ID);
        JSONObject publicationObject = jsonObject.getJSONObject(PUBLICATION);
        // not all fields exist
        try {
            id = jsonObject.getString(PUBLICATION_ID);
        } catch (JSONException e) {
            id = "";
        }
        try {
            name = jsonObject.getString(NAME);
        } catch (JSONException e) {
            name = "";
        }
        try {

            publicationIdentifier = publicationObject.getString(ACCESSION);
        } catch (JSONException e) {
            publicationIdentifier = "";
        }
        try {
            description = jsonObject.getString(DESCRIPTION);
        } catch (JSONException e) {
            description = "";
        }

        try {
            List<String> authorsList = new ArrayList<String>();
            JSONArray authorsArray = publicationObject.getJSONArray(AUTHORS);
            for (int i = 0; i < authorsArray.length(); i++) {
                JSONObject author = authorsArray.getJSONObject(i);
                String name = author.getString(NAME);
                authorsList.add(name);
            }
            authors = String.join(", ", authorsList);
        } catch (JSONException e) {
            authors = "";
        }
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
