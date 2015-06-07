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
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import org.cy3sbml.SBMLManager;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.application.swing.CytoPanelName;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.RowSetRecord;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;



public class SBMLControlPanel extends JPanel implements CytoPanelComponent, HyperlinkListener, RowsSetListener{
	private static SBMLControlPanel uniqueInstance;
	private JTree sbmlTree;
	private JEditorPane textPane;
	
	public static synchronized SBMLControlPanel getInstance(){
		if (uniqueInstance == null){
			System.out.println("cy3sbml: NavControlPanel created");
			uniqueInstance = new SBMLControlPanel();
		}
		return uniqueInstance;
	}
	

	private SBMLControlPanel(){
		/** Construct the Navigation panel for cy3sbml. */
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
		
	/////////////////// SET PANEL CONTENT ///////////////////////////////////
	
	public void setHelp(){
		try {
			URL url = new URL(SBMLControlPanel.class.getResource("/info.html").toString());
			textPane.setPage(url);
			// TODO: is this correct ?
			// this.repaint();
			// ?? not updated
		} catch (IOException e) {
			e.printStackTrace();
		}
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
			
			textPane.setText("Node selected" + node.getSUID().toString());
			// TODO: get the information for the mapped SBML node			
		}
	}
	

	public void hyperlinkUpdate(HyperlinkEvent evt) {
		URL url = evt.getURL();
		System.out.println(url);
		
		//TODO: implement the browser support
		
		/*
		import cytoscape.util.OpenBrowser;
		import cysbml.cytoscape.CytoscapeWrapper;
		if (url != null) {
			if (evt.getEventType() == HyperlinkEvent.EventType.ENTERED) {
				CytoscapeWrapper.setStatusBarMsg(url.toString());
			} else if (evt.getEventType() == HyperlinkEvent.EventType.EXITED) {
				CytoscapeWrapper.clearStatusBar();
			} else if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				OpenBrowser.openURL(url.toString());
			}
		}
		*/	
	}

}
