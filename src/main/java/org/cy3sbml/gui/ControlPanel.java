package org.cy3sbml.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;

import javax.swing.Icon;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.cy3sbml.SBMLManager;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.util.swing.OpenBrowser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Control Panel for cy3sbml.
 */
public class ControlPanel extends JPanel implements CytoPanelComponent, HyperlinkListener, RowsSetListener{
	private static final Logger logger = LoggerFactory.getLogger(ControlPanel.class);
	private static final long serialVersionUID = 1L;

	private static ControlPanel uniqueInstance;
	
	private OpenBrowser openBrowser;
	private JTree sbmlTree;
	private JEditorPane textPane;

	private boolean active = false;	

	// TODO: better to refactor the information panel things -> put in separate class
	private long lastInformationThreadId = -1;
	
	
	public static synchronized ControlPanel getInstance(OpenBrowser openBrowser){
		if (uniqueInstance == null){
			logger.info("ControlPanel created");
			uniqueInstance = new ControlPanel(openBrowser);
		}
		return uniqueInstance;
	}
	

	private ControlPanel(OpenBrowser openBrowser){
		/** Construct the Navigation panel for cy3sbml. */
		this.openBrowser = openBrowser;
		
		setLayout(new BorderLayout(0, 0));
		
		// --- Annotation Area ------------------
		textPane = new JEditorPane();
		textPane.setEditable(false);
		textPane.addHyperlinkListener(this);
		textPane.setFont(new Font("Dialog", Font.PLAIN, 11));
		textPane.setContentType("text/html");
		this.setHelp();
		
		JScrollPane annotationScrollPane = new JScrollPane();
		annotationScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		annotationScrollPane.setViewportView(textPane);

		this.add(textPane);
		/*
		// --- Navigation Tree  ------------------
		sbmlTree = new JTree();
		sbmlTree.setVisibleRowCount(12);
		sbmlTree.setEditable(false);
		
		// TODO: listen to tree events
		// sbmlTree.addTreeSelectionListener(this);
		
		sbmlTree.setFont(new Font("Dialog", Font.PLAIN, 10));
		
		// TODO: handle the navigation tree
		// setNavigationTreeInJTree();
		
		JScrollPane treeScrollPane = new JScrollPane();
		treeScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		treeScrollPane.setViewportView(sbmlTree);	
		
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.2);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		
		// set coponents
		splitPane.setRightComponent(annotationScrollPane);
		splitPane.setLeftComponent(treeScrollPane);
		
		this.add(splitPane);
		*/
		
		// set the size
		Dimension size = this.getSize();
		this.setSize(400, size.height);
	}
	
	/////////////////// CYTO PANEL COMPONENT ///////////////////////////////////
	
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
		// TODO add the icon
		return null;
	}

	@Override
	public String getTitle() {
		return "cy3sbml";
	}
	
	// TODO: refactor
	/*
    public void activate(){
		CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
		int index = cytoPanel.indexOfComponent(CySBML.NAME);
		if (index == -1){
			cytoPanel.add(CySBML.NAME, this);
			cytoPanel.setState(CytoPanelState.DOCK);
		}
		selectNavigationPanel();
		active = true;
	}
	
	public void deactivate(){
		CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
		int index = cytoPanel.indexOfComponent(CySBML.NAME);
		if (index != -1){
			cytoPanel.remove(index);
		}
		// Test if still other Components, otherwise hide
		if (cytoPanel.getCytoPanelComponentCount() == 0){
			cytoPanel.setState(CytoPanelState.HIDE);
		}
		active = false;
	}

	public boolean isActive(){
		return active;
	}
	
	public static void selectNavigationPanel(){
		CytoPanel cytoPanel = Cytoscape.getDesktop().getCytoPanel(SwingConstants.EAST);
		cytoPanel.setSelectedIndex(cytoPanel.indexOfComponent(CySBML.NAME));
	}
	*/
		
	/////////////////// TEXT CONTENT ///////////////////////////////////
	public JEditorPane getTextPane(){
		return textPane;
	}
		
	public void setText(String text){
		textPane.setText(text);
	}
		
	public void setHelp(){
		try {
			logger.info("set help in control panel");
			URL url = new URL(ControlPanel.class.getResource("/info.html").toString());
			logger.info(url.toString());
			textPane.setPage(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public long getLastInfoThreadId(){
		return lastInformationThreadId;
	}
	
	/////////////////// HANDLE EVENTS ///////////////////////////////////
	
	// http://chianti.ucsd.edu/cytoscape-3.2.1/API/org/cytoscape/model/package-summary.html
	public void handleEvent(RowsSetEvent e) {
		Collection<RowSetRecord> rowsSet = e.getColumnRecords(CyNetwork.SELECTED);
		for (RowSetRecord record: rowsSet) {
			CyRow row = record.getRow(); // Get the row that was set
			boolean selected = ((Boolean)record.getValue()).booleanValue();  // What it was set to
			// Take appropriate action.  For example, might want to get
			// the node or edge that was selected (or unselected)
			CyNetwork network = SBMLManager.getInstance(null, null).getCyApplicationManager().getCurrentNetwork();
			CyNode node = network.getNode(row.get(CyIdentifiable.SUID, Long.class));
			
			if (selected){
				textPane.setText("Node selection event: (" + node.getSUID().toString() + ")");
			} else {
				textPane.setText("Unselected event");
			}
			// TODO: get the information for the mapped SBML node			
		}
	}
	

	public void hyperlinkUpdate(HyperlinkEvent evt) {
		/* Open link in browser. */
		URL url = evt.getURL();
		if (url != null) {
			if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
				
			} else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
				
			} else if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				openBrowser.openURL(url.toString());
			}
		}	
	}

}
