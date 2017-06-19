package org.cy3sbml.util;


import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import java.io.*;

import org.apache.commons.io.FileUtils;

import org.apache.commons.io.IOUtils;
import org.cy3sbml.validator.Validator;
import org.cytoscape.work.TaskIterator;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.TidySBMLWriter;

import org.cy3sbml.SBMLManager;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.cy3sbml.gui.WebViewPanel;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.validator.ValidationFrame;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class GUIUtil {
    private static final Logger logger = LoggerFactory.getLogger(GUIUtil.class);

    /**
     * Loads an SBML example file from the given resource.
     * Needs access to the LoadNetworkFileTaskFaktory and the SynchronousTaskManager.
     *
     * TODO: make this a general function.
     * See also archive loading of xml.
     *
     * @param resource
     */
    public static void loadExampleFromResource(String resource){
        InputStream instream = GUIUtil.class.getResourceAsStream(resource);
        File tempFile;
        try {
            tempFile = File.createTempFile("tmp-example", ".xml");
            tempFile.deleteOnExit();
            FileOutputStream out = new FileOutputStream(tempFile);
            IOUtils.copy(instream, out);

            // read the file
            // FIXME: use observer
            ServiceAdapter adapter = WebViewPanel.getInstance().getAdapter();
            TaskIterator iterator = adapter.loadNetworkFileTaskFactory.createTaskIterator(tempFile);
            adapter.synchronousTaskManager.execute(iterator);
        } catch (Exception e) {
            logger.warn("Could not read example.", e);
            e.printStackTrace();
        }
    }


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

    /** Open url in external webView. */
    public static void openURLinExternalBrowser(String url){
        logger.debug("Open in external webView <" + url +">");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                OpenBrowser.openURL(url);
            }
        });
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
