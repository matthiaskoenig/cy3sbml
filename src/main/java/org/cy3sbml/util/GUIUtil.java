package org.cy3sbml.util;

import org.cy3sbml.SBMLManager;
import org.cytoscape.util.swing.OpenBrowser;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.TidySBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;


public class GUIUtil {
    private static final Logger logger = LoggerFactory.getLogger(GUIUtil.class);

    /**
     * Open current SBML in browser.
     * Writes a temporary file of the SBML which can be loaded.
     */
    public static void openCurrentSBMLInBrowser(OpenBrowser openBrowser){
        SBMLManager sbmlManager = SBMLManager.getInstance();
        SBMLDocument doc = sbmlManager.getCurrentSBMLDocument();
        //create a temp file
        File temp;
        try {
            temp = File.createTempFile("temp-file-name", ".xml");
            logger.info("Temp file : " + temp.getAbsolutePath());
            try {
                TidySBMLWriter.write(doc, temp.getAbsolutePath(), ' ', (short) 2);
                openBrowser.openURL("file://" + temp.getAbsolutePath());
            } catch (SBMLException | FileNotFoundException | XMLStreamException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
