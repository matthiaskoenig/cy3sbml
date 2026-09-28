package org.cy3sbml.gui;

import java.io.File;
import java.net.URI;
import java.net.URL;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventTarget;

/**
 * Browser for displaying HTML within a JavaFX Webview.
 * This can be embedded in Swing using a JFXPanel.
 * A HyperlinkListener processes the hyperlinks.
 */
public final class Browser extends Region implements PageLoader.Target {
    private static final Logger logger = LoggerFactory.getLogger(Browser.class);

    private final WebView webView;
    private final WebEngine webEngine;
    private final File appDirectory;
    private final BrowserHyperlinkListener hyperlinkListener;

    public Browser(File appDirectory, BrowserHyperlinkListener hyperlinkListener) {
        this.appDirectory = appDirectory;
        this.hyperlinkListener = hyperlinkListener;
        webView = new WebView();
        webEngine = webView.getEngine();
        logger.debug("WebView version: {}", webEngine.getUserAgent());

        // add WebView to scene
        getChildren().add(webView);

        // Link clicks bubble up to the document, which handles the clicks on all links of
        // the page, including links the scripts of the page add later.
        webEngine.documentProperty().addListener((observable, oldDocument, document) -> {
            if (document != null) {
                ((EventTarget) document).addEventListener("click", this::onClick, false);
            }
        });
    }

    /**
     * Passes a click on a link to the hyperlink listener, and keeps the WebView from loading
     * the link if the listener handles it.
     */
    private void onClick(Event event) {
        Node node = (Node) event.getTarget();
        while (node != null && !isLink(node)) {
            node = node.getParentNode();
        }
        if (node == null) {
            return;
        }
        // the base URI honors the <base href> of the cy3sbml pages
        URL url = BrowserHyperlinkListener.resolve(node.getBaseURI(), ((Element) node).getAttribute("href"));
        if (url != null && hyperlinkListener.linkActivated(url)) {
            event.preventDefault();
        }
    }

    private static boolean isLink(Node node) {
        return node instanceof Element element
                && "a".equalsIgnoreCase(element.getTagName())
                && element.hasAttribute("href");
    }

    /**
     * Load local resource;
     */
    @Override
    public void loadPageFromResource(String resource) {
        File file = new File(appDirectory, resource);
        URI fileURI = file.toURI();
        logger.debug("Load page:{}", fileURI);
        loadPage(fileURI.toString());
    }

    /**
     * Load page in webView;
     */
    public void loadPage(String url) {
        Platform.runLater(() -> webEngine.load(url));
    }

    /**
     * Load HTML text in the webEngine.
     */
    @Override
    public void loadText(String text) {
        Platform.runLater(() -> webEngine.loadContent(text));
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
