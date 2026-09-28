package org.cy3sbml.biomodel;

import org.cy3sbml.gui.GUIConstants;

public class BioModelDialogText {
    public static String getHeaderString() {
        String imgsrc = BioModelDialogText.class
                .getResource(GUIConstants.LOGO_BIOMODELS)
                .toString();
        String info = "<a href=\"https://www.biomodels.org/\"><img src=\"" + imgsrc
                + "\" alt=\"BioModels Logo\" height=80 width=80 border=0></img></a>";
        return info;
    }

    public static String getInfo() {
        String info = getHeaderString();
        info += "<h2>Import of BioModels</h2>"
                + "<p>Load SBML models from <a href=\"https://www.biomodels.org/\">BioModels</a> by "
                + "(1) searching the repository or (2) via providing a set of BioModel identifiers.</p>"
                + "<p>(1) Type search terms in the <span color=\"gray\"><b>Name</b></span> field and click "
                + "<span color=\"gray\"><b>Search</b></span> (or press Enter). "
                + "BioModels searches the terms in the whole model entry, e.g. model name, description, "
                + "authors, publication and annotations. Multiple search terms are combined via AND "
                + "(all terms must match) or OR (any term matches). "
                + "Up to " + BiomodelsQuery.MAX_RESULTS + " results of a search are listed.</p>"
                + "<p>Search results are shown in list format. Select model ids in the list to see their details. "
                + "To load one or multiple models select the model ids in the list and click "
                + "<span color=\"gray\"><b>Load Selected</b></span>.</p>"
                + "<p>(2) For direct access to specific models type the BioModel Ids in the text field.<br>"
                + "Click <span color=\"gray\"><b>Parse Ids</b></span>"
                + " to get information about the models or <span color=\"gray\"><b>Load Ids</b></span> to load the models for the given ids.<br>"
                + "Arbitrary text containing BioModel Ids can be used as input for the parsing and loading.</p>"
                + "<p>The access via the BioModel WebService can take a few seconds depending on the search query.</p>";
        return info;
    }

    public static String getWebserviceError() {
        String info = getHeaderString();
        info += "<p>The BioModels web service could not be accessed.</p>"
                + "<p>Test your internet connection and set the proxy information for Cytoscape "
                + "(Edit -> Preferences -> Proxy Settings) for your connection in Cytoscape.</p>"
                + "<p>Possibly, the BioModels web service is temporarily not available. "
                + "In this case download the model of interest as SBML from "
                + "<a href=\"https://www.biomodels.org/\">BioModels</a> and "
                + "import it via File -> Import -> Network from File.</p>";
        return info;
    }

    public static String performBioModelSearch() {
        String info = getHeaderString();
        info += "<p>Searching BioModels ...</p>" + "<p>... the request can take a few seconds.</p>";
        return info;
    }

    public static String getString(String msg) {
        String info = getHeaderString();
        info += msg;
        return info;
    }

    public static String getWebserviceSBMLRequest() {
        String info = getHeaderString();
        info += "<p>Getting the BioModels from the BioModels web service ...</p>"
                + "<p>... the request can take a few seconds.</p>";
        return info;
    }
}
