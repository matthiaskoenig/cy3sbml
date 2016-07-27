package org.cy3sbml.gui;

import java.util.*;

/**
 * Constants used in GUI.
 */
public class GUIConstants {

    public static String HTML_HELP_RESOURCE = "/gui/help.html";
    public static String HTML_EXAMPLE_RESOURCE = "/gui/examples.html";

    public static String IMAGE_CY3SBML_ICON = "/gui/images/cy3sbml_icon.png";
    public static String IMAGE_BIOMODELS_LOGO_80 = "/gui/images/biomodels_logo_80.png";
    public static String IMAGE_BIOMODELS_LOGO = "/gui/images/biomodels_logo.png";
    public static String IMAGE_CHANGESTATE = "/gui/images/changestate.png";
    public static String IMAGE_COFACTOR = "/gui/images/cofactor.png";
    public static String IMAGE_EXAMPLES = "/gui/images/examples.png";
    public static String IMAGE_HELP = "/gui/images/help.png";
    public static String IMAGE_IMPORT = "/gui/images/import.png";
    public static String IMAGE_VALIDATION = "/gui/images/validation.png";
    public static String IMAGE_LOADLAYOUT = "/gui/images/loadlayout.png";
    public static String IMAGE_SAVELAYOUT = "/gui/images/savelayout.png";

    public static String URL_BIOMODELS = "http://cy3sbml-biomodels";
    public static String URL_CHANGESTATE = "http://cy3sbml-changestate";
    public static String URL_IMPORT = "http://cy3sbml-import";
    public static String URL_VALIDATION = "http://cy3sbml-validation";
    public static String URL_EXAMPLES = "http://cy3sbml-examples";

    public static String URL_SBMLFILE = "http://sbml-file";


    public static final Map<String, String> EXAMPLE_SBML;
    public static final Set<String> URLS_ACTION;
    static {
        HashMap<String, String> map = new HashMap<>();
        map.put("http://cy3sbml-glucose", "/models/Koenig2014_Glucose_Metabolism.xml");
        map.put("http://cy3sbml-galactose", "/models/Galactose_v129_Nc1_core.xml");
        map.put("http://cy3sbml-HepatoNet1", "/models/HepatoNet1.xml");
        map.put("http://cy3sbml-e_coli_core", "/models/e_coli_core.xml");
        map.put("http://cy3sbml-iAB_RBC_283", "/models/iAB_RBC_283.xml");
        map.put("http://cy3sbml-iIT341", "/models/iIT341.xml");
        map.put("http://cy3sbml-RECON1", "/models/RECON1.xml");
        map.put("http://cy3sbml-BIOMD0000000016", "/models/BIOMD0000000016.xml");
        map.put("http://cy3sbml-BIOMD0000000084", "/models/BIOMD0000000084.xml");
        map.put("http://cy3sbml-hsa04360", "/models/hsa04360.xml");
        EXAMPLE_SBML = Collections.unmodifiableMap(map);

        Set<String> set = new HashSet<>();
        set.add(URL_BIOMODELS);
        set.add(URL_CHANGESTATE);
        set.add(URL_IMPORT);
        set.add(URL_VALIDATION);
        set.add(URL_EXAMPLES);
        URLS_ACTION = Collections.unmodifiableSet(set);
    }


    private GUIConstants(){}

}
