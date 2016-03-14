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
		// selected node SUIDs
		// TODO: different inputs for the nodes
		
		LinkedList<Long> suids = new LinkedList<Long>();
		List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		
		
		
		// Do the redirection
		// TODO: this has to be stored in the Mappings for reversal
		
		
		for (CyNode cofactor : nodes){
			Long suid = cofactor.getSUID();
			
			// get the edges/neighbors for the node
			List<CyEdge> edges = network.getAdjacentEdgeList(cofactor, CyEdge.Type.ANY);

			// redirect edges
			for (int k=0; k<edges.size(); k++){
				CyEdge edge = edges.get(k);

				//
				CyNode cofactorClone = network.addNode();
				copyNodeAttributes(network, cofactor, cofactorClone);
				// update the SBML type
				String sbmlType = AttributeUtil.get(network, cofactorClone, SBML.NODETYPE_ATTR, String.class);
				System.out.println("sbml-type: " + sbmlType);
				sbmlType = sbmlType + "Clone";
				
				AttributeUtil.set(network, cofactorClone, SBML.NODETYPE_ATTR, sbmlType, String.class);
				
				
				System.out.println("Redirect edge:" + edge.getSUID().toString());
				CyNode source = edge.getSource();
				CyNode target = edge.getTarget();
				
				// Clone the edge
				// TODO: attributes
				CyEdge edgeClone = null;
				if (source.getSUID() == cofactor.getSUID()){
					edgeClone = network.addEdge(cofactorClone, target, edge.isDirected());
				} else if (target.getSUID() == cofactor.getSUID()){
					edgeClone = network.addEdge(source, cofactorClone, edge.isDirected());
				}
				copyEdgeAttributes(network, edge, edgeClone);
				
				
			}
		
			// remove the original node (make invisible)
			network.removeNodes(Collections.singletonList(cofactor));
		
			
		}
		
	}
	
	// TODO: move to AttribUtils
	
	/** 
	 * Copy node attributes.
	 * 
	 * Gets all node attributes from the DefaultNodeTable and copies from 
	 * source to target node.
	 */
	public void copyNodeAttributes(CyNetwork network, CyNode source, CyNode target){
		
		CyTable table = network.getDefaultNodeTable();
		Collection<CyColumn> columns = table.getColumns();
		for (CyColumn column : columns){
			String columnName = column.getName();
			
		 	AttributeUtil.set(network, target, 
		 			columnName,
		 			AttributeUtil.get(network, source, columnName, column.getType()), 
		 			column.getType());
		}
	}
	
	/** Copy edge attribute.
	 * 
	 * Gets all edge attributes from DefaultEdgeTable and copies from source
	 * to target edge.
	 */
	public void copyEdgeAttributes(CyNetwork network, CyEdge source, CyEdge target){
		CyTable table = network.getDefaultEdgeTable();
		Collection<CyColumn> columns = table.getColumns();
		for (CyColumn column : columns){
			String columnName = column.getName();
			
		 	AttributeUtil.set(network, target, 
		 			columnName,
		 			AttributeUtil.get(network, source, columnName, column.getType()), 
		 			column.getType());
		}
	}

}

