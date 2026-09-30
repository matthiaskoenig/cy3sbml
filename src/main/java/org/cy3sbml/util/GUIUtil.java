package org.cy3sbml.util;

import static org.cy3sbml.gui.GUIConstants.EXPORT_HTML;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cytoscape.work.TaskIterator;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.TidySBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Actions of the info panel links: loading the examples and opening the SBML, the panel HTML
 * or a URL in the system browser.
 */
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
     * Opens the SBML of the current document in the browser: writes it to a temporary file,
     * deleted on exit. Does nothing if there is no current document.
     *
     * @param sbmlManager the manager with the current document
     */
    public static void openCurrentSBMLInBrowser(SBMLManager sbmlManager) {
        SBMLDocument doc = sbmlManager.getCurrentSBMLDocument();
        if (doc == null) {
            logger.warn("No current SBML document, nothing to open in the browser.");
            return;
        }
        try {
            File temp = File.createTempFile("cy3sbml", ".xml");
            temp.deleteOnExit();
            TidySBMLWriter.write(doc, temp.getAbsolutePath(), ' ', (short) 2);
            openFileInBrowser(temp);
        } catch (SBMLException | XMLStreamException | IOException e) {
            logger.error("SBML could not be opened in the browser.", e);
        }
    }

    /**
     * Opens the URL in the system browser, on the Swing event dispatch thread.
     *
     * @param url the URL
     */
    public static void openURLinExternalBrowser(String url) {
        logger.debug("Open in external webView <{}>", url);
        SwingUtilities.invokeLater(() -> OpenBrowser.openURL(url));
    }

    /**
     * Opens the HTML of the info panel in the system browser, without its export button.
     *
     * @param html the HTML of the panel, null if the panel shows none yet
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
     * Opens the HTML in the system browser: writes it to a temporary file, deleted on exit.
     *
     * @param html the HTML page
     */
    public static void openHTMLInBrowser(String html) {
        try {
            File temp = File.createTempFile("cy3sbml", ".html");
            temp.deleteOnExit();
            Files.writeString(temp.toPath(), html, StandardCharsets.UTF_8);
            GUIUtil.openFileInBrowser(temp);
        } catch (IOException e) {
            logger.error("File could not be opened.", e);
        }
    }

    /**
     * Opens the file in the system browser, on the Swing event dispatch thread.
     *
     * @param temp the file
     */
    public static void openFileInBrowser(File temp) {
        String url = fileUrl(temp);
        SwingUtilities.invokeLater(() -> OpenBrowser.openURL(url));
    }

    /**
     * The file URL of the file. {@code "file://" + path} is no valid URL for a path with a
     * space or a Windows path.
     */
    static String fileUrl(File file) {
        return file.getAbsoluteFile().toURI().toString();
    }
}
