package org.cy3sbml.validator;


import java.awt.*;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.*;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.gui.Browser;
import org.cy3sbml.gui.GUIConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SBML Validation Display.
 * JavaFX window displaying validation messages.
 */
public class ValidationFrame extends JFrame implements SetCurrentNetworkListener,
        NetworkAddedListener,
        NetworkViewAddedListener,
        NetworkViewAboutToBeDestroyedListener {

    private static final Logger logger = LoggerFactory.getLogger(ValidationFrame.class);
    private static ValidationFrame uniqueInstance;

    private ServiceAdapter adapter;
    private Browser browser;
    private String html;


    /**
     * Singleton.
     */
    public static synchronized ValidationFrame getInstance(ServiceAdapter adapter) {
        if (uniqueInstance == null) {
            logger.debug("ValidationFrame created");
            uniqueInstance = new ValidationFrame(adapter);
        }
        return uniqueInstance;
    }

    /**
     * Constructor.
     * @param adapter
     */
    private ValidationFrame(ServiceAdapter adapter) {
        super();
        this.adapter = adapter;
        this.setTitle("SBML validation");

        try {
            this.setIconImage(ImageIO.read(getClass().getResource(GUIConstants.ICON_VALIDATION)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        final JFXPanel fxPanel = new JFXPanel();
        this.add(fxPanel);

        int width = 1400;
        int height = 1000;
        this.setPreferredSize(new Dimension(width, height));
        this.setSize(new Dimension(width, height));
        this.setResizable(true);

        this.setBackground(new Color(255, 255, 255));
        this.setLocationRelativeTo(adapter.cySwingApplication.getJFrame());
        this.setAlwaysOnTop(false);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(fxPanel);
                resetInformation();
            }
        });
    }

    /**
     * Get current HTML of the webview.
     * @return html
     */
    public String getHtml() {
        return html;
    }

    /**
     * Initialize the JavaFX components.
     * This creates the browser and adds it to the scene.
     */
    private void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        browser = new Browser(adapter.cy3sbmlDirectory);
        Scene scene = new Scene(browser, 300, 600);
        fxPanel.setScene(scene);
        // necessary to support the detached mode
        Platform.setImplicitExit(false);
    }


    /// INFORMATION DISPLAY ///

    /**
     * Reset information in webview.
     */
    public void resetInformation() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                html = null;
                browser.loadPageFromResource(GUIConstants.HTML_VALIDATION_RESOURCE);
            }
        });
    }

    /**
     * Set text.
     */
    public void setText(String text) {
        html = text;
        // Necessary to use invokeLater to handle the Swing GUI update
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                browser.loadText(text);
                ValidationFrame.super.setVisible(true);
                ValidationFrame.super.toFront();
            }
        });
    }

    /// EVENT HANDLING ///
    @Override
    public void handleEvent(SetCurrentNetworkEvent event) {
    }

    @Override
    public void handleEvent(NetworkAddedEvent event) {
    }

    @Override
    public void handleEvent(NetworkViewAddedEvent event) {
    }

    @Override
    public void handleEvent(NetworkViewAboutToBeDestroyedEvent event) {
    }

}
