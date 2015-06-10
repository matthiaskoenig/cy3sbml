package org.cy3sbml.mapping;

import java.util.List;

import org.cy3sbml.SBML;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.sbml.jsbml.SBMLDocument;



/** The mapping changed completely. 
 * Things are now managed via suids.
 */



public class NamedSBaseToNodeMapping extends OneToManyMapping{	
	
	
	
	/* One to one Mapping between network and tree, so
	 * network can be used to create the mapping. 
	 */
	public NamedSBaseToNodeMapping(SBMLDocument document, CyNetwork network){
		super();		
		List<CyNode> nodes = network.getNodeList();
		for (CyNode node: nodes){
			Long suid = node.getSUID();
			// get attributes from node
			CyRow attributes = network.getRow(node);
			String sbml_id = attributes.get(SBML.SBML_ID_ATTR, String.class);
			
			// TODO: get node attributes
			put(sbml_id, suid);
		}
	}
	
	
	/* In the Layout case the LayoutNodes are mapped to species and reaction nodes.
	 * Mapping is one to many !, meaning that multiple 
	 */
	/*
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
