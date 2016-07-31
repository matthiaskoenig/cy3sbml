package org.cy3sbml.util;

import org.apache.commons.io.FileUtils;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.cy3sbml.gui.WebViewPanel;
import org.cytoscape.util.swing.OpenBrowser;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.TidySBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class GUIUtil {
    private static final Logger logger = LoggerFactory.getLogger(GUIUtil.class);

    /**
     * Open current SBML in browser.
     * Writes a temporary file of the SBML which can be loaded.
     */
    public static void openCurrentSBMLInBrowser(OpenBrowser openBrowser){
        SBMLManager sbmlManager = SBMLManager.getInstance();
        SBMLDocument doc = sbmlManager.getCurrentSBMLDocument();

        try {
            // write to tmp file
            File temp = File.createTempFile("cy3sbml", ".xml");
            logger.debug("Temp file : " + temp.getAbsolutePath());

            try {
                TidySBMLWriter.write(doc, temp.getAbsolutePath(), ' ', (short) 2);
                openFileInBrowser(temp, openBrowser);
            } catch (SBMLException | FileNotFoundException | XMLStreamException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Open HTML information in external Browser.
     */
    public static void openCurrentHTMLInBrowser(OpenBrowser openBrowser){
        String html = WebViewPanel.getInstance().getHtml();
        // remove the export button, exported html cannot be exported
        html = html.replace(SBaseHTMLFactory.EXPORT_HTML, "");

        // TODO: replace the title
        Pattern pattern = Pattern.compile("<h2>(.+)");
        Matcher matcher = pattern.matcher(html);
        String title = "";
        if (matcher.matches()){
            logger.info("Title found");
            title = matcher.group(0);
            html = html.replace("<title>cy3sbml</title>",
                    String.format("<title>%s</title>", title));
        }

        // TODO: make favicon work

        try {
            File temp = File.createTempFile("cy3sbml", ".html");
            logger.debug("Temp file : " + temp.getAbsolutePath());

            FileUtils.writeStringToFile(temp, html);
            GUIUtil.openFileInBrowser(temp, openBrowser);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Open a given file in browser. */
    public static void openFileInBrowser(File temp, OpenBrowser openBrowser){
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                openBrowser.openURL("file://" + temp.getAbsolutePath());
            }
        });

    }



}
