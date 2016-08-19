package org.cy3sbml.validator;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.gui.Browser;
import org.cy3sbml.gui.GUIConstants;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;


/**
 * cy3sbml validation panel.
 * TODO: implement generic WebView CytoPanelComponent.
 */
public class ValidationPanel extends JFXPanel implements CytoPanelComponent2,
        SetCurrentNetworkListener,
        NetworkAddedListener,
        NetworkViewAddedListener,
        NetworkViewAboutToBeDestroyedListener {
	private static final Logger logger = LoggerFactory.getLogger(ValidationPanel.class);
	private static final long serialVersionUID = 1L;

	private static ValidationPanel uniqueInstance;


    private ServiceAdapter adapter;
    private CytoPanel cytoPanelEast;
	private Browser browser;
    private String html;


	/** Singleton. */
	public static synchronized ValidationPanel getInstance(ServiceAdapter adapter){
		if (uniqueInstance == null){
			logger.debug("ValidationPanel created");
			uniqueInstance = new ValidationPanel(adapter);
		}
		return uniqueInstance;
	}

	/**
	 * Get the unique instance.
	 */
	public static synchronized ValidationPanel getInstance(){
		return uniqueInstance;
	}

	/** Constructor */
	private ValidationPanel(ServiceAdapter adapter){
		this.cytoPanelEast = adapter.cySwingApplication.getCytoPanel(CytoPanelName.EAST);
        this.adapter = adapter;

		setLayout(new BorderLayout());

		JFXPanel fxPanel = this;
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initFX(fxPanel);
                setEmptyValidation();
			}
		});
	}

    /**
     * Initialize the JavaFX components.
     * This creates the browser and adds it to the scene.
     */
    private void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        browser = new Browser(adapter.cy3sbmlDirectory, adapter.openBrowser);
        Scene scene = new Scene(browser,300,600);
        fxPanel.setScene(scene);
        // necessary to support the detached mode
        Platform.setImplicitExit(false);
    }

    public ServiceAdapter getAdapter() {
        return adapter;
    }
    public String getHtml() { return html; }


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
        return new ImageIcon(getClass().getResource(GUIConstants.ICON_VALIDATION));
    }

    @Override
    public String getIdentifier() {
        return "validation";
    }

	@Override
	public String getTitle() {
		return "validation ";
	}

	public boolean isActive(){
		return (cytoPanelEast.getState() != CytoPanelState.HIDE);
	}

    /////////////////// ACTIVATION HANDLING ///////////////////////////////////

    public void activate(){
		// If the state of the cytoPanelWest is HIDE, show it
		if (cytoPanelEast.getState() == CytoPanelState.HIDE) {
			cytoPanelEast.setState(CytoPanelState.DOCK);
		}	
		// Select panel
		select();
    }
		
	public void deactivate(){
		// Test if still other Components in Panel, otherwise hide the complete panel
		if (cytoPanelEast.getCytoPanelComponentCount() == 1){
			cytoPanelEast.setState(CytoPanelState.HIDE);
		}
	}

	public void changeState(){
		if (isActive()){
			deactivate();
		} else {
			activate();
		}
	}
	
	public void select(){
		int index = cytoPanelEast.indexOfComponent(this);
		if (index == -1) {
			return;
		}
		if (cytoPanelEast.getSelectedIndex() != index){
			cytoPanelEast.setSelectedIndex(index);
		}
	}

    /////////////////// INFORMATION DISPLAY ///////////////////////////////////

    public void setEmptyValidation(){
        browser.loadPageFromResource(GUIConstants.HTML_VALIDATION_RESOURCE);
    }

	/** Set text. */
	public void setText(String text){
	    html = text;
		// Necessary to use invokeLater to handle the Swing GUI update
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				browser.loadText(text);
			}
		});
	}

	/////////////////// HANDLE EVENTS ///////////////////////////////////

	/**
	 * Listening to changes in Networks and NetworkViews.
	 * When must the SBMLDocument store be updated.
	 * - NetworkViewAddedEvent
	 * - NetworkViewDestroyedEvent
	 *
	 * An event indicating that a network view has been set to current.
	 *  SetCurrentNetworkViewEvent
	 *
	 * An event signaling that the a network has been set to current.
	 *  SetCurrentNetworkEvent
	 */
	@Override
	public void handleEvent(SetCurrentNetworkEvent event) {
		CyNetwork network = event.getNetwork();
		SBMLManager.getInstance().updateCurrent(network);
		updateInformation();
	}

	/** If networks are added check if they are subnetworks
	 * of SBML networks and add the respective SBMLDocument
	 * to them in the mapping.
	 * Due to the mapping based on the RootNetworks sub-networks
	 * automatically can use the mappings of the parent networks.
	 */
	@Override
	public void handleEvent(NetworkAddedEvent event) {}

	@Override
	public void handleEvent(NetworkViewAddedEvent event){
		updateInformation();
	}

	@Override
	public void handleEvent(NetworkViewAboutToBeDestroyedEvent event) {
		setEmptyValidation();
	}


	/**
     * Updates panel information within a separate thread.
     */
	public void updateInformation(){
		logger.debug("updateInformation()");

        // Only update if active
        if (!this.isActive()){
            return;
        }

        // TODO: set the current validation object.
        logger.info("TODO: set current validation information");
	}

}
