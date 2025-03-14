package org.cy3sbml.gui;

import javax.swing.event.HyperlinkEvent;
import java.net.URL;
import java.util.*;

import org.codefx.libfx.control.webview.WebViewHyperlinkListener;
import org.codefx.libfx.control.webview.WebViews;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.actions.*;
import org.cy3sbml.util.GUIUtil;

import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * Handle hyperlink events in WebView.
 * Either opens browser for given hyperlink or triggers Cytoscape actions
 * for subsets of special hyperlinks.
 *
 * This provides an easy solution for integrating app functionality
 * with click on hyperlinks.
 * Alternative javascript upcalls could be performed.
 */
public class BrowserHyperlinkListener implements WebViewHyperlinkListener{
    private static final Logger logger = LoggerFactory.getLogger(BrowserHyperlinkListener.class);

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
    public static final String URL_SELECT_METAID = "http://select-metaid/";
    public static final String URL_SELECT_ID = "http://select-id/";

    public static final Map<String, String> EXAMPLE_SBML;
    public static final Set<String> URLS_ACTION;

    // Set all the URL actions
    static {
        HashMap<String, String> map = new HashMap<>();
        map.put("http://cy3sbml-glucose", "/models/Koenig_glucose_v1.xml");
        map.put("http://cy3sbml-galactose", "/models/Koenig_galactose_31.xml");
        map.put("http://cy3sbml-HepatoNet1", "/models/HepatoNet1.xml");
        map.put("http://cy3sbml-e_coli_core", "/models/e_coli_core.xml");
        map.put("http://cy3sbml-iAB_RBC_283", "/models/iAB_RBC_283.xml");
        map.put("http://cy3sbml-iIT341", "/models/iIT341.xml");
        map.put("http://cy3sbml-RECON1", "/models/RECON1.xml");
        map.put("http://cy3sbml-BIOMD0000000001", "/models/BIOMD0000000001.xml");
        map.put("http://cy3sbml-BIOMD0000000012", "/models/BIOMD0000000012.xml");
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

    @Override
    public boolean hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
        logger.info(WebViews.hyperlinkEventToString(hyperlinkEvent));

        // clicked url
        URL url = hyperlinkEvent.getURL();
        Boolean cancel = processURLEvent(url);
        return cancel;
    }

    /**
     * Processes the given url.
     * Decides what to do if a given URL is encountered.
     * Here the actions are called.
     *
     * @param url
     * @return cancel action, i.e. is the WebView event further processed
     */
    private static Boolean processURLEvent(URL url){
        if (url != null) {
            String s = url.toString();

            ServiceAdapter adapter = WebViewPanel.getInstance().getAdapter();

            // Cytoscape Action
            if (URLS_ACTION.contains(s)){
                AbstractCyAction action = null;
                if (s.equals(URL_CHANGESTATE)){
                    action = new ChangeStateAction();
                }
                if (s.equals(URL_IMPORT)){
                    action = new ImportAction(adapter);
                }
                if (s.equals(URL_VALIDATION)){
                    ValidationAction.runValidation(adapter.taskManager);
                }
                if (s.equals(URL_EXAMPLES)){
                    action = new ExamplesAction();
                }
                if (s.equals(URL_BIOMODELS)){
                    action = new BiomodelsAction(adapter);
                }
                if (s.equals(URL_HELP)){
                    action = new HelpAction();
                }
                if (s.equals(URL_COFACTOR_NODES)){
                    CofactorAction.runCofactorAction(adapter);
                }
                if (s.equals(URL_SAVELAYOUT)){
                    action = new SaveLayoutAction(adapter);
                }
                if (s.equals(URL_LOADLAYOUT)){
                    action = new LoadLayoutAction(adapter);
                }

                // execute action
                if (action != null){
                    action.actionPerformed(null);
                } else {
                    logger.error(String.format("Action not created for <%s>", s));
                }
            }
            else if (s.startsWith(URL_SELECT_METAID) || (s.startsWith(URL_SELECT_ID))) {
                // Only select if current network exists
                CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
                if (network != null) {
                    String[] tokens = s.split("/");
                    String identifier = tokens[tokens.length - 1];
                    if (s.startsWith(URL_SELECT_ID)) {
                        NetworkUtil.selectById(network, identifier);
                    } else if (s.startsWith(URL_SELECT_METAID)) {
                        NetworkUtil.selectByMetaId(network, identifier);
                    }
                }
            }

            // Example networks
            else if (EXAMPLE_SBML.containsKey(s)){
                String resource = EXAMPLE_SBML.get(s);
                logger.info("Loading: " + s);
                GUIUtil.loadExampleFromResource(resource);
            }

            // SBML file
            else if (s.equals(URL_SBMLFILE)){
                GUIUtil.openCurrentSBMLInBrowser();
            }

            // SBase HTML
            else if (s.equals(URL_HTML_SBASE)){
                GUIUtil.openSBaseHTMLInBrowser();
            }

            // Validator HTML
            else if (s.equals(URL_HTML_VALIDATION)){
                GUIUtil.openValidationHTMLInBrowser();
            }

            // HTML links
            else {
                GUIUtil.openURLinExternalBrowser(s);
            }
            return true;
        }
        // This is a link we should load, do not cancel.
        return false;
    }

}
