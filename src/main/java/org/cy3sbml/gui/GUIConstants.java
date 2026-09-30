package org.cy3sbml.gui;

import static org.cy3sbml.HtmlTemplateParser.load;
import static org.cy3sbml.HtmlTemplateParser.parseTemplateSections;

import java.util.Collections;
import java.util.Map;

/**
 * Constants of the GUI: the resources, icons, toolbar positions and descriptions of the
 * actions, and the HTML fragments of the info panel from {@code gui/linktemplate.html}.
 */
public class GUIConstants {
    public static final String HTML_HELP_RESOURCE = "/gui/help.html";

    public static final String HTML_EXAMPLE_RESOURCE = "/gui/examples.html";

    public static final String LOGO_BIOMODELS = "/gui/images/logos/biomodels_logo.png";

    public static final String ICON_CHANGESTATE = "/gui/images/changestate.png";
    public static final String ICON_IMPORT = "/gui/images/import.png";

    public static final String ICON_EXAMPLES = "/gui/images/examples.png";
    public static final String ICON_COFACTOR_SPLIT = "/gui/images/cofactor-split.png";
    public static final String ICON_COFACTOR_MERGE = "/gui/images/cofactor-merge.png";
    public static final String ICON_BIOMODELS = "/gui/images/biomodels.png";
    public static final String ICON_HELP = "/gui/images/help.png";
    public static final String ICON_LOADLAYOUT = "/gui/images/layout-load.png";
    public static final String ICON_SAVELAYOUT = "/gui/images/layout-save.png";

    public static final float GRAVITY_CHANGESTATE = (float) 100.0;
    public static final float GRAVITY_IMPORT = (float) 101.0;

    public static final float GRAVITY_EXAMPLES = (float) 106.0;
    public static final float GRAVITY_BIOMODELS = (float) 110.0;
    public static final float GRAVITY_HELP = (float) 112.0;

    public static final float GRAVITY_COFACTOR_SPLIT = (float) 113.0;
    public static final float GRAVITY_COFACTOR_MERGE = (float) 113.5;
    public static final float GRAVITY_LOADLAYOUT = (float) 114.0;
    public static final float GRAVITY_SAVELAYOUT = (float) 120.0;

    public static final String DESCRIPTION_CHANGESTATE = "Hide|show panel";
    public static final String DESCRIPTION_IMPORT = "Import SBML";
    public static final String DESCRIPTION_EXAMPLES = "SBML examples";
    public static final String DESCRIPTION_COFACTOR_SPLIT = "Split cofactor nodes";
    public static final String DESCRIPTION_COFACTOR_MERGE = "Merge cofactor nodes";
    public static final String DESCRIPTION_BIOMODELS = "BioModels Import";
    public static final String DESCRIPTION_HELP = "Help";
    public static final String DESCRIPTION_LOADLAYOUT = "Load Layout";
    public static final String DESCRIPTION_SAVELAYOUT = "Save Layout";

    /** The HTML fragments of {@code gui/linktemplate.html} by section name. */
    public static final Map<String, String> htmlFragments = Collections.unmodifiableMap(parseTemplateSections(load()));

    public static final String HTML_START_TEMPLATE = htmlFragments.get("HTML_START");
    public static final String HTML_STOP_TEMPLATE = htmlFragments.get("HTML_STOP");
    public static final String ICON_WARNING = htmlFragments.get("WARNING");
    public static final String ICON_TRUE = htmlFragments.get("TRUE");
    public static final String ICON_FALSE = htmlFragments.get("FALSE");
    public static final String ICON_INVISIBLE = htmlFragments.get("INVISIBLE");
    /** Link icon, with its tooltip in the placeholder {title}. */
    public static final String ICON_LINK = htmlFragments.get("LINK_ICON");

    public static final String ICON_SPINNER = htmlFragments.get("SPINNER");
    public static final String EXPORT_HTML =
            htmlFragments.get("EXPORT_HTML").replace("{URL}", BrowserHyperlinkListener.URL_HTML_SBASE);
    public static final String TABLE_START = htmlFragments.get("TABLE_START");
    public static final String TABLE_END = htmlFragments.get("TABLE_END");
    public static final String TS = htmlFragments.get("TABLE_ROW_START");
    public static final String TM = htmlFragments.get("TABLE_ROW_MIDDLE");
    public static final String TE = htmlFragments.get("TABLE_ROW_END");
    public static final String OLS_TERM_ERROR = htmlFragments.get("OLS_TERM_ERROR");
    public static final String DESCRIPTION_LABEL = htmlFragments.get("DESCRIPTION_LABEL");
    public static final String SYNONYMS_LABEL = htmlFragments.get("SYNONYMS_LABEL");
    public static final String ONTOLOGY_TERM_LINK = htmlFragments.get("ONTOLOGY_TERM_LINK");
    public static final String IDENTIFIER_PATTERN_MISMATCH = htmlFragments.get("IDENTIFIER_PATTERN_MISMATCH");
    public static final String MIRIAM_COLLECTION_LINK = htmlFragments.get("MIRIAM_COLLECTION_LINK");
    public static final String INVISIBLE_RESOURCE_LINK = htmlFragments.get("INVISIBLE_RESOURCE_LINK");
    public static final String UNKNOWN_DATA_COLLECTION = htmlFragments.get("UNKNOWN_DATA_COLLECTION");
    public static final String IDENTIFIER_LINK = htmlFragments.get("IDENTIFIER_LINK");
    public static final String EMAIL_LINK = htmlFragments.get("EMAIL_LINK");
    public static final String MODIFIED_DATE = htmlFragments.get("MODIFIED_DATE");
    public static final String CREATED_DATE1 = htmlFragments.get("CREATED_DATE");

    private GUIConstants() {}
}
