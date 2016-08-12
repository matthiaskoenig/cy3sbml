package org.cy3sbml.mapping;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


import org.cy3sbml.SBML;
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
 *
 * Objects are stored under the created CyIds.
 *
 * FIXME: This should be done when the nodes are created, i.e.
 * with every created node the respective SBase should be saved.
 *
 */
public class CyIdSBaseMap {
	private static final Logger logger = LoggerFactory.getLogger(CyIdSBaseMap.class);
	private Map<String, SBase> objectMap;

    /** Constructor. */
	public CyIdSBaseMap(){
	    objectMap = new HashMap<>();
	}
	
	/**
     * Constructor.
     * Register SBMLObjects for cyId lookup from SBMLDocument.
     *
     * @param document SBMLDocument for which the mapping is created.
     */
	public CyIdSBaseMap(SBMLDocument document){
		this();
		logger.debug("Create CyIdSBaseMap for SBMLDocument");
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

            // UnitDefinitions
            // stored with prefix due to different namespace
            for (UnitDefinition ud: model.getListOfUnitDefinitions()){
                objectMap.put(unitDefinitionCyId(ud), ud);
                for (Unit unit: ud.getListOfUnits()){
                    String cyId = unitCyId(ud, unit);
                    objectMap.put(cyId, unit);
                }
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
                String cyId = initialAssignmentCyId(variable);
                objectMap.put(cyId, assignment);
            }

            // Rules (no ids)
            for (Rule rule: model.getListOfRules()){
                String variable = SBMLUtil.getVariableFromRule(rule);
                String cyId = ruleCyId(variable);
                objectMap.put(cyId, rule);
            }

            // Reactions
            addListOf(model.getListOfReactions());
			
			// LocalParameters & KineticLaws
			for (Reaction reaction : model.getListOfReactions()){
				if (reaction.isSetKineticLaw()){
					KineticLaw law = reaction.getKineticLaw();
					String lawCyId = kineticLawCyId(reaction);
					objectMap.put(lawCyId, law);
				
					// this were made unique during reading of SBML
					for (LocalParameter lp: law.getListOfLocalParameters()){
					    String lpCyId = localParameterCyId(reaction, lp);
						objectMap.put(lpCyId, lp);
					}
				}
			}

            // Constraints
            ListOf<Constraint> constraints = model.getListOfConstraints();
            for (int k=0; k<constraints.size(); k++) {
                Constraint constraint = constraints.get(k);
                String cyId = CyIdSBaseMap.constraintCyId(k);
                objectMap.put(cyId, constraint);
            }

            // Events
            ListOf<Event> events = model.getListOfEvents();
            for (int k=0; k<events.size(); k++) {
                Event event = events.get(k);
                String cyId = CyIdSBaseMap.eventCyId(k);
                objectMap.put(cyId, event);
            }

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
			logger.error("CyIdSBaseMap could not be created", t);
			t.printStackTrace();
		}
	}

    /**
     * Generic function to add ListOf to the objectMapping.
     * The NamedSBase can be mapped via their ids.
     * @param list
     */
	private void addListOf(ListOf<? extends NamedSBase> list){
		for (NamedSBase obj: list ){
			objectMap.put(obj.getId(), obj);
		}
	}

    /**
     * Get SBase object by cyId.
     * @param cyId
     * @return
     */
	public SBase getObjectByCyId(String cyId){
		SBase sbase = null;
		if (objectMap.containsKey(cyId)){
			return objectMap.get(cyId);
		}
		return sbase;
	}

	/** Get SBase Objects in the map. */
	public Collection<SBase> getObjects(){
	    return objectMap.values();
	}


    /////////////////////////////////////////////////////////////////////////////////////////
    // CYID HELPER
    /////////////////////////////////////////////////////////////////////////////////////////
    /*
     * Necessary to create unique cyIds for all SBase objects.
     * For NamedSBases this are directly the ids, for SBases the ids have to be generated.
     */

    public static final String CYID_SEPARATOR = "_";
    public static final String CYID_UNITSID_PREFIX = "UnitSId__";
    public static final String CYID_PREFIX_CONSTRAINT = "constraint_";
    public static final String CYID_PREFIX_EVENT = "event_";

    /**
     * Creates unique cyId for LocalParameter.
     *
     * @param reaction
     * @param lp
     * @return
     */
    public static String localParameterCyId(Reaction reaction, LocalParameter lp){
        String id = lp.getId();
        String reactionId = reaction.getId();
        if (id.startsWith(reactionId)){
            return id;
        }else {
            return String.format("%s_%s", reaction.getId(), lp.getId());
        }
    }

    /**
     * Creates unique cyId for KineticLaw.
     * Uses the reaction id in which the kinetic law is defined.
     *
     * @param reaction
     * @return
     */
    public static String kineticLawCyId(Reaction reaction){
        return String.format("%s_%s", reaction.getId(), SBML.SUFFIX_KINETIC_LAW);
    }

    public static String initialAssignmentCyId(String variable){
        return String.format("%s_%s", variable, SBML.SUFFIX_INITIAL_ASSIGNMENT);
    }

    public static String ruleCyId(String variable){

        return String.format("%s_%s", variable, SBML.SUFFIX_RULE);
    }

    /**
     * Creates unique cyId for Unit.
     * A unit is only unique in combination with its UnitDefinition.
     *
     * @param unitDefinition
     * @param unit
     * @return
     */
    public static String unitCyId(UnitDefinition unitDefinition, Unit unit){
        return String.format("%s%s%s%s",
                CYID_UNITSID_PREFIX, unitDefinition.getId(), CYID_SEPARATOR, unit.getKind().toString());
    }

    /**
     * Creates unique cyId for UnitDefinition.
     * @param ud
     * @return
     */
    public static String unitDefinitionCyId(UnitDefinition ud){
        return String.format("%s%s",
                CYID_UNITSID_PREFIX, ud.getId());
    }

    /**
     * Creates unique cyId for Constraint.
     * @param counter
     * @return
     */
    public static String constraintCyId(Integer counter){
        return String.format("%s%s",
                CYID_PREFIX_CONSTRAINT, counter);
    }

    /**
     * Creates unique cyId for Constraint.
     * @param counter
     * @return
     */
    public static String eventCyId(Integer counter){
        return String.format("%s%s",
                CYID_PREFIX_EVENT, counter);
    }

    public static String eventAssignmentCyId(Integer counter){
        return String.format("%s%s",
                CYID_PREFIX_EVENT, counter);
    }




}
