package org.cy3sbml.gui;

import java.util.*;

/**
 * Constants used in GUI.
 */
public class GUIConstants {
    public static final String HTML_HELP_RESOURCE = "/gui/help.html";
    public static final String HTML_VALIDATION_RESOURCE = "/gui/validation.html";
    public static final String HTML_EXAMPLE_RESOURCE = "/gui/examples.html";

    public static final String LOGO_BIOMODELS = "/gui/images/logos/biomodels_logo.png";
    public static final String ICON_CY3SBML = "/gui/images/logos/cy3sbml_icon.png";

    public static final String ICON_CHANGESTATE = "/gui/images/changestate.png";
    public static final String ICON_IMPORT = "/gui/images/import.png";
    public static final String ICON_VALIDATION = "/gui/images/validation.png";
    public static final String ICON_EXAMPLES = "/gui/images/examples.png";
    public static final String ICON_COFACTOR = "/gui/images/cofactor.png";
    public static final String ICON_BIOMODELS = "/gui/images/biomodels.png";
    public static final String ICON_HELP = "/gui/images/help.png";
    public static final String ICON_LOADLAYOUT = "/gui/images/loadlayout.png";
    public static final String ICON_SAVELAYOUT = "/gui/images/savelayout.png";

    public static float GRAVITY_CHANGESTATE = (float) 100.0;
    public static float GRAVITY_IMPORT = (float) 102.0;
    public static float GRAVITY_VALIDATION = (float) 104.0;
    public static float GRAVITY_EXAMPLES = (float) 106.0;
    public static float GRAVITY_LOCATION = (float) 108.0;
    public static float GRAVITY_BIOMODELS = (float) 110.0;
    public static float GRAVITY_HELP = (float) 112.0;
    public static float GRAVITY_LOADLAYOUT = (float) 114.0;
    public static float GRAVITY_SAVELAYOUT = (float) 120.0;

    public static final String DESCRIPTION_CHANGESTATE = "Hide|show panel";
    public static final String DESCRIPTION_IMPORT = "Import SBML";
    public static final String DESCRIPTION_VALIDATION = "Validate SBML";
    public static final String DESCRIPTION_EXAMPLES = "SBML examples";
    public static final String DESCRIPTION_COFACTOR = "Cofactor nodes";
    public static final String DESCRIPTION_BIOMODELS = "Biomodel Import";
    public static final String DESCRIPTION_HELP = "Help";
    public static final String DESCRIPTION_LOADLAYOUT = "Save Layout";
    public static final String DESCRIPTION_SAVELAYOUT = "Load Layout";

    public static final String URL_CHANGESTATE = "http://cy3sbml-changestate";
    public static final String URL_IMPORT = "http://cy3sbml-import";
    public static final String URL_VALIDATION = "http://cy3sbml-validation";
    public static final String URL_EXAMPLES = "http://cy3sbml-examples";
    public static final String URL_BIOMODELS = "http://cy3sbml-biomodels";
    public static final String URL_HELP = "http://cy3sbml-help";
    public static final String URL_COFACTOR_NODES = "http://cy3sbml-cofactor";
    public static final String URL_LOADLAYOUT = "http://cy3sbml-layoutload";
    public static final String URL_SAVELAYOUT = "http://cy3sbml-layoutsave";


    public static final String URL_SBMLFILE = "http://sbml-file";
    public static final String URL_HTML_SBASE = "http://html-sbase";
    public static final String URL_HTML_VALIDATION = "http://html-validation";
    public static final String URL_SELECT_SBASE = "http://select-sbase/";


    public static final Map<String, String> EXAMPLE_SBML;
    public static final Set<String> URLS_ACTION;
    // Set all the URL actions
    static {
        HashMap<String, String> map = new HashMap<>();
        map.put("http://cy3sbml-glucose", "/models/Koenig2014_Glucose_Metabolism.xml");
        map.put("http://cy3sbml-galactose", "/models/Galactose_v129_Nc1_core.xml");
        map.put("http://cy3sbml-HepatoNet1", "/models/HepatoNet1.xml");
        map.put("http://cy3sbml-e_coli_core", "/models/e_coli_core.xml");
        map.put("http://cy3sbml-iAB_RBC_283", "/models/iAB_RBC_283.xml");
        map.put("http://cy3sbml-iIT341", "/models/iIT341.xml");
        map.put("http://cy3sbml-RECON1", "/models/RECON1.xml");
        map.put("http://cy3sbml-BIOMD0000000001", "/models/BIOMD0000000001.xml");
        map.put("http://cy3sbml-BIOMD0000000016", "/models/BIOMD0000000016.xml");
        map.put("http://cy3sbml-BIOMD0000000084", "/models/BIOMD0000000084.xml");
        map.put("http://cy3sbml-hsa04360", "/models/hsa04360.xml");
        EXAMPLE_SBML = Collections.unmodifiableMap(map);

        Set<String> set = new HashSet<>();

        set.add(URL_CHANGESTATE);
        set.add(URL_IMPORT);
        set.add(URL_VALIDATION);
        set.add(URL_EXAMPLES);
        set.add(URL_BIOMODELS);
        set.add(URL_HELP);
        set.add(URL_COFACTOR_NODES);
        set.add(URL_SAVELAYOUT);
        set.add(URL_LOADLAYOUT);
        URLS_ACTION = Collections.unmodifiableSet(set);
    }


    private GUIConstants(){}

}
