package org.cy3sbml.biomodel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biomodels.ws.SimpleModel;

/**
 * Tools to interact with BioModels.
 */
public class BioModelInterfaceTools {

    // string and html representations

    public static String getHTMLInformationForSimpleModels(List<String> modelIds,
                                                           List<String> selectedSimpleModels) throws IOException, ExecutionException, InterruptedException {
        String info = "";
        ArrayList<Biomodel> biomodelArrayList = BiomodelsQueryResult.getBiomodelsFromIds(modelIds);
        ;
        for (int i = 0; i < modelIds.size(); i++) {
            String modelId = modelIds.get(i);

            Biomodel model = biomodelArrayList.get(i);
            boolean modelIsSelected = false;
            for (int j = 0; j < selectedSimpleModels.size(); ++j) {
                String selectedId = selectedSimpleModels.get(j);
                if (modelId.equals(selectedId)) {
                    modelIsSelected = true;
                }
            }
            info += getHTMLInformationForSimpleModel(model, modelIsSelected);
            info += "<hr>";
        }
        return info;
    }


    public static String getHTMLInformationForSimpleModel(Biomodel simpleModel, boolean selected) {
        String id = simpleModel.getId();
        String name = simpleModel.getName();
        String publicationId = simpleModel.getPublicationIdentifier();
        String submissionIdentifier = simpleModel.getSubmissionIdentifier();
        //String dateModified = simpleModel.getLastModificationDateStr();
        String description = simpleModel.getDescription();
        String authors = simpleModel.getAuthors();
        String info;
        if (selected) {
            info = "<table><tr><td bgcolor=\"#339933\">&nbsp;&nbsp;&nbsp;<td><td>";
        } else {
            info = "<table><tr><td>&nbsp;&nbsp;&nbsp;<td><td>";
        }
        info += createHTMLTableHeader(selected) +
                createHTMLTableRow("ID", id) +
                createHTMLTableRow("Submission ID", submissionIdentifier) +
                createHTMLTableRow("Name", name) +
                createHTMLTableRow("Description", description.toString()) +
                createHTMLTableRow("Authors", authors) +
                createHTMLTableRow("Pubmed", createPubmedHTMLLink(publicationId)) +
                //createHTMLTableRow("modified", dateModified) +
                //FIXME: modified date info is not available in the response body

                "</table>" +
                "</td></tr></table>";
        return info;
    }

    private static String createHTMLTableHeader(boolean selected) {
        String border = "0";
        String header = String.format("<table border=%s>", border);
        return header;
    }

    private static String createHTMLTableRow(String attribute, String value) {
        return String.format(
                "<tr>" +
                        "	<td><b><font size=\"-1\">%s</font></b></td> " +
                        "	<td><font size=\"-1\">%s</font></b></td></tr>",
                attribute, value);
    }

    public static String createBioModelHTMLLink(String bioModelId) {
        return String.format(
                "<a href=\"http://www.ebi.ac.uk/biomodels-main/%s\" target=\"_blank\">%s</a>",
                bioModelId, bioModelId);
    }

    private static String createPubmedHTMLLink(String pubmedId) {
        return String.format(
                "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed?term=%s\" target=\"_blank\">%s</a>",
                pubmedId, pubmedId);
    }

}
