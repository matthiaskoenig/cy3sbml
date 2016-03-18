package org.cy3sbml.cofactors;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to manage the cofactor nodes in a given network.
 * 
 * Has to manage for a given network the cofactor nodes.
 * 
 * TODO: update the SBML mapping with the cofactor node identifiers
 *
 */
public class CofactorManager {
	private static final Logger logger = LoggerFactory.getLogger(CofactorManager.class);
	private static final String CLONE_TAG = "-clone";
	
	
	private static CofactorManager uniqueInstance;
	// store of CofactorMappings for given network
	private Map<Long, CofactorMapping> network2CofactorMappings;

	
	public static synchronized CofactorManager getInstance(){
		if (uniqueInstance == null){
			uniqueInstance = new CofactorManager();
		}
		return uniqueInstance;
	}
	
	private CofactorManager(){
		logger.info("CofactorManager created");
		network2CofactorMappings = new HashMap<Long, CofactorMapping>();
	}
	
	
	/*
	 * Function to handle the cofactor nodes.
	 * 
	 * If the cofactor node is not cloned it is cloned. 
	 * On the other hand, if the cofactor is already cloned, it is reunified.
	 * Checks in the Mapping if cloned or not cloned.
	 */
	public void handleCofactorNode(CyNetwork network, CyNode cofactor){
		Long suid = network.getSUID();
		
		// Get mapping for network
		CofactorMapping cofactorMapping = network2CofactorMappings.get(suid);
		if (cofactorMapping == null){
			cofactorMapping = new CofactorMapping();
			network2CofactorMappings.put(suid, cofactorMapping);
		}
				
		// degree of node in network
		int degree = network.getAdjacentEdgeList(cofactor, CyEdge.Type.ANY).size();
		
		
		// In the cofactorMapping for every split cofactor the targets are stored
		List<Long> clones = cofactorMapping.get(cofactor.getSUID());
		if (clones == null){
			logger.info("Split cofactors");
			splitCofactorNode(cofactorMapping, network, cofactor);	
		} else {
			logger.info("Merge cofactors");
			mergeCofactorClones(network, clones);
			cofactorMapping.remove(cofactor.getSUID());
		}
	}
	
	/**
	 * Cofactor node of degree N is split into N single nodes.
	 */
	private void splitCofactorNode(CofactorMapping mapping, CyNetwork network, CyNode cofactor){
		
		// get edges/neighbors for cofactor
		List<CyEdge> edges = network.getAdjacentEdgeList(cofactor, CyEdge.Type.ANY);
		List<Long> targets = new LinkedList<Long>();
		
		// redirect edges
		for (int k=0; k<edges.size(); k++){
			CyEdge edge = edges.get(k);

			// add clone node 
			CyNode cofactorClone = network.addNode();
			AttributeUtil.copyNodeAttributes(network, cofactor, cofactorClone);
		
			// add cofactor -> clone mapping
			targets.add(cofactorClone.getSUID());
			// add clone -> cofactor mapping
			List<Long> origin = new LinkedList<Long>();
			origin.add(cofactor.getSUID());
			mapping.put(cofactorClone.getSUID(), origin);
		
			// update sbml-type to have clone tag
			String sbmlType = AttributeUtil.get(network, cofactorClone, SBML.NODETYPE_ATTR, String.class);
			AttributeUtil.set(network, cofactorClone, SBML.NODETYPE_ATTR, sbmlType + CLONE_TAG, String.class);
			
			// Clone the edge
			CyNode source = edge.getSource();
			CyNode target = edge.getTarget();
			CyEdge edgeClone = null;
			if (source.getSUID() == cofactor.getSUID()){
				edgeClone = network.addEdge(cofactorClone, target, edge.isDirected());
			} else if (target.getSUID() == cofactor.getSUID()){
				edgeClone = network.addEdge(source, cofactorClone, edge.isDirected());
			}
			AttributeUtil.copyEdgeAttributes(network, edge, edgeClone);
		}
		// Add full cofactor -> clones mapping
		mapping.put(cofactor.getSUID(), targets);
		
		// remove the original node (make invisible)
		network.removeNodes(Collections.singletonList(cofactor));
	}
	
	/**
	 * N single cofactor nodes are merged into single cofactor node with
	 * degree N.
	 */
	private void mergeCofactorClones(CyNetwork network, List<Long> clones){
		
		// we get the first node from the targets (on this node the other 
		//     targets are merged)
		Long cofactorSUID = clones.get(0);
		CyNode cofactor = network.getNode(cofactorSUID);
		
		// set attribute for force update of style
		String cloneType = AttributeUtil.get(network, cofactor, SBML.NODETYPE_ATTR, String.class);
		String sbmlType = cloneType.substring(0, cloneType.length()-CLONE_TAG.length());
		System.out.println("sbmlType: " + sbmlType);
		AttributeUtil.set(network, cofactor, SBML.NODETYPE_ATTR, sbmlType, String.class);
		
		// redirect remaining clone edges
		for (int k=1; k<clones.size(); k++){
			// clone
			Long cloneSUID = clones.get(k);
			CyNode clone = network.getNode(cloneSUID);
			// edge connecting clone
			List<CyEdge> edges = network.getAdjacentEdgeList(cofactor, CyEdge.Type.ANY);
			if (edges.size() != 1){
				logger.warn("Target has more than one edge.");
			}
			CyEdge edge = edges.get(0);
			CyNode source = edge.getSource();
			CyNode target = edge.getTarget();
			
			// redirect edge (add new, remove old)
			CyEdge newEdge = null;
			if (source.getSUID() == clone.getSUID()){
				 newEdge = network.addEdge(cofactor, target, edge.isDirected());
			} else if (target.getSUID() == clone.getSUID()){
				newEdge = network.addEdge(source, cofactor, edge.isDirected());
			} else {
				logger.error("Problems with cofactors.");
			}
			AttributeUtil.copyEdgeAttributes(network, edge, newEdge);
			network.removeEdges(Collections.singletonList(edge));
			
			// remove isolated clone node
			network.removeNodes(Collections.singletonList(clone));
		}
	}

}



