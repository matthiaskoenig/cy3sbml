package org.cy3sbml.mapping;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


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
 * Class handles SBase lookup from given metaId.
 * Most of the functionality could be done via
 * the doc.getElementbyMetaId();
 */
public class MetaIdSBaseMap {
	private static final Logger logger = LoggerFactory.getLogger(MetaIdSBaseMap.class);
	private Map<String, SBase> sbaseMap;

    /** Constructor. */
	public MetaIdSBaseMap(){
	    sbaseMap = new HashMap<>();
	}
	
	/**
     * Constructor.
     * Register SBMLObjects for cyId lookup from SBMLDocument.
     *
     * @param document SBMLDocument for which the mapping is created.
     */
	public MetaIdSBaseMap(SBMLDocument document){
		this();
		logger.debug("Create MetaIdSBaseMap for SBMLDocument");
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
                return;
            }
            // UnitDefinitions
            addListOf(model.getListOfUnitDefinitions());
            for (UnitDefinition ud: model.getListOfUnitDefinitions()){
            	addListOf(ud.getListOfUnits());
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
            // InitialAssignments
            addListOf(model.getListOfInitialAssignments());
            // Rules
            addListOf(model.getListOfRules());
            // Reactions
            addListOf(model.getListOfReactions());
			// LocalParameters & KineticLaws
			for (Reaction reaction : model.getListOfReactions()){
				if (reaction.isSetKineticLaw()){
					KineticLaw law = reaction.getKineticLaw();
					add(law);
					addListOf(law.getListOfLocalParameters());
				}
			}
            // Constraints
			addListOf(model.getListOfConstraints());
            // Events
            addListOf(model.getListOfEvents());
			for (Event event: model.getListOfEvents()){
			    addListOf(event.getListOfEventAssignments());
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
			logger.error("MetaIdSBaseMap could not be created", t);
			t.printStackTrace();
		}
	}

    /**
     * Generic function to add ListOf to the objectMapping.
     * The NamedSBase can be mapped via their metaIds.
     * All metaIds are set prior to this function.
     *
     * @param list
     */
	private void addListOf(ListOf<? extends SBase> list){
		for (SBase sbase: list){
		    add(sbase);
		}
	}

    private void add(SBase sbase){
        sbaseMap.put(sbase.getMetaId(), sbase);
    }


    /**
     * Get SBase object by cyId.
     * @param cyId
     * @return
     */
	public SBase getObjectByCyId(String cyId){
		SBase sbase = null;
		if (sbaseMap.containsKey(cyId)){
			return sbaseMap.get(cyId);
		}
		return sbase;
	}

    /**
     * Get SBase Objects in map.
     * @return collection of sbases
     */
	public Collection<SBase> getObjects(){
	    return sbaseMap.values();
	}

}
