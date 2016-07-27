package org.cy3sbml.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent2;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
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
import org.cytoscape.work.TaskIterator;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.actions.ExamplesAction;
import org.cy3sbml.actions.ImportAction;
import org.cy3sbml.actions.ValidationAction;
import org.cy3sbml.biomodel.BioModelDialog;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.cy3sbml.util.GUIUtil.openCurrentSBMLInBrowser;

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
public class ResultsPanel extends JPanel implements CytoPanelComponent2,
        HyperlinkListener,
        RowsSetListener,
        SetCurrentNetworkListener,
        NetworkAddedListener,
        NetworkViewAddedListener,
        NetworkViewAboutToBeDestroyedListener {
	private static final Logger logger = LoggerFactory.getLogger(ResultsPanel.class);
	private static final long serialVersionUID = 1L;

	private static ResultsPanel uniqueInstance;
	private CytoPanel cytoPanelEast;
	private ServiceAdapter adapter;
	private JEditorPaneSBML textPane;


	/** Singleton. */
	public static synchronized ResultsPanel getInstance(ServiceAdapter adapter){
		if (uniqueInstance == null){
			logger.debug("ResultsPanel created");
			uniqueInstance = new ResultsPanel(adapter);
		}
		return uniqueInstance;
	}
	public static synchronized ResultsPanel getInstance(){
		return uniqueInstance;
	}
	
	/** Constructor */
	private ResultsPanel(ServiceAdapter adapter){
		this.adapter = adapter;
		this.cytoPanelEast = adapter.cySwingApplication.getCytoPanel(CytoPanelName.EAST);
		
		// SBML information area
		setLayout(new BorderLayout(0, 0));
		
		textPane = new JEditorPaneSBML();
		textPane.addHyperlinkListener(this);
		
		JScrollPane annotationScrollPane = new JScrollPane();
		annotationScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		annotationScrollPane.setViewportView(textPane);
		this.add(annotationScrollPane);
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
		return "cy3sbml";
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
		
	public JEditorPaneSBML getTextPane(){
		return textPane;
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
	public void hyperlinkUpdate(HyperlinkEvent evt) {
		/* Open link in browser. */
		URL url = evt.getURL();
		if (url != null) {
			if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
				
			} else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
				
			} else if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				
				String s = url.toString();
				
				// BioModels
				if (s.equals(GUIConstants.URL_BIOMODELS)){
					 BioModelDialog bioModelsDialog = BioModelDialog.getInstance(adapter);
					 bioModelsDialog.setVisible(true);
				}
				// ChangeState
				else if (s.equals(GUIConstants.URL_CHANGESTATE)){
					ResultsPanel panel = ResultsPanel.getInstance();
					panel.changeState();
				}
				// Import
				else if (s.equals(GUIConstants.URL_IMPORT)){
					ImportAction importAction = new ImportAction(adapter);
					importAction.actionPerformed(null);
				}
				// Validation
				else if (s.equals(GUIConstants.URL_VALIDATION)){
					ValidationAction.openValidationPanel(adapter);
				}
				// Examples
				else if (s.equals(GUIConstants.URL_EXAMPLES)){
					ExamplesAction examplesAction = new ExamplesAction(adapter.cySwingApplication);
					examplesAction.actionPerformed(null);
				}
				// Example networks
				else if (GUIConstants.URLS_EXAMPLE_SBML.containsKey(s)){
					String resource = GUIConstants.URLS_EXAMPLE_SBML.get(s);
					loadExampleFromResource(resource);
				}
				// SBML file
				else if (s.equals(GUIConstants.URL_SBMLFILE)){
					openCurrentSBMLInBrowser(adapter.openBrowser);
				}
				// HTML links	
				else {
					adapter.openBrowser.openURL(url.toString());	
				}
			}
		}
	}


	/**
	 * Loads an SBML example file from the given resource.
	 * Needs access to the LoadNetworkFileTaskFaktory and the SynchronousTaskManager.
	 *
	 * TODO: make this a general function.
	 *
	 * @param resource
     */
	private void loadExampleFromResource(String resource){
		InputStream instream = getClass().getResourceAsStream(resource);
		File tempFile;
		try {
			tempFile = File.createTempFile("tmp-example", ".xml");
			tempFile.deleteOnExit();
			FileOutputStream out = new FileOutputStream(tempFile);
			IOUtils.copy(instream, out);
			
			// read the file
			TaskIterator iterator = adapter.loadNetworkFileTaskFactory.createTaskIterator(tempFile);
			adapter.synchronousTaskManager.execute(iterator);	
		} catch (Exception e) {
			logger.warn("Could not read example");
			e.printStackTrace();
		}
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
		logger.debug("updateInformation()");
		CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
		CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
		logger.debug("current view: " + view);
		logger.debug("current network: " + network);
		if (network == null || view == null){
			return;
		}

		// Update the information in separate thread
		if (!this.isActive()){
			this.textPane.setText("");
			return;
		}

		// Update the information in separate thread
		try {
			UpdatePanel updater = new UpdatePanel(this.textPane, network);
			Thread t = new Thread(updater);
			t.start();	
		} catch (Throwable t){
			logger.error("Error in handling node selection in CyNetwork");
			t.printStackTrace();
		}
	}
	
	/** Set help information. */
	public void setHelp(){
		textPane.setHelp();
	}
	
}
