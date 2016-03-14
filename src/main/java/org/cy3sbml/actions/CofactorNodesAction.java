package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.swing.ImageIcon;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cy3sbml.SBML;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.util.AttributeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Action to handle cofactor nodes.
 * 
 * Handle clones of network nodes for better visualization.
 * This allows to clone a selected set of nodes.
 */
public class CofactorNodesAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(CofactorNodesAction.class);
	private static final long serialVersionUID = 1L;
	private ServiceAdapter adapter;

	public CofactorNodesAction(ServiceAdapter adapter){
		super("CofactorNodesAction");
		this.adapter = adapter;
		ImageIcon icon = new ImageIcon(getClass().getResource("/images/cofactor.png"));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, "Cofactor Nodes");
		setToolbarGravity((float) 95.0);
	}
		
	public boolean isInToolBar() {
		return true;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		logger.info("actionPerformed()"); 
		
		System.out.println("****************************************");
		System.out.println("Cofactor nodes");
		System.out.println("****************************************");
	
		// Get set of cofactor nodes
		// Testcase: selected node in network
		
		// Get the current network via the service adapter
		CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
		CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
		if (network == null || view == null){
			return;
		}
		
		// Selected nodes are inputs to the cofactor handling
		// TODO: implement different inputs (from SBML, from List)
		List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		
		CofactorManager cofactorManager = CofactorManager.getInstance();	
		for (CyNode cofactor : nodes){
			cofactorManager.handleCofactorNode(network, cofactor);
		}
		
		view.updateView();
		/*
		Collection<CyNetworkView> views = adapter.cyNetworkViewManager.getNetworkViews(network);
		for (CyNetworkView view: views){
			view.updateView();
		}
		*/
		
	}

}

