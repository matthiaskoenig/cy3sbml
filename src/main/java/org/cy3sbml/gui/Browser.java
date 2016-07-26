package org.cy3sbml.gui;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	final File appDirectory; 

	public Browser(File appDirectory) {
		this.appDirectory = appDirectory;
        logger.info("WebView version: " + webEngine.getUserAgent());
        logger.info("appDirectory: " + appDirectory);

		//add the web view to the scene
		getChildren().add(browser);
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