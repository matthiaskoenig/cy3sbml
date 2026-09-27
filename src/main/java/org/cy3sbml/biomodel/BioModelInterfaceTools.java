package org.cy3sbml.biomodel;

import java.util.List;
import java.util.Map;

/**
 * Tools to interact with BioModels.
 */
public class BioModelInterfaceTools {

    // string and html representations

    public static String getHTMLInformationForSimpleModels(
            BiomodelsQuery query, List<String> modelIds, List<String> selectedSimpleModels) {
        String info = "";
        Map<String, Biomodel> biomodels = BiomodelsQueryResult.getBiomodelsFromIds(modelIds, query);
        for (String modelId : modelIds) {
            Biomodel model = biomodels.get(modelId);
            if (model == null) {
                info += String.format("<p>BioModel <b>%s</b> could not be loaded.</p><hr>", modelId);
                continue;
            }
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
        String id = simpleModel.id();
        String name = simpleModel.name();
        String publicationId = simpleModel.publicationIdentifier();
        String submissionIdentifier = simpleModel.submissionIdentifier();
        String description = simpleModel.description();
        String authors = simpleModel.authors();
        String info;
        if (selected) {
            info = "<table><tr><td bgcolor=\"#339933\">&nbsp;&nbsp;&nbsp;<td><td>";
        } else {
            info = "<table><tr><td>&nbsp;&nbsp;&nbsp;<td><td>";
        }
        info += createHTMLTableHeader() + createHTMLTableRow("ID", id)
                + createHTMLTableRow("Submission ID", submissionIdentifier)
                + createHTMLTableRow("Name", name)
                + createHTMLTableRow("Description", description.toString())
                + createHTMLTableRow("Authors", authors)
                + createHTMLTableRow("Pubmed", createPubmedHTMLLink(publicationId))
                + "</table>"
                + "</td></tr></table>";
        return info;
    }

    private static String createHTMLTableHeader() {
        String border = "0";
        String header = String.format("<table border=%s>", border);
        return header;
    }

    private static String createHTMLTableRow(String attribute, String value) {
        return String.format(
                "<tr>" + "	<td><b><font size=\"-1\">%s</font></b></td> "
                        + "	<td><font size=\"-1\">%s</font></b></td></tr>",
                attribute, value);
    }

    public static String createBioModelHTMLLink(String bioModelId) {
        return String.format(
                "<a href=\"https://www.biomodels.org/%s\" target=\"_blank\">%s</a>", bioModelId, bioModelId);
    }

    private static String createPubmedHTMLLink(String pubmedId) {
        return String.format(
                "<a href=\"http://www.ncbi.nlm.nih.gov/pubmed?term=%s\" target=\"_blank\">%s</a>", pubmedId, pubmedId);
    }
}
