package org.cy3sbml.gui;

import java.io.File;
import java.net.URI;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import org.codefx.libfx.control.webview.WebViews;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.event.HyperlinkEvent;


/**
 * Browser for displaying HTML within a JavaFX Webview.
 * This can be embedded in Swing using a JFXPanel.
 * A HyperlinkListener processes the hyperlinks.
 */
public class Browser extends Region {
    private static final Logger logger = LoggerFactory.getLogger(Browser.class);

    private final WebView webView;
    private final WebEngine webEngine;
    private final File appDirectory;

    // single instance for all browsers
    // avoid concurrency issues
    private static final BrowserHyperlinkListener eventProcessingListener = new BrowserHyperlinkListener();


    public Browser(File appDirectory) {
        this.appDirectory = appDirectory;
        webView = new WebView();
        webEngine = webView.getEngine();
        logger.debug("WebView version: " + webEngine.getUserAgent());

        // add WebView to scene
        getChildren().add(webView);

        // Listening to hyperlink events
        // BrowserHyperlinkListener eventProcessingListener = new BrowserHyperlinkListener();
        WebViews.addHyperlinkListener(webView, eventProcessingListener, HyperlinkEvent.EventType.ACTIVATED);
    }

    /**
     * Load local resource;
     */
    public void loadPageFromResource(String resource) {
        File file = new File(appDirectory, resource);
        URI fileURI = file.toURI();
        logger.debug("Load page:" + fileURI);
        loadPage(fileURI.toString());
    }

    /**
     * Load page in webView;
     */
    public void loadPage(String url) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                webEngine.load(url);
            }
        });
    }

    /**
     * Load HTML text in the webEngine.
     */
    public void loadText(String text) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                webEngine.loadContent(text);
            }
        });

    }

    @Override
    protected void layoutChildren() {
        double w = getWidth();
        double h = getHeight();
        layoutInArea(webView, 0, 0, w, h, 0, HPos.CENTER, VPos.CENTER);
    }

    @Override
    protected double computePrefWidth(double height) {
        return 900;
    }

    @Override
    protected double computePrefHeight(double width) {
        return 600;
    }

}