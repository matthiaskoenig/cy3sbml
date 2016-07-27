package org.cy3sbml.gui;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.cytoscape.util.swing.OpenBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.net.URI;

/**
 * Browser for displaying HTML.
 * This can be embedded in Swing using a JFXPanel.
 */
public class Browser extends Region {
    private static final Logger logger = LoggerFactory.getLogger(Browser.class);

	final WebView browser = new WebView();
	final WebEngine webEngine = browser.getEngine();
    final OpenBrowser openBrowser;
	final File appDirectory; 

	public Browser(File appDirectory, OpenBrowser openBrowser) {
		this.appDirectory = appDirectory;
        this.openBrowser = openBrowser;
        logger.info("WebView version: " + webEngine.getUserAgent());
        logger.info("appDirectory: " + appDirectory);

		//add the web view to the scene
		getChildren().add(browser);

        /*
		// Handle all links by opening external browser
		// http://blogs.kiyut.com/tonny/2013/07/30/javafx-webview-addhyperlinklistener/
		// FIXME: this is a bad hack, should behave similar to HyperLinkListener in JTextPane
		webEngine.locationProperty().addListener(new ChangeListener<String>(){
			@Override
			public void changed(ObservableValue<? extends String> observable, final String oldValue, final String newValue){
				// Links to open in external browser
				if (GUIConstants.isExternalLink(newValue)){
				    // reload the old page
					loadPage(oldValue);
					// open url
					openURLinExternalBrowser(newValue);
				}
			}
		});
        */

	}

    /** Open url in external browser. */
    private void openURLinExternalBrowser(String url){
        if (openBrowser != null){
            logger.info("Open in external browser <" + url +">");
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    openBrowser.openURL(url);
                }
            });
        } else {
            logger.error("No external browser available.");
        }
    }


	/**
	 * Load local resource;
	 */
	public void loadPageFromResource(String resource) {
		File file = new File(appDirectory, resource);
		URI fileURI = file.toURI();
		logger.info("resource to load:" + fileURI);
		loadPage(fileURI.toString());
	}

    /** Load page in browser; */
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
    public void loadText(String text){
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
		layoutInArea(browser,0,0,w,h,0, HPos.CENTER, VPos.CENTER);
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