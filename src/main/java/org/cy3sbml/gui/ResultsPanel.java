package org.cy3sbml.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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
import javax.xml.stream.XMLStreamException;

import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.TidySBMLWriter;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.actions.ExamplesAction;
import org.cy3sbml.actions.ImportAction;
import org.cy3sbml.actions.ValidationAction;
import org.cy3sbml.biomodel.BioModelDialog;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class ResultsPanel extends JPanel implements CytoPanelComponent, HyperlinkListener, RowsSetListener{
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
		return new ImageIcon(getClass().getResource("/images/cy3sbml_icon.png"));
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
				if (s.equals("http://cy3sbml-biomodels")){
					 BioModelDialog bioModelsDialog = BioModelDialog.getInstance(adapter);
					 bioModelsDialog.setVisible(true);
				}
				// ChangeState
				else if (s.equals("http://cy3sbml-changestate")){
					ResultsPanel panel = ResultsPanel.getInstance();
					panel.changeState();
				}
				// Import
				else if (s.equals("http://cy3sbml-import")){
					ImportAction importAction = new ImportAction(adapter);
					importAction.actionPerformed(null);
				}
				// Validation
				else if (s.equals("http://cy3sbml-validation")){
					ValidationAction.openValidationPanel(adapter);
				}
				// Examples
				else if (s.equals("http://cy3sbml-examples")){
					ExamplesAction examplesAction = new ExamplesAction(adapter.cySwingApplication);
					examplesAction.actionPerformed(null);
				}
				
				// Example networks
				else if (s.equals("http://cy3sbml-glucose")){
					loadExampleFromResource("/models/Koenig2014_Glucose_Metabolism.xml");
				}else if (s.equals("http://cy3sbml-galactose")){
					loadExampleFromResource("/models/Galactose_v129_Nc1_core.xml");
				}else if (s.equals("http://cy3sbml-HepatoNet1")){
					loadExampleFromResource("/models/HepatoNet1.xml");
				}else if (s.equals("http://cy3sbml-e_coli_core")){
					loadExampleFromResource("/models/e_coli_core.xml");
				}else if (s.equals("http://cy3sbml-iAB_RBC_283")){
					loadExampleFromResource("/models/iAB_RBC_283.xml");
				}else if (s.equals("http://cy3sbml-iIT341")){
					loadExampleFromResource("/models/iIT341.xml");
				}else if (s.equals("http://cy3sbml-RECON1")){
					loadExampleFromResource("/models/RECON1.xml");
				}else if (s.equals("http://cy3sbml-BIOMD0000000016")){
					loadExampleFromResource("/models/BIOMD0000000016.xml");
				}else if (s.equals("http://cy3sbml-BIOMD0000000084")){
					loadExampleFromResource("/models/BIOMD0000000084.xml");
				}else if (s.equals("http://cy3sbml-hsa04360")){
					loadExampleFromResource("/models/hsa04360.xml");
				}
				
				// SBML Document
				else if (s.equals("http://sbml-file")){
					SBMLManager sbmlManager = SBMLManager.getInstance();
					SBMLDocument doc = sbmlManager.getCurrentSBMLDocument();
					 //create a temp file
			    	File temp;
					try {
						temp = File.createTempFile("temp-file-name", ".xml");
						System.out.println("Temp file : " + temp.getAbsolutePath());
						try {
							TidySBMLWriter.write(doc, temp.getAbsolutePath(), ' ', (short) 2);
							adapter.openBrowser.openURL("file://" + temp.getAbsolutePath());
						} catch (SBMLException | FileNotFoundException | XMLStreamException e) {
							e.printStackTrace();
						}
					} catch (IOException e) {
						e.printStackTrace();
					} 
				}
				
				// HTML links	
				else {
					// handle the HTML links
					adapter.openBrowser.openURL(url.toString());	
				}
			}
		}
	}
	
	private void loadExampleFromResource(String resource){
		// load the example network
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
	
	/** Handle node selection events in the table/network. 
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
	
	/*
	 * Updates information within a separate thread.
	 */
	public void updateInformation(){
		logger.info("updateInformation()");
		CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
		CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
		logger.info("current view: " + view);
		logger.info("current network: " + network);
		if (network == null || view == null){
			return;
		}
		// Update the information in separate thread
		select();
		try {
			UpdatePanelInformation updater = new UpdatePanelInformation(this, network);
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
