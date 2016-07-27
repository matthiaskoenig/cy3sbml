package org.cy3sbml.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Run the webView example.
 */
public class BrowserSample extends Application {

	private Scene scene;
	@Override public void start(Stage stage) {
		// create the scene
		stage.setTitle("Web View");
		Browser browser = new Browser(null, null);
		scene = new Scene(browser,900,600, Color.web("#666970"));
		stage.setScene(scene);
		stage.show();
		browser.loadPage("http://www.google.com");
	}

	public static void main(String[] args){
		launch(args);
	}
}