package org.cy3sbml.gui;

import javax.swing.*;
import java.awt.*;
import java.io.File;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.*;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;

import org.cy3sbml.archive.BundleManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * cy3sbml WebView panel based on javafx.
 * <p>
 * The panel is registered as Cytoscape Results Panel.
 * This panel is the main area for displaying SBML information for the
 * network.
 * <p>
 * BundlePanel is a singleton class.
 */
public class BundlePanel extends JFXPanel implements CytoPanelComponent2,
        RowsSetListener,
        SetCurrentNetworkListener,
        NetworkAddedListener,
        NetworkViewAddedListener,
        NetworkViewAboutToBeDestroyedListener {
    private static final Logger logger = LoggerFactory.getLogger(BundlePanel.class);
    private static final long serialVersionUID = 1L;

    private static BundlePanel uniqueInstance;

    private CyApplicationManager cyApplicationManager;
    private File applicationDirectory;

    private CytoPanel cytoPanelEast;
    private Browser browser;
    private String html;


    /**
     * Singleton.
     */
    public static synchronized BundlePanel getInstance(CySwingApplication cySwingApplication,
                                                       CyApplicationManager cyApplicationManager,
                                                       File applicationDirectory) {
        if (uniqueInstance == null) {
            logger.debug("BundlePanel created");
            uniqueInstance = new BundlePanel(cySwingApplication, cyApplicationManager, applicationDirectory);
        }
        return uniqueInstance;
    }

    public static synchronized BundlePanel getInstance() {
        return uniqueInstance;
    }

    /**
     * Constructor
     */
    private BundlePanel(CySwingApplication cySwingApplication,
                        CyApplicationManager cyApplicationManager,
                        File applicationDirectory) {
        this.cytoPanelEast = cySwingApplication.getCytoPanel(CytoPanelName.EAST);
        this.cyApplicationManager = cyApplicationManager;
        this.applicationDirectory = applicationDirectory;

        setLayout(new BorderLayout());

        JFXPanel fxPanel = this;
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                initFX(fxPanel);
            }
        });
    }

    /**
     * Initialize the JavaFX components.
     * This creates the browser and adds it to the scene.
     */
    private void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        browser = new Browser(applicationDirectory);
        Scene scene = new Scene(browser, 300, 600);
        fxPanel.setScene(scene);
        // necessary to support the detached mode
        Platform.setImplicitExit(false);
    }

    public String getHtml() {
        return html;
    }


    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.EAST;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public Icon getIcon() {
        // return new ImageIcon(getClass().getResource(GUIConstants.ICON_HELP));
        return null;
    }

    @Override
    public String getIdentifier() {
        return "Archive";
    }

    @Override
    public String getTitle() {
        return "Archive ";
    }

    public boolean isActive() {
        return (cytoPanelEast.getState() != CytoPanelState.HIDE);
    }

    /////////////////// ACTIVATION HANDLING ///////////////////////////////////

    public void activate() {
        // If the state of the cytoPanelWest is HIDE, show it
        if (cytoPanelEast.getState() == CytoPanelState.HIDE) {
            cytoPanelEast.setState(CytoPanelState.DOCK);
        }
        // Select panel
        select();
    }

    public void deactivate() {
        // Test if still other Components in Panel, otherwise hide the complete panel
        if (cytoPanelEast.getCytoPanelComponentCount() == 1) {
            cytoPanelEast.setState(CytoPanelState.HIDE);
        }
    }

    public void changeState() {
        if (isActive()) {
            deactivate();
        } else {
            activate();
        }
    }

    public void select() {
        int index = cytoPanelEast.indexOfComponent(this);
        if (index == -1) {
            return;
        }
        if (cytoPanelEast.getSelectedIndex() != index) {
            cytoPanelEast.setSelectedIndex(index);
        }
    }

    /////////////////// INFORMATION DISPLAY ///////////////////////////////////

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
            }
        });
    }



    /////////////////// EVENT HANDLING ///////////////////////////////////

    /**
     * Handle node selection events in the table/network.
     * <p>
     * The RowsSet event is quit broad (happens a lot in network generation and layout, so
     * make sure to minimize the unnecessary action here.
     * I.e. only act on the Event if everything in the right state.
     * <p>
     * RowSetEvent:
     * An Event object generated when an event occurs to a RowSet object. A RowSetEvent object is
     * generated when a single row in a rowset is changed, the whole rowset is changed, or the
     * rowset cursor moves.
     * When an event occurs on a RowSet object, one of the RowSetListener methods will be sent
     * to all registered listeners to notify them of the event. An Event object is supplied to the
     * RowSetListener method so that the listener can use it to find out which RowSet object is
     * the source of the event.
     * <p>
     * http://chianti.ucsd.edu/cytoscape-3.2.1/API/org/cytoscape/model/package-summary.html
     */
    public void handleEvent(RowsSetEvent event) {
        CyNetwork network = cyApplicationManager.getCurrentNetwork();
        if (!event.getSource().equals(network.getDefaultNodeTable()) ||
                !event.containsColumn(CyNetwork.SELECTED)) {
            return;
        }
        updateInformation();
    }

    /**
     * Listening to changes in Networks and NetworkViews.
     */
    @Override
    public void handleEvent(SetCurrentNetworkEvent event) {
        logger.info("SetCurrentNetworkEvent");
        CyNetwork network = event.getNetwork();
        BundleManager bundleManager = BundleManager.getInstance();
        bundleManager.updateCurrent(network);

        updateInformation();
    }

    @Override
    public void handleEvent(NetworkAddedEvent event) {
    }

    @Override
    public void handleEvent(NetworkViewAddedEvent event) {
        updateInformation();
    }

    @Override
    public void handleEvent(NetworkViewAboutToBeDestroyedEvent event) {
    }

    /**
     * Updates panel information within a separate thread.
     */
    public void updateInformation() {
        logger.info("updateInformation()");

        // Only update if active
        if (!this.isActive()) {
            return;
        }

        // Only update if current network and view
        CyNetwork network = cyApplicationManager.getCurrentNetwork();
        CyNetworkView view = cyApplicationManager.getCurrentNetworkView();
        logger.debug("current view: " + view);
        logger.debug("current network: " + network);
        if (network == null || view == null) {
            return;
        }


        // Update the information in separate thread
        try {
            BundlePanelUpdater updater = new BundlePanelUpdater(this, network);
            Thread t = new Thread(updater);
            t.start();
        } catch (Throwable t) {
            logger.error("Error in handling node selection in CyNetwork", t);
            t.printStackTrace();
        }

    }

}
