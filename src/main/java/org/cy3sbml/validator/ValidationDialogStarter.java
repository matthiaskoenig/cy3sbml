package org.cy3sbml.validator;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import org.cy3sbml.gui.Browser;
import org.cytoscape.util.swing.OpenBrowser;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Starts the validation dialog.
 */
public class ValidationDialogStarter {
	public static OpenBrowser openBrowser;
    public static File appDirectory;
	
	/* Single reused instance of dialog */
	public static JDialog dialog;
	
	private static void initAndShowGUI(final JFrame parentFrame) {
        // This method is invoked on the EDT thread
		
		if (dialog != null){
			dialog.setVisible(true);
			return;
		}
		dialog = new ValidationDialog(parentFrame);
        
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(fxPanel);
            }
       });
    }

    private static void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
			// Set browser
           Browser browser = new Browser(appDirectory, openBrowser);
            Scene scene = new Scene(browser,300,600);
            fxPanel.setScene(scene);
            // necessary to support the detached mode
            Platform.setImplicitExit(false);
    }
	
    /** 
     * Start the JFXPanel. 
     * Main entry point to open the GUI.
     */
    public static void launch(JFrame parentFrame, OpenBrowser openBrowser, File appDirectory){
    	ValidationDialogStarter.openBrowser = openBrowser;
    	ValidationDialogStarter.appDirectory = appDirectory;
    	
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
    	// It is ecessary to provide access to file resources in a consistent
    	// way in bundle and non-bundle context.
    	// The solution is the use of the ResourceExtractor to use local files.
    	
    	File appDirectory = new File("src/main/resources");

    	// GUI launch without Cytoscape
    	launch(null, null, null);
    }
}
