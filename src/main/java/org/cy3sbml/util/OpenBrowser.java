package org.cy3sbml.util;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static OpenBrower helper class.
 */
public class OpenBrowser {
    private static final Logger logger = LoggerFactory.getLogger(OpenBrowser.class);
    private static String[] BROWSERS = {"xdg-open", "htmlview", "firefox", "mozilla", "konqueror", "chrome", "chromium"};

    /**
     * Opens the specified URL in the system default web browser.
     *
     * @return true if the URL opens successfully.
     */

    public static boolean openURL(final String url) {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL has an incorrect format: " + url);
        }

        if (openURLWithDesktop(uri)) {
            return true;
        } else {
            for (final String browser : BROWSERS) {
                if (openURLWithBrowser(url, browser)) {
                    return true;
                }
            }
        }

        JOptionPane.showInputDialog(null, "Cytoscape was unable to open your web browser.. "
                + "\nPlease copy the following URL and paste it into your browser:", url);
        return false;
    }

    private static boolean openURLWithDesktop(final URI uri) {
        if (!Desktop.isDesktopSupported())
            return false;
        try {
            Desktop.getDesktop().browse(uri);
            return true;
        } catch (IOException e) {
            logger.warn("Failed to launch browser through java.awt.Desktop.browse(): " + e.getMessage());
            return false;
        }
    }

    private static boolean openURLWithBrowser(final String url, final String browser) {
        final ProcessBuilder builder = new ProcessBuilder(browser, url);
        try {
            builder.start();
            return true;
        } catch (IOException e) {
            logger.info(String.format("Failed to launch browser process %s: %s", browser, e.getMessage()));
            return false;
        }
    }
}