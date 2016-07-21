package org.cy3sbml.mapping;


import java.util.HashMap;
import java.util.Map;



import org.sbml.jsbml.SBase;

import org.sbml.jsbml.KineticLaw;
import org.sbml.jsbml.ListOf;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;

import org.sbml.jsbml.ext.qual.QualConstants;
import org.sbml.jsbml.ext.qual.QualModelPlugin;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class manages the SBMLObject lookup from given key.
 * FIXME: most of the lookups should be done via the SBMLDocument.
 * only in the cases where ids are generated these have to be looked
 * up separately.
 *
 * This will guarantee that new SBases are available without the need
 * to register them first (this is a major source of bugs).
 *
 */
public class IdObjectMap {
	private static final Logger logger = LoggerFactory.getLogger(IdObjectMap.class);
	private Map<String, SBase> objectMap;
	
	public IdObjectMap(){
		objectMap = new HashMap<String, SBase>(); 
	}
	
	/** Register SBMLObjects for id lookup from SBMLDocument. */ 
	public IdObjectMap(SBMLDocument document){
		this();
		logger.debug("Create IdObjectMap for SBMLDocument");
		if (document == null){
			logger.debug("No SBMLDocument");
			return;
		}
	
		try{
			Model model = document.getModel();
			
			// Parameters
			addListOf(model.getListOfParameters());
			
			// LocalParameters & KineticLaws
			for (Reaction r : model.getListOfReactions()){	
				if (r.isSetKineticLaw()){
					KineticLaw law = r.getKineticLaw();
					
					// Create law id (analogue to reader)
					String reactionId = r.getId();
					String lawId = String.format("%s_law", reactionId);
					objectMap.put(lawId, law);
				
					// have been made unique during reading of SBML
					for (LocalParameter lp: law.getListOfLocalParameters()){
						objectMap.put(lp.getId(), lp);	
					}
				}
			}
			
			// Compartments
			addListOf(model.getListOfCompartments());
			// Species
			addListOf(model.getListOfSpecies());
			// Reactions
			addListOf(model.getListOfReactions());
			// FunctionDefinitions
			addListOf(model.getListOfFunctionDefinitions());
			
	        QualModelPlugin qualModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
			if (qualModel != null){
				// QualitativeSpecies
				addListOf(qualModel.getListOfQualitativeSpecies());
				// Transitions
				addListOf(qualModel.getListOfTransitions());
			}
			
			FBCModelPlugin fbcModel = (FBCModelPlugin) model.getExtension(FBCConstants.namespaceURI);
			if (fbcModel != null){
				// GeneProducts
				addListOf(fbcModel.getListOfGeneProducts());
			}
			
			CompModelPlugin compModel = (CompModelPlugin) model.getExtension(CompConstants.namespaceURI);
			if (compModel != null){
				// Ports
				addListOf(compModel.getListOfPorts());
			}
			
		} catch (Throwable t) {
			logger.error("IdObjectMap could not be created");
			t.printStackTrace();
		}
	}
	
	private void addListOf(ListOf<? extends NamedSBase> list){
		for (NamedSBase obj: list ){
			objectMap.put(obj.getId(), obj);
		}
	}
	
	/** SBMLObject lookup via id key. */
	public SBase getObject(String key){
		SBase sbase = null;
		if (objectMap.containsKey(key)){
			return objectMap.get(key);
		}
		return sbase;
	}	
}
