package org.cy3sbml.gui;

import java.util.Map;

import static org.cy3sbml.HtmlTemplateParser.load;
import static org.cy3sbml.HtmlTemplateParser.parseTemplateSections;

/**
 * Constants used in GUI.
 */
public class GUIConstants {
    public static final String HTML_HELP_RESOURCE = "/gui/help.html";

    public static final String HTML_EXAMPLE_RESOURCE = "/gui/examples.html";

    public static final String LOGO_BIOMODELS = "/gui/images/logos/biomodels_logo.png";
    public static final String ICON_CY3SBML = "/gui/images/logos/cy3sbml_icon.png";

    public static final String ICON_CHANGESTATE = "/gui/images/changestate.png";
    public static final String ICON_ARCHIVE = "/gui/images/archive.png";
    public static final String ICON_IMPORT = "/gui/images/import.png";

    public static final String ICON_EXAMPLES = "/gui/images/examples.png";
    public static final String ICON_COFACTOR = "/gui/images/cofactor.png";
    public static final String ICON_BIOMODELS = "/gui/images/biomodels.png";
    public static final String ICON_BIOMODELS_DEPRECATED = "/gui/images/biomodels_deprecated.png";
    public static final String ICON_HELP = "/gui/images/help.png";
    public static final String ICON_LOADLAYOUT = "/gui/images/layout-load.png";
    public static final String ICON_SAVELAYOUT = "/gui/images/layout-save.png";

    public static float GRAVITY_CHANGESTATE = (float) 100.0;
    public static float GRAVITY_IMPORT = (float) 101.0;
    public static float GRAVITY_ARCHIVE = (float) 102.0;

    public static float GRAVITY_EXAMPLES = (float) 106.0;
    public static float GRAVITY_BIOMODELS = (float) 110.0;
    public static float GRAVITY_HELP = (float) 112.0;

    public static float GRAVITY_LOCATION = (float) 113.0;
    public static float GRAVITY_LOADLAYOUT = (float) 114.0;
    public static float GRAVITY_SAVELAYOUT = (float) 120.0;

    public static final String DESCRIPTION_CHANGESTATE = "Hide|show panel";
    public static final String DESCRIPTION_ARCHIVE = "Import Archive (COMBINE & ResearchObjects)";
    public static final String DESCRIPTION_IMPORT = "Import SBML";
    public static final String DESCRIPTION_EXAMPLES = "SBML examples";
    public static final String DESCRIPTION_COFACTOR = "Cofactor nodes";
    public static final String DESCRIPTION_BIOMODELS = "Biomodel Import";
    public static final String DESCRIPTION_HELP = "Help";
    public static final String DESCRIPTION_LOADLAYOUT = "Load Layout";
    public static final String DESCRIPTION_SAVELAYOUT = "Save Layout";

    //HTML FRAGMENTS
    public static final String htmlTemplate = load();
    public static final Map<String,String> htmlFragments = parseTemplateSections(htmlTemplate);

    public static final String HTML_START_TEMPLATE = htmlFragments.get("HTML_START");
    public static final String HTML_STOP_TEMPLATE = htmlFragments.get("HTML_STOP");
    public static final String ICON_WARNING = htmlFragments.get("WARNING");
    public static final String ICON_TRUE = htmlFragments.get("TRUE");
    public static final String ICON_FALSE = htmlFragments.get("FALSE");
    public static final String ICON_NONE = htmlFragments.get("NONE");
    public static final String ICON_INVISIBLE = htmlFragments.get("INVISIBLE");
    public static final String EXPORT_HTML = htmlFragments.get("EXPORT_HTML").replace("{URL}", BrowserHyperlinkListener.URL_HTML_SBASE);;
    public static final String TABLE_START = htmlFragments.get("TABLE_START");
    public static final String TABLE_END = htmlFragments.get("TABLE_END");
    public static final String TS = htmlFragments.get("TABLE_ROW_START");
    public static final String TM = htmlFragments.get("TABLE_ROW_MIDDLE");
    public static final String TE = htmlFragments.get("TABLE_ROW_END");
    public static final String OLS_TERM_ERROR = htmlFragments.get("OLS_TERM_ERROR");
    public static final String DESCRIPTION_LABEL = htmlFragments.get("DESCRIPTION_LABEL");
    public static final String OBO_SYNONYMS_LABEL = htmlFragments.get("OBO_SYNONYMS_LABEL");
    public static final String SYNONYMS_LABEL = htmlFragments.get("SYNONYMS_LABEL");
    public static final String ONTOLOGY_TERM_LINK = htmlFragments.get("ONTOLOGY_TERM_LINK");
    public static final String CONDITIONAL_LINK = htmlFragments.get("CONDITIONAL_LINK");
    public static final String IDENTIFIER_PATTERN_MISMATCH = htmlFragments.get("IDENTIFIER_PATTERN_MISMATCH");
    public static final String MIRIAM_COLLECTION_LINK = htmlFragments.get("MIRIAM_COLLECTION_LINK");
    public static final String INVISIBLE_RESOURCE_LINK = htmlFragments.get("INVISIBLE_RESOURCE_LINK");
    public static final String UNKNOWN_DATA_COLLECTION = htmlFragments.get("UNKNOWN_DATA_COLLECTION");
    public static final String IDENTIFIER_LINK = htmlFragments.get("IDENTIFIER_LINK");
    public static final String EMAIL_LINK = htmlFragments.get("EMAIL_LINK");
    public static final String MODIFIED_DATE = htmlFragments.get("MODIFIED_DATE");
    public static final String CREATED_DATE1 = htmlFragments.get("CREATED_DATE");

    private GUIConstants(){}

}
