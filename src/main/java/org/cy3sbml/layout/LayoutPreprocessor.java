package org.cy3sbml.layout;

import java.util.LinkedList;
import java.util.List;

import org.cy3sbml.mapping.One2ManyMapping;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.ModifierSpeciesReference;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SpeciesReference;
import org.sbml.jsbml.ext.layout.Layout;
import org.sbml.jsbml.ext.layout.ReactionGlyph;
import org.sbml.jsbml.ext.layout.SpeciesGlyph;
import org.sbml.jsbml.ext.layout.SpeciesReferenceGlyph;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.qual.Transition;

/**
 * Layout manipulating class.
 * The layouts are prepared to use for visualization. Part of this is setting the
 * glyph ids.
 */
public class LayoutPreprocessor{
	public static final String ALL = "all"; 
	private static int speciesIdCounter;
	private static int reactionIdCounter;
	
	private Layout layout;
	private Model model;
	private QualModelPlugin qualModel;
	private One2ManyMapping<String, String> species2speciesGlyphs;
	private One2ManyMapping<String, String> reaction2reactionGlyphs;
	
	public LayoutPreprocessor(Model sbmlModel, QualModelPlugin qualModel, Layout layout){
		this.model = sbmlModel;
		this.qualModel = qualModel;
		this.layout = layout;
		
		species2speciesGlyphs = new One2ManyMapping<String, String>();
		reaction2reactionGlyphs = new One2ManyMapping<String, String>();
		
		// Create the missing glyph ids
		speciesIdCounter = 0;
		reactionIdCounter = 0;
		createMissingSpeciesGlyphIds();
		createMissingReactionGlyphIds();
		
		// Create the mapping maps between model and layout
		if (layout.isSetListOfReactionGlyphs()){
			createReactionGlyphMapping(layout.getListOfReactionGlyphs());
		}
		if (layout.isSetListOfSpeciesGlyphs()){
			createSpeciesGlyphMapping(layout.getListOfSpeciesGlyphs());
		}
		//
		generateAllEdges();
	}
	
	public Layout getProcessedLayout(){
		return layout;
	}
	
	public boolean hasModel(){
		return (model != null);
	}
	public boolean hasQualitativeModel(){
		return (qualModel != null);
	}
	
	private void createMissingSpeciesGlyphIds(){
		if (layout.isSetListOfSpeciesGlyphs()){
			for (SpeciesGlyph glyph : layout.getListOfSpeciesGlyphs()){
				if (!glyph.isSetId()){
					speciesIdCounter ++;
					String id = String.format("speciesGlyph_%s", speciesIdCounter);
					glyph.setId(id);
				}
			}
		}
	}
	
	private void createMissingReactionGlyphIds(){
		if (layout.isSetListOfReactionGlyphs()){
			for (ReactionGlyph glyph : layout.getListOfReactionGlyphs()){
				if (!glyph.isSetId()){
					reactionIdCounter ++;
					String id = String.format("reactionGlyph_%s", reactionIdCounter);
					glyph.setId(id);
				}
			}
		}
	}
	
	public void createSpeciesGlyphMapping(ListOf<SpeciesGlyph> listOfGlyphs){
		species2speciesGlyphs = new One2ManyMapping<String, String>();
		for (SpeciesGlyph glyph : listOfGlyphs){
			if (glyph.isSetSpecies()){
				species2speciesGlyphs.put(glyph.getSpecies(), glyph.getId());
			}
		}
	}
	
	public void createReactionGlyphMapping(ListOf<ReactionGlyph> listOfGlyphs){
		reaction2reactionGlyphs = new One2ManyMapping<String, String>();
		for (ReactionGlyph glyph : listOfGlyphs){
			if (glyph.isSetReaction()){
				reaction2reactionGlyphs.put(glyph.getReaction(), glyph.getId());
			}
		}
	}
	

	public static boolean hasEdgeInformation(Layout layout){
		boolean hasInfo = false;
		if (layout.isSetListOfReactionGlyphs()){
			for (ReactionGlyph glyph: layout.getListOfReactionGlyphs()){
				if (hasEdgeInformation(glyph)){
					hasInfo = true;
					break;
				}
			}
		}
		return hasInfo;
	}
	
	public static boolean hasEdgeInformation(ReactionGlyph glyph){
		ListOf<SpeciesReferenceGlyph> speciesReferenceGlyphs = glyph.getListOfSpeciesReferenceGlyphs();
		if (speciesReferenceGlyphs == null | speciesReferenceGlyphs.size() == 0){
			return false;
		}
		return true;
	}
	
	private void generateAllEdges(){
		if (layout.isSetListOfReactionGlyphs()){
			for (ReactionGlyph rGlyph : layout.getListOfReactionGlyphs()){
				if (!hasEdgeInformation(rGlyph)){
					generateEdgesForReactionGlyph(rGlyph);
				}
			}
		}
	}
	
	private void generateEdgesForReactionGlyph(ReactionGlyph rGlyph){
		Reaction reaction = getReactionForReactionGlyph(rGlyph);		
		if (reaction != null){
			List<String[]> connectedSpecies = getConnectedSpecies(reaction);
			for (String[] data: connectedSpecies){
				String speciesId = data[0];
				String role = data[1];
				generateAllEdges(rGlyph, speciesId, role);
			}
		}
		Transition transition = getTransitionForReactionGlyph(rGlyph);
		if (transition != null){
			List<String[]> connectedQSpecies = getConnectedQualitativeSpecies(transition);
			for (String[] data : connectedQSpecies){
				String qSpeciesId = data[0];
				String role = data[1];
				generateAllEdges(rGlyph, qSpeciesId, role);
			}
		}
	}
	
	private void generateAllEdges(ReactionGlyph rGlyph, String speciesId, String role){
		ListOf<SpeciesReferenceGlyph> speciesReferenceGlyphList = rGlyph.getListOfSpeciesReferenceGlyphs();
		if (species2speciesGlyphs.containsKey(speciesId)){
			for (String sGlyphId : species2speciesGlyphs.getValues(speciesId)){
				SpeciesReferenceGlyph speciesReferenceGlyph = new SpeciesReferenceGlyph(sGlyphId);
				speciesReferenceGlyph.setSpeciesGlyph(sGlyphId);
				speciesReferenceGlyph.setName(role);
				speciesReferenceGlyphList.add(speciesReferenceGlyph);
			}
		}
	}
			
	private List<String[]> getConnectedSpecies(Reaction reaction){
		List<String[]> connectedSpecies = new LinkedList<String[]>();
		if (reaction.isSetListOfReactants()){
			for (SpeciesReference speciesReference : reaction.getListOfReactants()){
				String[] data = new String[2];
				data[0] = speciesReference.getSpeciesInstance().getId();
				//data[1] = CySBMLGraphReader.EDGETYPE_REACTION_REACTANT;
				connectedSpecies.add(data);
			}
		}
		if (reaction.isSetListOfProducts()){
			for (SpeciesReference speciesReference : reaction.getListOfProducts()){
				String[] data = new String[2];
				data[0] = speciesReference.getSpeciesInstance().getId();
				//data[1] = CySBMLGraphReader.EDGETYPE_REACTION_PRODUCT;
				connectedSpecies.add(data);
			}
		}
		if (reaction.isSetListOfModifiers()){
			for (ModifierSpeciesReference speciesReference : reaction.getListOfModifiers()){
				String[] data = new String[2];
				data[0] = speciesReference.getSpeciesInstance().getId();
				//data[1] = CySBMLGraphReader.EDGETYPE_REACTION_MODIFIER;
				connectedSpecies.add(data);
			}
		}
		return connectedSpecies;
	}
	
	private List<String[]> getConnectedQualitativeSpecies(Transition transition){
		List<String[]> connectedQSpecies = new LinkedList<String[]>();
		if (transition.isSetListOfInputs()){
			for (Input input : transition.getListOfInputs()){
				String[] data = new String[2];
				data[0] = input.getQualitativeSpecies();
				//data[1] = CySBMLGraphReader.EDGETYPE_TRANSITION_INPUT;
				connectedQSpecies.add(data);
			}
		}
		if (transition.isSetListOfOutputs()){
			for (Output output : transition.getListOfOutputs()){
				String[] data = new String[2];
				data[0] = output.getQualitativeSpecies();
				//data[1] = CySBMLGraphReader.EDGETYPE_TRANSITION_INPUT;
				connectedQSpecies.add(data);
			}
		}
		return connectedQSpecies;
	}
	
	public Transition getTransitionForReactionGlyph(ReactionGlyph rGlyph) {
		if (qualModel == null || !rGlyph.isSetReaction()){
			return null;
		}
		String reactionId = rGlyph.getReaction();
		Transition transition = null;
		if (qualModel.isSetListOfTransitions()) {
			for (Transition t : qualModel.getListOfTransitions()) {
				if (t.getId().equals(reactionId)) {
					transition = t; 
					break;
				}
			}
		}
		return transition;
	}
	
	public Reaction getReactionForReactionGlyph(ReactionGlyph rGlyph) {
		if (!rGlyph.isSetReaction()){
			return null;
		}
		String reactionId = rGlyph.getReaction();
		Reaction reaction = null;
		if (model.isSetListOfReactions()) {
			for (Reaction r : model.getListOfReactions()) {
				if (r.getId().equals(reactionId)) {
					reaction = r; 
					break;
				}
			}
		}
		return reaction;
	}
	
}
