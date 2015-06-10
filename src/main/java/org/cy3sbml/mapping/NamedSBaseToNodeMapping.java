package org.cy3sbml.mapping;

import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;


/** The mapping changed completely. 
 * Things are now managed via suids.
 * @author mkoenig
 *
 */
public class NamedSBaseToNodeMapping extends OneToManyMapping{	
	
	
	
	/* One to one Mapping between network and tree, so
	 * network can be used to create the mapping. 
	 */
	public NamedSBaseToNodeMapping(CyNetwork network){
		super();		
		List<CyNode> nodes = network.getNodeList();
		for (CyNode node: nodes){
			// TODO: better long dictionary
			String value = node.getSUID().toString();
			String key = value;
			put(key, value);
		}
	}
	
	/* In the Layout case the LayoutNodes are mapped to species and reaction nodes.
	 * Mapping is one to many !, meaning that multiple 
	 */
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
}
