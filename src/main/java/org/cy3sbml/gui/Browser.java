package org.cy3sbml.gui;

import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.layout.Region;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.io.File;

/**
 * Browser for displaying HTML.
 * This can be embedded in Swing using a JFXPanel.
 */
public class Browser extends Region {

	final WebView browser = new WebView();
	final WebEngine webEngine = browser.getEngine();
	final File appDirectory; 

	public Browser(File appDirectory) {
		this.appDirectory = appDirectory;
		//apply the styles
		// getStyleClass().add("browser");
		
		// external URLs work
		// webEngine.load("http://sabiork.h-its.org/newSearch/index");
		
		// String content works
        // webEngine.loadContent("<html><h1>Hello world</h1></html>");
		
		// Resource content does not work
		// URL queryURL = getClass().getResource("/gui/query.html");
		// System.out.println(queryURL);    
        // loadPage(queryURL.toString());
        
		/*
        // load local resource
        File file = new File(appDirectory + "/gui/query.html");
		URI fileURI = file.toURI();
		System.out.println(fileURI);    
        loadPage(fileURI.toString());
        
        System.out.println("app directory: " + appDirectory.getAbsolutePath());
    	*/

        System.out.println("WebView version: " + webEngine.getUserAgent());

        // webEngine.load("http://www.google.com");
		loadPage("http://www.google.com");

		//add the web view to the scene
		getChildren().add(browser);

	}

    /**
     * Load page in browser;
     */
	private void loadPage(String url) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                webEngine.load(url);
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