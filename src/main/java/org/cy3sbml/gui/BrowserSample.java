package org.cy3sbml.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * Run the browser example.
 */
public class BrowserSample extends Application {

	private Scene scene;
	@Override public void start(Stage stage) {
		// create the scene
		stage.setTitle("Web View");
		scene = new Scene(new Browser(null),900,600, Color.web("#666970"));
		stage.setScene(scene);
		// scene.getStylesheets().add("webviewsample/BrowserToolbar.css");
		stage.show();
	}

	public static void main(String[] args){
		launch(args);
	}
}