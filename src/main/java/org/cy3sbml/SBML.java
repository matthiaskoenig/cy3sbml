package org.cy3sbml;

public class SBML {
	public static final String NETWORKTYPE_ATTR = "SBML_NETWORK";
	public static final String NODETYPE_ATTR = "type";
	public static final String INTERACTION_ATTR = "interaction";
	
	public static final String LABEL = "name"; 
	
	public static final String ATTR_ID = "sbml id";
	public static final String ATTR_TYPE = "sbml type";
	public static final String ATTR_METAID = "sbml metaId";
	public static final String ATTR_NAME = "sbml name";
	public static final String ATTR_COMPARTMENT = "sbml compartment";
	public static final String ATTR_INITIAL_CONCENTRATION = "sbml initialConcentration";
	public static final String ATTR_INITIAL_AMOUNT = "sbml initialAmount";
	public static final String ATTR_CHARGE = "sbml charge";
	public static final String ATTR_SBOTERM = "sbml sbo";
	public static final String ATTR_CONSTANT = "sbml constant";
	public static final String ATTR_BOUNDARY_CONDITION = "sbml boundaryCondition";
	public static final String ATTR_HAS_ONLY_SUBSTANCE_UNITS = "sbml hasOnlySubstanceUnits";
	public static final String ATTR_REVERSIBLE = "sbml reversible";
	public static final String ATTR_STOICHIOMETRY = "sbml stoichiometry";
	public static final String ATTR_MAX_LEVEL = "sbml max level";
	public static final String ATTR_INITIAL_LEVEL = "sbml initialLevel";
	public static final String ATTR_CONVERSION_FACTOR = "sbml conversionFactor";
	public static final String ATTR_UNITS = "sbml units";
	public static final String ATTR_VALUE = "sbml value";
	public static final String ATTR_FAST = "sbml fast";
	public static final String ATTR_KINETIC_LAW = "sbml kineticLaw";
	
	public static final String ATTR_AREA_UNITS = "sbml areaUnits";
	public static final String ATTR_EXTENT_UNITS = "sbml extentUnits";
	public static final String ATTR_LENGTH_UNITS = "sbml lengthUnits";
	public static final String ATTR_SUBSTANCE_UNITS = "sbml substanceUnits";
	public static final String ATTR_TIME_UNITS = "sbml timeUnits";
	public static final String ATTR_VOLUME_UNITS = "sbml volumeUnits";
	
	public static final String KINETIC_LAW_ATTR_TEMPLATE = "kineticLaw-%1$s";
	public static final String KINETIC_LAW_UNITS_ATTR_TEMPLATE = "kineticLaw-%1$s-units";
	
	// node types
	public static final String NODETYPE_REACTION = "reaction";
	public static final String NODETYPE_SPECIES = "species";
	public static final String NODETYPE_QUAL_SPECIES = "qSpecies";
	public static final String NODETYPE_QUAL_TRANSITION = "qTransition";
	
	// edge types (interactions)
	public static final String INTERACTION_REACTION_ACTIVATOR = "activator-reaction";
	public static final String INTERACTION_REACTION_INHIBITOR = "inhibitor-reaction";
	public static final String INTERACTION_REACTION_MODIFIER = "modifier-reaction";
	public static final String INTERACTION_REACTION_PRODUCT = "reaction-product";
	public static final String INTERACTION_REACTION_SIDEPRODUCT = "reaction-sideproduct";
	public static final String INTERACTION_REACTION_REACTANT = "reactant-reaction";
	public static final String INTERACTION_REACTION_SIDEREACTANT = "sidereactant-reaction";
	public static final String INTERACTION_TRANSITION_INPUT = "input-transition";
	public static final String INTERACTION_TRANSITION_OUTPUT = "transition-output";
	public static final String INTERACTION_UNDEFINED = "undefined";
}
