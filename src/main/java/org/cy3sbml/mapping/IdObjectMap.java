package org.cy3sbml.mapping;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


import org.cy3sbml.util.SBMLUtil;
import org.sbml.jsbml.*;

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
 *
 * The information in this class must be synchronized with the SBMLReaderTask
 * and the
 *
 * Probably better to switch to the metaid of the objects which
 * is unique for the objects.
 * This requires to create meta ids for all nodes which are registered.
 */
public class IdObjectMap {
	private static final Logger logger = LoggerFactory.getLogger(IdObjectMap.class);
	private Map<String, SBase> objectMap;
	
	public IdObjectMap(){
		objectMap = new HashMap<>();
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
            ////////////////////////////////////////////////////////////////////////////
            // SBML CORE
            ////////////////////////////////////////////////////////////////////////////

            Model model;
            if (document.isSetModel()) {
                model = document.getModel();
            } else {
                // nothing to add
                return;
            }

            // FunctionDefinitions
            addListOf(model.getListOfFunctionDefinitions());
            // UnitDefinitions
            addListOf(model.getListOfUnitDefinitions());
            // Compartments
            addListOf(model.getListOfCompartments());
            // Species
            addListOf(model.getListOfSpecies());
			// Parameters
			addListOf(model.getListOfParameters());

            // InitialAssignments (no ids)
            for (InitialAssignment assignment: model.getListOfInitialAssignments()){
                String variable = assignment.getVariable();
                String id = SBMLUtil.initialAssignmentId(variable);
                objectMap.put(id, assignment);
            }

            // Rules (no ids)
            for (Rule rule: model.getListOfRules()){
                String variable = SBMLUtil.getVariableFromRule(rule);
                String id = SBMLUtil.ruleId(variable);
                objectMap.put(id, rule);
            }

            // Constraints
            // TODO: implement
            // addListOf(model.getListOfConstraints());

            // Reactions
            addListOf(model.getListOfReactions());
			
			// LocalParameters & KineticLaws
			for (Reaction r : model.getListOfReactions()){	
				if (r.isSetKineticLaw()){
					KineticLaw law = r.getKineticLaw();
					
					// Create law id (analogue to reader)
					String reactionId = r.getId();
					String lawId = SBMLUtil.kineticLawId(reactionId);
					objectMap.put(lawId, law);
				
					// this were made unique during reading of SBML
					for (LocalParameter lp: law.getListOfLocalParameters()){
						objectMap.put(lp.getId(), lp);	
					}
				}
			}

			// Events
            addListOf(model.getListOfEvents());

            ////////////////////////////////////////////////////////////////////////////
            // SBML QUAL
            ////////////////////////////////////////////////////////////////////////////

	        QualModelPlugin qualModel = (QualModelPlugin) model.getExtension(QualConstants.namespaceURI);
			if (qualModel != null){
				// QualitativeSpecies
				addListOf(qualModel.getListOfQualitativeSpecies());
				// Transitions
				addListOf(qualModel.getListOfTransitions());
			}

            ////////////////////////////////////////////////////////////////////////////
            // SBML FBC
            ////////////////////////////////////////////////////////////////////////////

            FBCModelPlugin fbcModel = (FBCModelPlugin) model.getExtension(FBCConstants.namespaceURI);
			if (fbcModel != null){
				// GeneProducts
				addListOf(fbcModel.getListOfGeneProducts());
			}

            ////////////////////////////////////////////////////////////////////////////
            // SBML COMP
            ////////////////////////////////////////////////////////////////////////////

            CompModelPlugin compModel = (CompModelPlugin) model.getExtension(CompConstants.namespaceURI);
			if (compModel != null){
				// Ports
				addListOf(compModel.getListOfPorts());
			}

            ////////////////////////////////////////////////////////////////////////////
            // SBML GROUPS
            ////////////////////////////////////////////////////////////////////////////

            // TODO: implement

            ////////////////////////////////////////////////////////////////////////////
            // SBML LAYOUT
            ////////////////////////////////////////////////////////////////////////////

            // TODO: implement

        } catch (Throwable t) {
			logger.error("IdObjectMap could not be created", t);
			t.printStackTrace();
		}
	}
	
	private void addListOf(ListOf<? extends NamedSBase> list){
		for (NamedSBase obj: list ){
			objectMap.put(obj.getId(), obj);
		}
	}
	
	/**
	 * SBMLObject lookup via id key.
	 */
	public SBase getObject(String key){
		SBase sbase = null;
		if (objectMap.containsKey(key)){
			return objectMap.get(key);
		}
		return sbase;
	}

	/** Get SBase Objects in the map. */
	public Collection<SBase> getObjects(){
		return objectMap.values();
	}


}
