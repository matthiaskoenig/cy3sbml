package org.cy3sbml.biomodelrest.gui;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import org.cy3sbml.ResourceExtractor;
import org.cy3sbml.biomodelrest.BiomodelsSBMLReader;

import org.cytoscape.util.swing.OpenBrowser;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;


@SuppressWarnings("restriction")
public class WebViewSwing {
	public static OpenBrowser openBrowser;
	public static BiomodelsSBMLReader sbmlReader;
	
	/* Single reused instance of dialog */
	public static JDialog dialog;
	
	private static void initAndShowGUI(final JFrame parentFrame) {
        // This method is invoked on the EDT thread
		
		if (dialog != null){
			dialog.setVisible(true);
			return;
		}
		
        dialog = new JDialog(parentFrame);
        dialog.setTitle("Biomodels Web Services");
        
        // use values from Scene Builder
        int width = 1400;
        int height = 990;
        
        final JFXPanel fxPanel = new JFXPanel();
        
        dialog.add(fxPanel);
        dialog.setSize(width, height);
        dialog.setVisible(true);
        dialog.setBackground(new Color(255, 255, 255));
        dialog.setLocationRelativeTo(parentFrame);
        dialog.setResizable(false);
        
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(fxPanel);
            }
       });
    }

    private static void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        
		try {
			// Set browser
	    	// Scene scene = new Scene(new Browser(appDirectory));
	        // fxPanel.setScene(scene);
			
			// Load FXML GUI scene
			// see : http://blog.admadic.com/2013/03/javafx-fxmlloader-with-osgi.html
			FXMLLoader.setDefaultClassLoader(WebViewSwing.class.getClassLoader());
			
			FXMLLoader loader = new FXMLLoader(WebViewSwing.class.getResource("/biomodels/gui/query.fxml"));
			ScrollPane root = loader.load();

			QueryFXMLController controller = loader.getController();
			
			// controller.initData();
		    Scene scene = new Scene(root);
		    
		    // from appDirectory
            URI cssURI = ResourceExtractor.fileURIforResource("/biomodels/gui/query.css");
		    String cssFile = cssURI.toString();
		    scene.getStylesheets().add(cssFile);
		    
		    fxPanel.setScene(scene);
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
    /** 
     * Start the JFXPanel. 
     * Main entry point to open the GUI.
     */
    public static void launch(JFrame parentFrame, OpenBrowser openBrowser, BiomodelsSBMLReader sbmlReader){
    	WebViewSwing.openBrowser = openBrowser;
    	WebViewSwing.sbmlReader = sbmlReader;
    	
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                initAndShowGUI(parentFrame);
            }
        });
    }
    
    
    /** 
     * Testing the GUI without Cytoscape specific functionality.
     * Run this main to use the SABIO-RK webservice without the
     * Cytoscape backend.
     * Allows to test functionality which currently cannot be backed
     * in the OSGI bundles. 
     */
    public static void main(String[] args) {
    	// It is necessary to provide access to file resources in a consistent
    	// way in bundle and non-bundle context.
    	// The solution is the use of the ResourceExtractor to use local files.
    	
    	File appDirectory = new File("src/main/resources");
    	ResourceExtractor.setAppDirectory(appDirectory);
    	
    	// GUI launch without Cytoscape
    	launch(null, null, null);
    }
}
