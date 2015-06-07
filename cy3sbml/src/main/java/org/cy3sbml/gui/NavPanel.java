package org.cy3sbml.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.net.URL;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class NavPanel extends JPanel implements CytoPanelComponent, HyperlinkListener{
	private static NavPanel uniqueInstance;
	private JTree sbmlTree;
	private JEditorPane textPane;
	
	public static synchronized NavPanel getInstance(){
		if (uniqueInstance == null){
			uniqueInstance = new NavPanel();
		}
		return uniqueInstance;
	}
	

	private NavPanel(){
		/** Construct the Navigation panel for cy3sbml. */
		setLayout(new BorderLayout(0, 0));
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.2);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		add(splitPane);
		
		// Annotation Area
		textPane = new JEditorPane();
		textPane.setEditable(false);
		textPane.addHyperlinkListener(this);
		textPane.setFont(new Font("Dialog", Font.PLAIN, 11));
		textPane.setContentType("text/html");
		setHelpInNavigationPanel();

		JScrollPane annotationScrollPane = new JScrollPane();
		annotationScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		annotationScrollPane.setViewportView(textPane);
		splitPane.setRightComponent(annotationScrollPane);
		
		JTabbedPane navTabbedPane = new JTabbedPane(JTabbedPane.TOP);
		splitPane.setLeftComponent(navTabbedPane);
		
		// Navigation Tree
		sbmlTree = new JTree();
		sbmlTree.setVisibleRowCount(12);
		sbmlTree.setEditable(false);
		
		// TODO: listen to tree events
		// sbmlTree.addTreeSelectionListener(this);
		
		sbmlTree.setFont(new Font("Dialog", Font.PLAIN, 10));
		
		// TODO: handle the navigation tree
		// setNavigationTreeInJTree();
		
		JScrollPane treeScrollPane = new JScrollPane();
		navTabbedPane.addTab("SBMLTree", null, treeScrollPane, null);
		treeScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		treeScrollPane.setViewportView(sbmlTree);		
	}
	
	@Override
	public CytoPanelName getCytoPanelName() {
		return CytoPanelName.WEST;
	}
	
	
	/////////////////// SET PANEL CONTENT ///////////////////////////////////
	
	private void setHelpInNavigationPanel(){
		try {
			URL url = new URL(NavPanel.class.getResource("help/info.html").toString());
			textPane.setPage(url);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	/////////////////// HANDLE EVENTS ///////////////////////////////////
	
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
