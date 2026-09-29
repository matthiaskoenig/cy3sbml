package org.cy3sbml.util;

import static org.cy3sbml.gui.GUIConstants.EXPORT_HTML;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cytoscape.work.TaskIterator;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.TidySBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GUIUtil {
    private static final Logger logger = LoggerFactory.getLogger(GUIUtil.class);

    /**
     * Loads an SBML example file from the given resource: copies it into a temporary file,
     * named like the resource, and loads the file in a Cytoscape task. Does not wait for the
     * load, so it can be called on the Swing event dispatch thread.
     */
    public static void loadExampleFromResource(ServiceAdapter adapter, String resource) {
        try (InputStream instream = GUIUtil.class.getResourceAsStream(resource)) {
            if (instream == null) {
                logger.warn("Could not find example resource: {}", resource);
                return;
            }
            String name = resource.substring(resource.lastIndexOf('/') + 1);
            Path directory = Files.createTempDirectory("cy3sbml-example");
            // deleted on exit in reverse order: the file before its directory
            directory.toFile().deleteOnExit();
            Path file = directory.resolve(name);
            file.toFile().deleteOnExit();
            Files.copy(instream, file);

            TaskIterator iterator = adapter.loadNetworkFileTaskFactory.createTaskIterator(file.toFile());
            adapter.dialogTaskManager.execute(iterator);
        } catch (IOException e) {
            logger.warn("Could not read example: {}", resource, e);
        } catch (RuntimeException e) {
            // UI boundary: called from a hyperlink in the WebView
            logger.error("Could not load example: {}", resource, e);
        }
    }

    /**
     * Open current SBML in browser.
     * Writes a temporary file of the SBML which can be loaded.
     */
    public static void openCurrentSBMLInBrowser(SBMLManager sbmlManager) {
        SBMLDocument doc = sbmlManager.getCurrentSBMLDocument();

        try {
            // write to tmp file
            File temp = File.createTempFile("cy3sbml", ".xml");
            logger.debug("Temp file : {}", temp.getAbsolutePath());

            try {
                TidySBMLWriter.write(doc, temp.getAbsolutePath(), ' ', (short) 2);
                openFileInBrowser(temp);
            } catch (SBMLException | FileNotFoundException | XMLStreamException e) {
                logger.error("SBML opening failed.", e);
            }
        } catch (IOException e) {
            logger.error("SBML could not be opened in browser.", e);
        }
    }

    /**
     * Open url in external webView.
     */
    public static void openURLinExternalBrowser(String url) {
        logger.debug("Open in external webView <{}>", url);
        SwingUtilities.invokeLater(() -> OpenBrowser.openURL(url));
    }

    /**
     * Open the given SBase HTML information in external Browser.
     */
    public static void openSBaseHTMLInBrowser(String html) {
        if (html == null) {
            logger.warn("No HTML available in the panel, nothing to open in the browser.");
            return;
        }
        // remove export button, exported html cannot be exported
        html = html.replace(EXPORT_HTML, "");
        openHTMLInBrowser(html);
    }

    /**
     * Open validation HTML in external Browser.
     */
    public static void openHTMLInBrowser(String html) {
        // write temp file
        try {
            File temp = File.createTempFile("cy3sbml", ".html");
            logger.debug("Temp file : {}", temp.getAbsolutePath());

            Files.writeString(temp.toPath(), html, StandardCharsets.UTF_8);
            GUIUtil.openFileInBrowser(temp);
        } catch (IOException e) {
            logger.error("File could not be opened.", e);
        }
    }

    /**
     * Open a given file in browser.
     */
    public static void openFileInBrowser(File temp) {
        SwingUtilities.invokeLater(() -> OpenBrowser.openURL("file://" + temp.getAbsolutePath()));
    }
}
