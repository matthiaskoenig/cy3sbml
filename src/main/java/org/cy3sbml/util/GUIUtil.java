package org.cy3sbml.util;


import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import org.apache.commons.io.IOUtils;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.work.TaskIterator;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.TidySBMLWriter;

import org.cy3sbml.SBMLManager;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.cy3sbml.gui.WebViewPanel;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.actions.*;
import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.validator.ValidationFrame;
import org.cy3sbml.validator.Validator;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GUIUtil {
    private static final Logger logger = LoggerFactory.getLogger(GUIUtil.class);

    /**
     * Open current SBML in browser.
     * Writes a temporary file of the SBML which can be loaded.
     */
    public static void openCurrentSBMLInBrowser(){
        SBMLManager sbmlManager = SBMLManager.getInstance();
        SBMLDocument doc = sbmlManager.getCurrentSBMLDocument();

        try {
            // write to tmp file
            File temp = File.createTempFile("cy3sbml", ".xml");
            logger.debug("Temp file : " + temp.getAbsolutePath());

            try {
                TidySBMLWriter.write(doc, temp.getAbsolutePath(), ' ', (short) 2);
                openFileInBrowser(temp);
            } catch (SBMLException | FileNotFoundException | XMLStreamException e) {
                logger.error("SBML opening failed.", e);
                e.printStackTrace();
            }
        } catch (IOException e) {
            logger.error("SBML could not be opened in browser.", e);
            e.printStackTrace();
        }
    }

    /**
     * Processes the given url.
     * Decides what to do if a given URL is encountered.
     * Here the actions are called.
     *
     * @param url
     * @return cancel action, i.e. is the WebView event further processed
     */
    public static Boolean processURLEvent(URL url){
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
                loadExampleFromResource(resource);
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
            openURLinExternalBrowser(s);
            return true;
        }
        // This is a link we should load, do not cancel.
        return false;
    }

    /** Open url in external webView. */
    private static void openURLinExternalBrowser(String url){
        logger.debug("Open in external webView <" + url +">");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                OpenBrowser.openURL(url);
            }
        });
    }


    /**
     * Loads an SBML example file from the given resource.
     * Needs access to the LoadNetworkFileTaskFaktory and the SynchronousTaskManager.
     *
     * TODO: make this a general function.
     *
     * @param resource
     */
    private static void loadExampleFromResource(String resource){
        InputStream instream = GUIUtil.class.getResourceAsStream(resource);
        File tempFile;
        try {
            tempFile = File.createTempFile("tmp-example", ".xml");
            tempFile.deleteOnExit();
            FileOutputStream out = new FileOutputStream(tempFile);
            IOUtils.copy(instream, out);

            // read the file
            ServiceAdapter adapter = WebViewPanel.getInstance().getAdapter();
            TaskIterator iterator = adapter.loadNetworkFileTaskFactory.createTaskIterator(tempFile);
            adapter.synchronousTaskManager.execute(iterator);
        } catch (Exception e) {
            logger.warn("Could not read example.", e);
            e.printStackTrace();
        }
    }

    /**
     * Open HTML information in external Browser.
     */
    public static void openSBaseHTMLInBrowser(){
        String html = WebViewPanel.getInstance().getHtml();
        // remove export button, exported html cannot be exported
        html = html.replace(SBaseHTMLFactory.EXPORT_HTML, "");
        openHTMLInBrowser(html);
    }

    /**
     * Open validation HTML in external Browser.
     */
    public static void openValidationHTMLInBrowser(){
        String html = ValidationFrame.getInstance(null).getHtml();
        // remove export button, exported html cannot be exported
        html = html.replace(Validator.EXPORT_HTML, "");
        openHTMLInBrowser(html);
    }

    /**
     * Open validation HTML in external Browser.
     */
    public static void openHTMLInBrowser(String html){
        // write temp file
        try {
            File temp = File.createTempFile("cy3sbml", ".html");
            logger.debug("Temp file : " + temp.getAbsolutePath());

            FileUtils.writeStringToFile(temp, html);
            GUIUtil.openFileInBrowser(temp);
        } catch (IOException e) {
            logger.error("File could not be opened.", e);
            e.printStackTrace();
        }
    }


    /** Open a given file in browser. */
    public static void openFileInBrowser(File temp){
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                OpenBrowser.openURL("file://" + temp.getAbsolutePath());
            }
        });

    }

}
