package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.ImageIcon;

import org.cy3sbml.gui.GUIConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.cofactors.CofactorManager;

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

	/** Constructor. */
	public CofactorNodesAction(ServiceAdapter adapter){
		super("CofactorNodesAction");
		this.adapter = adapter;
		ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.IMAGE_COFACTOR));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, "Cofactor Nodes");
		setToolbarGravity((float) 200.0);
	}
		
	public boolean isInToolBar() {
		return true;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		logger.info("actionPerformed()"); 
		
		// Get the current network via the service adapter
		CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
		CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
		if (network == null || view == null){
			return;
		}
		
		// Selected nodes are inputs to the cofactor handling
		// TODO: implement different inputs (from SBML, from List, cofactor files)
		//  (this has to be processed in the network generation)
		List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		
		CofactorManager cofactorManager = CofactorManager.getInstance();
		cofactorManager.processNodes(network, nodes);
		
		view.updateView();
	}

}

