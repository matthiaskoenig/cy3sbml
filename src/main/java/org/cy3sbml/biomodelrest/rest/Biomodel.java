package org.cy3sbml.biomodelrest.rest;

import org.json.JSONException;
import org.json.JSONObject;
import uk.ac.ebi.biomodels.ws.SimpleModel;

import java.util.List;

/**
 * Stores information for a given biomodel.
 */
public class Biomodel {
    // Fetch information about a given model at a particular revision.

    private JSONObject json;
    private String id;
    private String submissionIdentifier;
    private String publicationIdentifier;
    private String name;
    private String description;

    public Biomodel(JSONObject jsonObject){

        json = jsonObject;
        submissionIdentifier = json.getString("submissionIdentifier");

        // not all fields exist
        try {
            name = json.getString("name");
        } catch (JSONException e) {
            name = "";
        }
        try {
            publicationIdentifier = json.getString("publicationIdentifier");
        } catch (JSONException e){
            publicationIdentifier = "";
        }
        try{
            description = json.getString("description");
        }catch (JSONException e) {
            description = "";
        }

        // id
        if (publicationIdentifier.length()>0){
            id = publicationIdentifier;
        } else {
            id = submissionIdentifier;
        }
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

    public String getInfo(){
        String text = getPublicationIdentifier();
        return text;
    }

    // TODO: handle format, files, history


    // HTML information
    // TODO: implement HTML information


    public static String getHTMLInformationForSimpleModel(SimpleModel simpleModel){
        boolean selected = false;
        return getHTMLInformationForSimpleModel(simpleModel, selected);
    }

    public static String getHTMLInformationForSimpleModel(SimpleModel simpleModel, boolean selected){
        String id = simpleModel.getId();
        String name = simpleModel.getName();
        String publicationId = simpleModel.getPublicationId();
        //String dateModified = simpleModel.getLastModificationDateStr();
        List<String> authors = simpleModel.getAuthors();
        List<String> encoders = simpleModel.getEncoders();
        String info;
        if (selected){
            info = "<table><tr><td bgcolor=\"#339933\">&nbsp;&nbsp;&nbsp;<td><td>";
        } else {
            info = "<table><tr><td>&nbsp;&nbsp;&nbsp;<td><td>";
        }
        info += createHTMLTableHeader(selected)+
                createHTMLTableRow("id", createBioModelHTMLLink(id)) +
                createHTMLTableRow("name", name) +
                createHTMLTableRow("authors", authors.toString()) +
                createHTMLTableRow("pubmed", createPubmedHTMLLink(publicationId)) +
                //createHTMLTableRow("modified", dateModified) +
                createHTMLTableRow("encoders", encoders.toString()) +
                "</table>" +
                "</td></tr></table>";
        return info;
    }

    private static String createHTMLTableHeader(boolean selected){
        String border = "0";
        String header = String.format("<table border=%s>", border);
        return header;
    }

    private static String createHTMLTableRow(String attribute, String value){
        return String.format(
                "<tr>" +
                        "	<td><b><font size=\"-1\">%s</font></b></td> " +
                        "	<td><font size=\"-1\">%s</font></b></td></tr>",
                attribute, value);
    }

    public static String createBioModelHTMLLink(String bioModelId){
        return String.format(
                "<a href=\"http://www.ebi.ac.uk/biomodels-main/%s\" target=\"_blank\">%s</a>",
                bioModelId, bioModelId);
    }

    private static String createPubmedHTMLLink(String pubmedId){
        return String.format(
                "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed?term=%s\" target=\"_blank\">%s</a>",
                pubmedId, pubmedId);
    }

    public static String getTextInformationForSimpleModel(SimpleModel simpleModel){
        String smId = simpleModel.getId();
        String smName = simpleModel.getName();
        String smPublicationId = simpleModel.getPublicationId();
        String smDateModified = simpleModel.getLastModificationDateStr();
        String info = String.format(
                "ID:\t %s \n" +
                        "NAME:\t %s \n" +
                        "PUBID:\t %s \n" +
                        "MODIFIED:\t %s \n",
                smId, smName, smPublicationId, smDateModified
        );
        return info;
    }


}
