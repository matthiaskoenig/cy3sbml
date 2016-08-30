package org.cy3sbml.gui;


import org.codefx.libfx.control.webview.WebViewHyperlinkListener;
import org.codefx.libfx.control.webview.WebViews;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.actions.*;
import org.cy3sbml.util.GUIUtil;

import org.cytoscape.application.swing.AbstractCyAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.HyperlinkEvent;
import java.net.URL;

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
        if (url != null && WebViewPanel.getInstance() != null) {
            String s = url.toString();

            // Cytoscape Action
            if (GUIConstants.URLS_ACTION.contains(s)){
                ServiceAdapter adapter = WebViewPanel.getInstance().getAdapter();

                AbstractCyAction action = null;
                if (s.equals(GUIConstants.URL_CHANGESTATE)){
                    action = new ChangeStateAction();
                }
                if (s.equals(GUIConstants.URL_IMPORT)){
                    action = new ImportAction(adapter);
                }
                if (s.equals(GUIConstants.URL_VALIDATION)){
                    action = new ValidationAction(adapter);
                }
                if (s.equals(GUIConstants.URL_EXAMPLES)){
                    action = new ExamplesAction();
                }
                if (s.equals(GUIConstants.URL_BIOMODELS)){
                    action = new BiomodelsAction(adapter);
                }
                if (s.equals(GUIConstants.URL_HELP)){
                    action = new HelpAction();
                }
                if (s.equals(GUIConstants.URL_COFACTOR_NODES)){
                    action = new CofactorAction(adapter);
                }
                if (s.equals(GUIConstants.URL_SAVELAYOUT)){
                    action = new SaveLayoutAction(adapter);
                }
                if (s.equals(GUIConstants.URL_LOADLAYOUT)){
                    action = new LoadLayoutAction(adapter);
                }

                // execute action
                if (action != null){
                    action.actionPerformed(null);
                } else {
                    logger.error(String.format("Action not created for <%s>", s));
                }
                return true;
            }

            if (s.startsWith(GUIConstants.URL_SELECT_SBASE)){
                System.out.println("Select sbase: " + s);
            }

            // Example networks
            if (GUIConstants.EXAMPLE_SBML.containsKey(s)){
                String resource = GUIConstants.EXAMPLE_SBML.get(s);
                GUIUtil.loadExampleFromResource(resource);
                return true;
            }

            // SBML file
            if (s.equals(GUIConstants.URL_SBMLFILE)){
                GUIUtil.openCurrentSBMLInBrowser();
                return true;
            }

            // SBase HTML
            if (s.equals(GUIConstants.URL_HTML_SBASE)){
                GUIUtil.openSBaseHTMLInBrowser();
                return true;
            }

            // Validation HTML
            if (s.equals(GUIConstants.URL_HTML_SBASE)){
                GUIUtil.openValidationHTMLInBrowser();
                return true;
            }

            // HTML links
            GUIUtil.openURLinExternalBrowser(s);
            return true;
        }
        // This is a link we should load, do not cancel.
        return false;
    }
}
