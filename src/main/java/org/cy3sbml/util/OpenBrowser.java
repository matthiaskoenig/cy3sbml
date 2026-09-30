package org.cy3sbml.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opens URLs in the system browser: with {@link Desktop#browse}, else with the first
 * browser command that starts, else it shows the URL in a dialog to copy.
 */
public class OpenBrowser {
    private static final Logger logger = LoggerFactory.getLogger(OpenBrowser.class);
    private static final String[] BROWSERS = {
        "xdg-open", "htmlview", "firefox", "mozilla", "konqueror", "chrome", "chromium"
    };

    /**
     * Opens the URL in the system default web browser.
     *
     * @param url an absolute URL, e.g. of a link in the info panel
     * @return true if the URL opens successfully, false if it is no absolute URL or no
     *     browser could be started
     */
    public static boolean openURL(final String url) {
        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            logger.warn("Not opened in the browser, no valid URL: {}", url);
            return false;
        }
        // an absolute URL starts with its scheme, so a browser command cannot take it as an option
        if (!uri.isAbsolute()) {
            logger.warn("Not opened in the browser, no absolute URL: {}", url);
            return false;
        }
        if (openURLWithDesktop(uri)) {
            return true;
        }
        for (final String browser : BROWSERS) {
            if (openURLWithBrowser(url, browser)) {
                return true;
            }
        }
        JOptionPane.showInputDialog(
                null,
                "Cytoscape was unable to open your web browser."
                        + "\nPlease copy the following URL and paste it into your browser:",
                url);
        return false;
    }

    private static boolean openURLWithDesktop(final URI uri) {
        // a desktop can lack the browse action, e.g. without a registered browser
        if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            return false;
        }
        try {
            Desktop.getDesktop().browse(uri);
            return true;
        } catch (IOException | UnsupportedOperationException | SecurityException e) {
            logger.warn("Failed to launch browser through java.awt.Desktop.browse(): {}", e.getMessage());
            return false;
        }
    }

    private static boolean openURLWithBrowser(final String url, final String browser) {
        final ProcessBuilder builder = new ProcessBuilder(browser, url);
        try {
            builder.start();
            return true;
        } catch (IOException e) {
            logger.debug("Failed to launch browser process {}: {}", browser, e.getMessage());
            return false;
        }
    }
}
