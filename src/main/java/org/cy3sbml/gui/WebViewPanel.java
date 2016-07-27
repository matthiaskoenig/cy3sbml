package org.cy3sbml.gui;

import java.util.HashSet;
import java.util.Set;
import java.awt.*;
import javax.swing.*;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.cy3sbml.ServiceAdapter;
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

import org.cy3sbml.SBMLManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * cy3sbml WebView panel based on javafx.
 * 
 * The panel is registered as Cytoscape Results Panel.
 * This panel is the main area for displaying SBML information for the 
 * network.
 * 
 * WebViewPanel is a singleton class.
 */
public class WebViewPanel extends JFXPanel implements CytoPanelComponent2, SBMLPanel,
        RowsSetListener,
        SetCurrentNetworkListener,
        NetworkAddedListener,
        NetworkViewAddedListener,
        NetworkViewAboutToBeDestroyedListener {
	private static final Logger logger = LoggerFactory.getLogger(WebViewPanel.class);
	private static final long serialVersionUID = 1L;

	private static WebViewPanel uniqueInstance;


    private ServiceAdapter adapter;
    private CytoPanel cytoPanelEast;


    private Browser browser;

    /*
    private CySwingApplication cySwingApplication;
    private CyApplicationManager cyApplicationManager;
    private OpenBrowser openBrowser;
    private File appDirectory;
    */

	private long lastInformationThreadId = -1;


	/** Singleton. */
	public static synchronized WebViewPanel getInstance(ServiceAdapter adapter){
		if (uniqueInstance == null){
			logger.info("WebViewPanel created");
			uniqueInstance = new WebViewPanel(adapter);
		}
		return uniqueInstance;
	}
	public static synchronized WebViewPanel getInstance(){
		return uniqueInstance;
	}

	/** Constructor */
	private WebViewPanel(ServiceAdapter adapter){
        this.adapter = adapter;
		this.cytoPanelEast = adapter.cySwingApplication.getCytoPanel(CytoPanelName.EAST);

		setLayout(new BorderLayout());
        // TODO: set dimension of panel

		JFXPanel fxPanel = this;
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initFX(fxPanel);
                // set the help text
                setHelp();
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
        return new ImageIcon(getClass().getResource(GUIConstants.IMAGE_CY3SBML_ICON));
    }

    @Override
    public String getIdentifier() {
        return "cy3sbml";
    }

	@Override
	public String getTitle() {
		return "cy3sbml  ";
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
        setHelp();
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

    public void setHelp(){
        browser.loadPageFromResource(GUIConstants.HTML_HELP_RESOURCE);
    }

    public void setExamples(){
        browser.loadPageFromResource(GUIConstants.HTML_EXAMPLE_RESOURCE);
    }

	/** Set text. */
	@Override
	public void setText(String text){
		// Necessary to use invokeLater to handle the Swing GUI update
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				browser.loadText(text);
			}
		});
	}

	/**
	 * Update Text in the navigation panel.
	 * Only updates information if the current thread is the last requested thread
	 * for updating text.
	 */
	@Override
	public void setText(SBaseInfoThread infoThread){
		if (infoThread.getId() == lastInformationThreadId){
			this.setText(infoThread.info);
		}
	}

	/**
	 * Create information string for SBML Node and display.
	 */
	@Override
	public void showSBaseInfo(Object obj) {
        logger.info("showSBaseInfo for Object");
		Set<Object> objSet = new HashSet<Object>();
		objSet.add(obj);
		showSBaseInfo(objSet);
	}

	/**
	 * Display information for set of nodes.
	 */
	@Override
	public void showSBaseInfo(Set<Object> objSet) {
	    logger.info("showSBaseInfo for Set<Object>");
		this.setText("Retrieving information via WebServices ...");
		// starting threads for webservice calls
		SBaseInfoThread thread = new SBaseInfoThread(objSet, this);
		lastInformationThreadId = thread.getId();
		thread.start();
	}


	/////////////////// HANDLE EVENTS ///////////////////////////////////


	/** 
	 * Handle node selection events in the table/network. 
	 * 
	 * The RowsSet event is quit broad (happens a lot in network generation and layout, so 
	 * make sure to minimize the unnecessary action here.
	 * I.e. only act on the Event if everything in the right state.
	 * 
	 * RowSetEvent:
	 * An Event object generated when an event occurs to a RowSet object. A RowSetEvent object is 
	 * generated when a single row in a rowset is changed, the whole rowset is changed, or the 
	 * rowset cursor moves.
	 * When an event occurs on a RowSet object, one of the RowSetListener methods will be sent 
	 * to all registered listeners to notify them of the event. An Event object is supplied to the 
	 * RowSetListener method so that the listener can use it to find out which RowSet object is 
	 * the source of the event.
	 * 
	 * http://chianti.ucsd.edu/cytoscape-3.2.1/API/org/cytoscape/model/package-summary.html
	 */
	public void handleEvent(RowsSetEvent event) {
		CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
		if (!event.getSource().equals(network.getDefaultNodeTable()) ||
	            !event.containsColumn(CyNetwork.SELECTED)){
		    return;
		}
		updateInformation();
	}

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
		setHelp();
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

        // Only update if current network and view
		CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
		CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
		logger.debug("current view: " + view);
		logger.debug("current network: " + network);
		if (network == null || view == null){
			return;
		}

        // Update the information in separate thread
		try {
			UpdatePanel updater = new UpdatePanel(this, network);
			Thread t = new Thread(updater);
			t.start();	
		} catch (Throwable t){
			logger.error("Error in handling node selection in CyNetwork");
			t.printStackTrace();
		}
	}

}