package org.cy3sbml;

public class SBML {
	public static final String NETWORKTYPE_ATTR = "SBML_NETWORK";
	public static final String NODETYPE_ATTR = "type";
	public static final String INTERACTION_ATTR = "interaction";
	
	public static final String LABEL = "label"; 
	
	public static final String ATTR_ID = "id";
	public static final String ATTR_TYPE = "type";
	public static final String ATTR_METAID = "metaId";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_COMPARTMENT = "compartment";
	public static final String ATTR_INITIAL_CONCENTRATION = "initialConcentration";
	public static final String ATTR_INITIAL_AMOUNT = "initialAmount";
	public static final String ATTR_CHARGE = "charge";
	public static final String ATTR_SBOTERM = "sbo";
	public static final String ATTR_CONSTANT = "constant";
	public static final String ATTR_BOUNDARY_CONDITION = "boundaryCondition";
	public static final String ATTR_HAS_ONLY_SUBSTANCE_UNITS = "hasOnlySubstanceUnits";
	public static final String ATTR_REVERSIBLE = "reversible";
	public static final String ATTR_STOICHIOMETRY = "stoichiometry";
	public static final String ATTR_MAX_LEVEL = "maxLevel";
	public static final String ATTR_INITIAL_LEVEL = "initialLevel";
	public static final String ATTR_CONVERSION_FACTOR = "conversionFactor";
	public static final String ATTR_UNITS = "units";
	public static final String ATTR_DERIVED_UNITS = "derivedUnits";
	public static final String ATTR_VALUE = "value";
	public static final String ATTR_FAST = "fast";
	public static final String ATTR_KINETIC_LAW = "kineticLaw";
	
	public static final String ATTR_AREA_UNITS = "areaUnits";
	public static final String ATTR_EXTENT_UNITS = "extentUnits";
	public static final String ATTR_LENGTH_UNITS = "lengthUnits";
	public static final String ATTR_SUBSTANCE_UNITS = "substanceUnits";
	public static final String ATTR_TIME_UNITS = "timeUnits";
	public static final String ATTR_VOLUME_UNITS = "volumeUnits";
	
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
