package org.cy3sbml.cofactors;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cy3sbml.SBML;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.util.AttributeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to manage cofactor nodes in Cytoscape.
 * 
 * This manages the splitting of cofactor nodes into clones and the 
 * merging of clones into the original cofactor before splitting.
 * 
 * The CofactorManager is a singleton class.
 * 
 * TODO: Necessary to update the mappings in the SBML2Networkmapper
 */
public class CofactorManager {
	private static final Logger logger = LoggerFactory.getLogger(CofactorManager.class);
	private static final String CLONE_TAG = "-clone";
	
	private static CofactorManager uniqueInstance;
	private Network2CofactorMapper mapper;

	/** Access to singleton instance. */
	public static synchronized CofactorManager getInstance(){
		if (uniqueInstance == null){
			uniqueInstance = new CofactorManager();
		}
		return uniqueInstance;
	}
	
	/** Constructor. */
	private CofactorManager(){
		logger.debug("CofactorManager created");
		mapper = new Network2CofactorMapper();
	}
	
	/**
	 *  Get the mapper.
	 * Used in serialization of the cofactor mappings. 
	 */
	public Network2CofactorMapper getNetwork2CofactorMapper(){
		return mapper;
	}
	
	public void setNetwork2CofactorMapper(Network2CofactorMapper m){
		logger.info("Network2CofactorMapper from given mapper");
		this.mapper = m;
	}
	
	/**
	 * Process given nodes.
	 */
	
	public void processNodes(CyNetwork network, List<CyNode> nodes){
		System.out.println(this.toString());
		for (CyNode node : nodes){
			processNode(network, node);
		}
		System.out.println(this.toString());
	}
	
	/*
	 * Process given cofactor node.
	 * 
	 * If a given cofactor node is not yet cloned it is cloned 
	 * into degree (cofactor) clones. I the cofactor is already a clone, 
	 * it is merged with the other clones to create the original node.
	 */
	private void processNode(CyNetwork network, CyNode node){
		Long networkSUID = network.getSUID();
		
		// get or create mapping for network
		One2ManyMapping<Long, Long> cofactor2clones = mapper.getCofactor2CloneMapping(networkSUID);
		if (cofactor2clones == null){
			cofactor2clones = mapper.newCofactor2CloneMapping(networkSUID);
		}
		One2ManyMapping<Long, Long> clone2cofactors = mapper.getClone2CofactorMapping(networkSUID);
				
		Long nodeSUID = node.getSUID();
		// Is it a clone node
		if (clone2cofactors.containsKey(nodeSUID)){
			// get the cofactor
			Long cofactorSUID = (clone2cofactors.getValues(nodeSUID)).iterator().next();
			// retrieve node from root network
			CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();	
			node = rootNetwork.getNode(cofactorSUID);
			mergeCofactorClones(network, node);
		// It is a cofactor node
		} else if (cofactor2clones.containsKey(nodeSUID)){
			logger.warn("Selected nodes should never be cofactor nodes, something went wrong.");
			
		} else {
			logger.info("Node not in cofactor mapping -> splitting:" + node.toString());
			splitCofactorNode(network, node);
		}
	}
	
	/**
	 * Split the node in network.
	 * Cofactor node of degree N is split into N single nodes.
	 */
	private void splitCofactorNode(CyNetwork network, CyNode cofactor){
		// get edges/neighbors for cofactor
		List<CyEdge> edges = network.getAdjacentEdgeList(cofactor, CyEdge.Type.ANY);
		
		for (int k=0; k<edges.size(); k++){
			// add clone node 
			CyNode clone = network.addNode();
			AttributeUtil.copyNodeAttributes(network, cofactor, clone);
			// update sbml-type to clone
			String sbmlType = AttributeUtil.get(network, clone, SBML.NODETYPE_ATTR, String.class);
			AttributeUtil.set(network, clone, SBML.NODETYPE_ATTR, sbmlType + CLONE_TAG, String.class);
			// store in mapping
			mapper.put(network.getSUID(), cofactor.getSUID(), clone.getSUID());
			
			// clone edge
			CyEdge edge = edges.get(k);
			CyNode source = edge.getSource();
			CyNode target = edge.getTarget();
			CyEdge edgeClone = null;
			if (source.getSUID() == cofactor.getSUID()){
				edgeClone = network.addEdge(clone, target, edge.isDirected());
			} else if (target.getSUID() == cofactor.getSUID()){
				edgeClone = network.addEdge(source, clone, edge.isDirected());
			}
			AttributeUtil.copyEdgeAttributes(network, edge, edgeClone);
		}
		// remove the edges from network (still in rootnetwork)
		network.removeEdges(edges);
		
		// remove the original node from network (still in rootnetwork)
		network.removeNodes(Collections.singletonList(cofactor));
	}
	
	/**
	 * Merge the node in network.
	 * N single cofactor nodes are merged into single cofactor node with
	 * degree N.
	 */
	private void mergeCofactorClones(CyNetwork network, CyNode cofactor){
		// get clone ids
		HashSet<Long> cloneSUIDs = mapper.getCofactor2CloneMapping(network.getSUID()).getValues(cofactor.getSUID());
		for (Long cloneSUID: cloneSUIDs){
			List<CyEdge> edges = network.getAdjacentEdgeList(cofactor, CyEdge.Type.ANY);
			// remove the clone edge
			network.removeEdges(edges);
			// remove clone node
			network.removeNodes(Collections.singletonList(network.getNode(cloneSUID)));
		}
		
		// add cofactor node from to subnetwork
		((CySubNetwork)network).addNode(cofactor);
		// set attribute for force update of style
		AttributeUtil.set(network, cofactor, SBML.NODETYPE_ATTR, 
				AttributeUtil.get(network, cofactor, SBML.NODETYPE_ATTR, String.class),
				String.class);
		
		// add cofactor nodes to subnetwork
		CyRootNetwork rootNetwork = ((CySubNetwork)network).getRootNetwork();
		List<CyEdge> edges = rootNetwork.getAdjacentEdgeList(cofactor, CyEdge.Type.ANY);
		for (CyEdge edge: edges){
			
			// only add edges to nodes in subnetwork			
			CyNode source = edge.getSource();
			CyNode target = edge.getTarget();
			if (network.containsNode(source) && network.containsNode(target)){
				((CySubNetwork)network).addEdge(edge);
				AttributeUtil.set(network, edge, SBML.INTERACTION_ATTR, 
						AttributeUtil.get(network, edge, SBML.INTERACTION_ATTR, String.class),
						String.class);
			}	
		}
		
		// update mapper
		mapper.remove(network.getSUID(), cofactor.getSUID());
	}

	/** String representation. */
	public String toString(){
		return mapper.toString();
	}
	
}



