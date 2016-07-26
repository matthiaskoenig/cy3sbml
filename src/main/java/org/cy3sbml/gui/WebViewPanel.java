package org.cy3sbml.gui;


import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import org.cy3sbml.SBMLManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * cy3sbml results panel. 
 * 
 * The panel is registered as Cytoscape Results Panel and available
 * from within the GUI.
 * 
 * This panel is the main area for displaying SBML information for the 
 * network.
 * 
 * ResultsPanel is a singleton class.
 */
public class WebViewPanel extends JFXPanel implements CytoPanelComponent2, SBMLPanel,
        HyperlinkListener,
        RowsSetListener,
        SetCurrentNetworkListener,
        NetworkAddedListener,
        NetworkViewAddedListener,
        NetworkViewAboutToBeDestroyedListener {
	private static final Logger logger = LoggerFactory.getLogger(WebViewPanel.class);
	private static final long serialVersionUID = 1L;

	private static WebViewPanel uniqueInstance;
	private CytoPanel cytoPanelEast;
	private CySwingApplication cySwingApplication;
    private CyApplicationManager cyApplicationManager;
    private Browser browser;
    private File appDirectory;
	private long lastInformationThreadId = -1;


	/** Singleton. */
	public static synchronized WebViewPanel getInstance(CyApplicationManager cyApplicationManager,
                                                        CySwingApplication cySwingApplication, File appDirectory){
		if (uniqueInstance == null){
			logger.info("WebViewPanel created");
			uniqueInstance = new WebViewPanel(cyApplicationManager, cySwingApplication, appDirectory);
		}
		return uniqueInstance;
	}
	public static synchronized WebViewPanel getInstance(){
		return uniqueInstance;
	}

	/** Constructor */
	private WebViewPanel(CyApplicationManager cyApplicationManager, CySwingApplication cySwingApplication,
                         File appDirectory){
		this.cyApplicationManager = cyApplicationManager;
		this.cySwingApplication = cySwingApplication;
        this.appDirectory = appDirectory;
		this.cytoPanelEast = cySwingApplication.getCytoPanel(CytoPanelName.EAST);

		setLayout(new BorderLayout());
        // TODO: set dimension of panel


		JFXPanel fxPanel = this;
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initFX(fxPanel);
                // set the help text
                // load default page
                browser.loadPage("http://www.google.com");
                setHelp();
			}
		});

        // FIXME
		// textPane = new JEditorPaneSBML();
		// textPane.addHyperlinkListener(this);

	}

    /**
     * Initialize the JavaFX components.
     */
    private void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        browser = new Browser(appDirectory);
        Scene scene = new Scene(browser);
        fxPanel.setScene(scene);
        Platform.setImplicitExit(false);
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
        return "javafx";
    }

	@Override
	public String getTitle() {
		return "javafx  ";
	}

	public boolean isActive(){
		return (cytoPanelEast.getState() != CytoPanelState.HIDE);
	}

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
	 * Handle hyperlink events in the textPane.
	 * Either opens browser for given hyperlink or triggers Cytoscape actions
	 * for subsets of special hyperlinks.
	 * 
	 * This provides an easy solution for integrating app functionality
	 * with click on hyperlinks.
	 */
	@Override
	public void hyperlinkUpdate(HyperlinkEvent evt) {

	}

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
		CyNetwork network = cyApplicationManager.getCurrentNetwork();
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
		ResultsPanel.getInstance().updateInformation();
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
		ResultsPanel.getInstance().updateInformation();
	}

	@Override
	public void handleEvent(NetworkViewAboutToBeDestroyedEvent event) {
		ResultsPanel.getInstance().setHelp();
	}


	/** Update information within a separate thread. */
	public void updateInformation(){
		logger.info("updateInformation()");
		CyNetwork network = cyApplicationManager.getCurrentNetwork();
		CyNetworkView view = cyApplicationManager.getCurrentNetworkView();
		logger.debug("current view: " + view);
		logger.debug("current network: " + network);
		if (network == null || view == null){
			return;
		}

		// Update the information in separate thread
        if (!this.isActive()){
            this.setText("");
            return;
        }

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
