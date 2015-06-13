package org.cy3sbml.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cytoscape.application.swing.CytoPanel;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.application.swing.CytoPanelState;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Results Panel for cy3sbml registered as Cytoscape Results Panel.
 * This is the main display area for information.
 */
public class ResultsPanel extends JPanel implements CytoPanelComponent, HyperlinkListener, RowsSetListener{
	private static final Logger logger = LoggerFactory.getLogger(ResultsPanel.class);
	private static final long serialVersionUID = 1L;

	CytoPanel cytoPanelEast;
	private static ResultsPanel uniqueInstance;
	
	private ServiceAdapter adapter;
	private SBMLManager sbmlManager;
	
	private JEditorPaneSBML textPane;

	
	public static synchronized ResultsPanel getInstance(ServiceAdapter adapter){
		if (uniqueInstance == null){
			logger.info("ControlPanel created");
			uniqueInstance = new ResultsPanel(adapter);
		}
		return uniqueInstance;
	}
	public static synchronized ResultsPanel getInstance(){
		return uniqueInstance;
	}
	
	/** Constructor of cy3sbml Results Panel. */
	private ResultsPanel(ServiceAdapter adapter){
		this.adapter = adapter;
		this.cytoPanelEast = adapter.cySwingApplication.getCytoPanel(CytoPanelName.EAST);
		this.sbmlManager = SBMLManager.getInstance();
		
		// SBML information area
		setLayout(new BorderLayout(0, 0));
		
		textPane = new JEditorPaneSBML();
		textPane.addHyperlinkListener(this);
		JScrollPane annotationScrollPane = new JScrollPane();
		annotationScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		annotationScrollPane.setViewportView(textPane);
		this.add(textPane);
		
		// set the size
		Dimension size = this.getSize();
		this.setSize(400, size.height);
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
		// return null;
	}

	@Override
	public String getTitle() {
		return "cy3sbml ";
	}
	
	public boolean isActive(){
		return (cytoPanelEast.getState() != CytoPanelState.HIDE);
	}

    public void activate(){
    	logger.info("Activate cy3sbml ControlPanel");
		// If the state of the cytoPanelWest is HIDE, show it
		if (cytoPanelEast.getState() == CytoPanelState.HIDE) {
			cytoPanelEast.setState(CytoPanelState.DOCK);
		}	
		// Select panel
		select();
    }
		
	public void deactivate(){
		logger.info("Deactivate cy3sbml ControlPanel");
		// Test if still other Components in Control Panel, otherwise hide
		// the complete panel
		if (cytoPanelEast.getCytoPanelComponentCount() == 1){
			cytoPanelEast.setState(CytoPanelState.HIDE);
		}
	}

	public void select(){
		int index = cytoPanelEast.indexOfComponent(this);
		if (index == -1) {
			return;
		}
		cytoPanelEast.setSelectedIndex(index);
	}
		
	public JEditorPaneSBML getTextPane(){
		return textPane;
	}
	
	/////////////////// HANDLE EVENTS ///////////////////////////////////

	/** Handle hyperlink events in the info TextPane. */
	public void hyperlinkUpdate(HyperlinkEvent evt) {
		/* Open link in browser. */
		URL url = evt.getURL();
		if (url != null) {
			if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
				
			} else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
				
			} else if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				adapter.openBrowser.openURL(url.toString());
			}
		}	
	}
	
	/** Handle node selection events in the table/network. 
	 * The RowsSet event is quit broad (happens a lot in network generation and layout, so 
	 * make sure to minimize the unnecessary action here.
	 * I.e. only act on the Event if everything in the right state.
	 * 
	 * The Event is fired many times (N+1) for selected nodes. 
	 * TODO: necessary to get event which is only fired once upon selection !
	 * FIXME: this is major performance problem in large networks.
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
		try {
			if (!isActive()){
				textPane.setText("");
				return;
			}
			CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
			CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
			if (network == null || view == null){
				return;
			}
		
			if (!event.getSource().equals(network.getDefaultNodeTable()) ||
		            !event.containsColumn(CyNetwork.SELECTED)){
			    return;
			}
		        			
			LinkedList<Long> suids = new LinkedList<Long>();
			List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
			for (CyNode n : nodes){
				suids.add(n.getSUID());
			}
			
			// get selected nodes
			logger.info("--- SELECTION ---");
			for (Long suid: suids){
				logger.info(suid.toString());
			}
			logger.info("-----------------");
			
			// display information for selected nodes
			SBMLDocument document = sbmlManager.getCurrentSBMLDocument();
			if (document != null){				
				List<String> selectedNSBIds = getNSBIds(suids);
			
				// display information
				if (selectedNSBIds.size() > 0){
					// TODO: better handling of multiple selections
					// only use the first in the row
					String nsbId = selectedNSBIds.get(0);
					NamedSBase nsb = sbmlManager.getNamedSBaseById(nsbId);
					textPane.showNSBInfo(nsb);
				}
			} else {
				textPane.setText("no SBML information");
			}
		
		} catch (Throwable t){
			logger.error("Error in handling node selection in CyNetwork");
			t.printStackTrace();
		}
	}
	
	/*
	public void handleEvent2(RowsSetEvent event) {
		
		// Filter out many RowsSetEvents I do not care about
		CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
		CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
		if (network == null || view == null){
			return;
		}
		if (!event.getSource().equals(network.getDefaultNodeTable()) ||
	            !event.containsColumn(CyNetwork.SELECTED)){
		    return;
		}
		
		logger.info("--- ROWS_SET_EVENT ---");
		
		// I want the suids of the selected nodes
		LinkedList<Long> suids = new LinkedList<Long>();
		
		// Solution I am using now
		// handles the problem with multiple fired events, by just getting all 
		// selected nodes
		// This especially deals with the problem of a second empty event.
		// --> everything is performed twice per selection which can be wrong !
		// --> do not use counters or other stuff relying on being called only once
		
		// List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		// for (CyNode n : nodes){
		//	suids.add(n.getSUID());
		// }
		
		
		// Just getting the changes.
        for (RowSetRecord record: event.getColumnRecords(CyNetwork.SELECTED)) {
            if ((Boolean)record.getValue() == true) {
                // Add the suids
            	CyRow row = record.getRow();
            	logger.info(row.toString());
            	Long suid = row.get(CyIdentifiable.SUID, Long.class);
            	
            	// There are nodes and edges in the selection !
            	// To only get the nodes do the following
            	CyNode node = network.getNode(suid);
            	if (node != null){
            		suids.add(suid);	
            	}
            }
        }
				
		// get selected nodes
		logger.info("--- SELECTION ---");
		for (Long suid: suids){
			logger.info(suid.toString());
		}
		logger.info("-----------------");
	}
	*/
	
	
	private LinkedList<Long> getSUIDsForSelectedNodes(CyNetwork network){
		List<CyNode> selectedNodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		LinkedList<Long> suids = new LinkedList<Long>();	
		for (CyNode node : selectedNodes){
			suids.add(node.getSUID());
		}
		return suids;
	}
	
	private List<Long> getSUIDs(List<String> NSBIds){ 
		One2ManyMapping<String, Long> mapping = sbmlManager.getCurrentNSB2CyNodeMapping();
		return mapping.getValues(NSBIds);
	}
	private List<String> getNSBIds(List<Long> suids){ 
		One2ManyMapping<Long, String> mapping = sbmlManager.getCurrentCyNode2NSBMapping();
		return mapping.getValues(suids);
	}

	
}
