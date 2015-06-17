package org.cy3sbml.mapping;

import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.sbml.jsbml.SBMLDocument;

import org.cy3sbml.SBML;

/**
 * Mapping between the CyNetwork elements and the SBML elements are 
 * based on the unique SUIDs of CyNodes and unique SBML identifiers 
 * for a single SBML model (mapping is always restricted to a single
 * SBML <- multiple networks).
 * Depending on the generated network different mappings are created.
 * For instance the Layout networks have to be managed in a different manner than
 * Qual networks.
 */
public class NamedSBase2CyNodeMapping extends One2ManyMapping<String,Long>{	
	
	/**
	 * Create mapping from SBML network.
	 */
	public static NamedSBase2CyNodeMapping fromSBMLNetwork(SBMLDocument document, CyNetwork network){
		NamedSBase2CyNodeMapping mapping = new NamedSBase2CyNodeMapping();
		
		List<CyNode> nodes = network.getNodeList();
		for (CyNode node: nodes){
			Long suid = node.getSUID();
			
			// get attribute from node
			CyRow attributes = network.getRow(node);
			String sbml_id = attributes.get(SBML.ATTR_ID, String.class);
			
			// normally one should check if the NamedSbases are really in the SBML,
			// but the network was created from the SBML, so we skip this part here
			// for performance
			
			mapping.put(sbml_id, suid);
		}
		return mapping;
		
	}

	 // TODO: generate the layout networks
	 // Find a general solution for SBML based information. A master graph from which
	 // subnetworks are generated is probably the best solution and easily extendible to 
	 // other SBML extensions.
	
	 /* In the Layout case the LayoutNodes are mapped to species and reaction nodes.
	  * Mapping is one to many !, meaning that multiple 

	public NamedSBaseToNodeMapping(Layout layout){
		super();	
		for (ReactionGlyph glyph: layout.getListOfReactionGlyphs()){
			String value = glyph.getId();
			if (glyph.isSetReaction()){
				String key = glyph.getReaction();
				put(key, value);
			}
		}
		for (SpeciesGlyph glyph: layout.getListOfSpeciesGlyphs()){
			String value = glyph.getId();
			if (glyph.isSetSpecies()){
				String key = glyph.getSpecies();
				put(key, value);
			}
		}
	}
	*/
}
