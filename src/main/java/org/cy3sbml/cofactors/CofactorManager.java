package org.cy3sbml.cofactors;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.mapping.NavigationTree;
import org.cy3sbml.mapping.SBML2NetworkMapper;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
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
		
		// Get mapping for network
		CofactorMapping networkMapping = network2CofactorMappings.get(network.getSUID());
		if (networkMapping == null){
			networkMapping = new CofactorMapping();
			network2CofactorMappings.put(network.getSUID(), networkMapping);
		}
			
		// degree of node
		int degree = network.getAdjacentEdgeList(cofactor, CyEdge.Type.ANY).size();
		
		// targets in the mapping
		List<Long> targets = networkMapping.get(cofactor.getSUID());
		if (targets == null){
			if (degree <= 1){
				logger.info("Node has degree 1 or 0, will not be split: " + cofactor.getSUID());
				return;
			}
			logger.info("Split cofactors");
			splitCofactorNode(networkMapping, network, cofactor);	
		} else {
			// the node was already split and should have exactly one target
			// we get the node back from the root network
			CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
			cofactor = rootNetwork.getNode(targets.get(0));
			logger.info("Merge cofactors");
			mergeCofactorNode(networkMapping, network, cofactor);
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
		
			// update sbml-type to *Clone
			String sbmlType = AttributeUtil.get(network, cofactorClone, SBML.NODETYPE_ATTR, String.class);
			System.out.println("sbml-type: " + sbmlType);
			sbmlType = sbmlType + "Clone";
			AttributeUtil.set(network, cofactorClone, SBML.NODETYPE_ATTR, sbmlType, String.class);
			
			System.out.println("Redirect edge:" + edge.getSUID().toString());
			CyNode source = edge.getSource();
			CyNode target = edge.getTarget();
			
			// Clone the edge
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
	private void mergeCofactorNode(CofactorMapping mapping, CyNetwork network, CyNode cofactor){
		System.out.println("cofactor node: " + cofactor);
		System.out.println("cofactor node: " + cofactor.getSUID());
		
		// add the original node
		((CySubNetwork) network).addNode(cofactor);
		
		// remove all clones
		List<Long> cloneSuids = mapping.get(cofactor.getSUID());
		System.out.println("Clones: " + cloneSuids);
		// set attribute for force update of style
		AttributeUtil.set(network, cofactor, SBML.NODETYPE_ATTR, 
				AttributeUtil.get(network, cofactor, SBML.NODETYPE_ATTR, String.class), 
				String.class);
		
				
		// edges from root network
		CyRootNetwork rootNetwork = ((CySubNetwork) network).getRootNetwork();
		List<CyEdge> edges = rootNetwork.getAdjacentEdgeList(cofactor, CyEdge.Type.ANY);
		for (CyEdge edge : edges){
			Long sourceSuid = edge.getSource().getSUID();
			Long targetSuid = edge.getTarget().getSUID();
			// add edge if part of subnetwork
			if ((sourceSuid == cofactor.getSUID()) && (network.getNode(targetSuid) != null)){
				((CySubNetwork) network).addEdge(edge);
			} else if ((targetSuid == cofactor.getSUID()) && (network.getNode(sourceSuid) != null)){
				((CySubNetwork) network).addEdge(edge);
			}
		}
		
		
		// remove the clone nodes
		List<CyNode> cloneNodes = new LinkedList<CyNode>();
		for (Long suid: cloneSuids){
			CyNode clone = network.getNode(suid);
			cloneNodes.add(clone);
			mapping.remove(suid);
		}
		network.removeNodes(cloneNodes);
		
		// TODO: update the mappings (forward & backwards)
		mapping.remove(cofactor.getSUID());
	}

}



