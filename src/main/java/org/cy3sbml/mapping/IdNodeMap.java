package org.cy3sbml.mapping;

import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.sbml.jsbml.SBMLDocument;

import org.cy3sbml.SBML;

/**
 * Mapping between SBML & Cytoscape elements.
 * 
 * The mapping between CyNetwork elements and SBML elements uses  
 * the unique SUIDs of CyNodes and unique SBML identifiers. 
 * for a single SBML model (mapping is always restricted to a single
 * SBML <- multiple networks).
 * 
 * Depending on the kind of generated different mappings are created.
 * For instance Cytoscape Layout networks for an SBML have different 
 * mappings Qual networks.
 */
public class IdNodeMap {	
	
	/**
	 * Construct the mapping from given network and SBMLDocument.
	 * 
	 * Uses the SBML identifiers which are node attributes in the network
	 * to build the network.
	 */
	public static One2ManyMapping<String, Long> fromSBMLNetwork(SBMLDocument document, CyNetwork network, One2ManyMapping<String, Long> mapping){
		if (mapping == null){
			mapping = new One2ManyMapping<String, Long>();
		}
		// Create the mapping from all nodes in the network
		List<CyNode> nodes = network.getNodeList();
		for (CyNode node: nodes){
			// get sbml id
			CyRow attributes = network.getRow(node);
			String sbmlId = attributes.get(SBML.ATTR_ID, String.class);
			
			// put the node in the mapping
			mapping.put(sbmlId, node.getSUID());
		}
		return mapping;
	}
}
