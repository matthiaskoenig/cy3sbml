package org.cy3sbml;

public class SBML {
	// TODO: fix the attributes
	public static final String SBML_NETWORK = "SBML_NETWORK";
	public static final String NODE_NAME_ATTR_LABEL = "name"; 
	public static final String INTERACTION_TYPE_ATTR = "interaction type";
	public static final String SBML_TYPE_ATTR = "sbml type";
	public static final String SBML_ID_ATTR = "sbml id";
	public static final String SBML_INITIAL_CONCENTRATION_ATTR = "sbml initial concentration";
	public static final String SBML_INITIAL_AMOUNT_ATTR = "sbml initial amount";
	public static final String SBML_CHARGE_ATTR = "sbml charge";
	public static final String SBML_COMPARTMENT_ATTR = "sbml compartment";
	public static final String SBML_TYPE_SPECIES = "species";
	public static final String SBML_TYPE_REACTION = "reaction";
	public static final String INTERACTION_TYPE_REACTION_PRODUCT = "reaction-product";
	public static final String INTERACTION_TYPE_REACTION_REACTANT = "reaction-reactant";
	public static final String INTERACTION_TYPE_REACTION_MODIFIER = "reaction-modifier";
	public static final String KINETIC_LAW_ATTR_TEMPLATE = "kineticLaw-%1$s";
	public static final String KINETIC_LAW_UNITS_ATTR_TEMPLATE = "kineticLaw-%1$s-units";
	
	public static final String ATT_ID = "sbml id";
	public static final String ATT_TYPE = "sbml type";
	public static final String ATT_METAID = "sbml metaId";
	public static final String ATT_NAME = "sbml name";
	public static final String ATT_COMPARTMENT = "sbml compartment";
	public static final String ATT_INITIAL_CONCENTRATION = "sbml initialConcentration";
	public static final String ATT_INITIAL_AMOUNT = "sbml initialAmount";
	public static final String ATT_CHARGE = "sbml charge";
	public static final String ATT_SBOTERM = "sbml sbo";
	public static final String ATT_CONSTANT = "sbml constant";
	public static final String ATT_BOUNDARY_CONDITION = "sbml boundaryCondition";
	public static final String ATT_HAS_ONLY_SUBSTANCE_UNITS = "sbml hasOnlySubstanceUnits";
	public static final String ATT_REVERSIBLE = "sbml reversible";
	public static final String ATT_STOICHIOMETRY = "sbml stoichiometry";
	public static final String ATT_MAX_LEVEL = "sbml max level";
	public static final String ATT_INITIAL_LEVEL = "sbml initialLevel";
	
	public static final String NODETYPE_REACTION = "reaction";
	public static final String NODETYPE_SPECIES = "species";
	public static final String NODETYPE_QUAL_SPECIES = "qSpecies";
	public static final String NODETYPE_QUAL_TRANSITION = "qTransition";
	
	public static final String EDGETYPE_REACTION_ACTIVATOR = "activator-reaction";
	public static final String EDGETYPE_REACTION_INHIBITOR = "inhibitor-reaction";
	public static final String EDGETYPE_REACTION_MODIFIER = "modifier-reaction";
	
	public static final String EDGETYPE_REACTION_PRODUCT = "reaction-product";
	public static final String EDGETYPE_REACTION_SIDEPRODUCT = "reaction-sideproduct";
	
	public static final String EDGETYPE_REACTION_REACTANT = "reactant-reaction";
	public static final String EDGETYPE_REACTION_SIDEREACTANT = "sidereactant-reaction";
	
	public static final String EDGETYPE_TRANSITION_INPUT = "input-transition";
	public static final String EDGETYPE_TRANSITION_OUTPUT = "transition-output";
	public static final String EDGETYPE_UNDEFINED = "undefined";

}
