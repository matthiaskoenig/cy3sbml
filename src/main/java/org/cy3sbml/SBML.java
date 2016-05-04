package org.cy3sbml;

/**
 * Definition of cy3sbml and SBML constants.
 */
public class SBML {
	public static final String NETWORKTYPE_ATTR = "SBML_NETWORK";
	public static final String NETWORKTYPE_SBML = "SBML";
	public static final String NETWORKTYPE_LAYOUT = "LAYOUT";
	public static final String LEVEL_VERSION = "version";
	
	public static final String NODETYPE_ATTR = "sbml-type";
	public static final String INTERACTION_ATTR = "sbml-interaction";
	
	/* Node and edge attributes */
	public static final String LABEL = "label"; 
	public static final String ATTR_ID = "id";
	public static final String ATTR_NAME = "name";
	public static final String ATTR_SBOTERM = "sbo";
	public static final String ATTR_METAID = "metaId";
	
	public static final String ATTR_COMPARTMENT = "compartment";
	public static final String ATTR_COMPARTMENT_CODE = "compartmentCode";
	public static final String ATTR_INITIAL_CONCENTRATION = "initialConcentration";
	public static final String ATTR_INITIAL_AMOUNT = "initialAmount";
	public static final String ATTR_CHARGE = "charge";
	public static final String ATTR_CONSTANT = "constant";
	public static final String ATTR_BOUNDARY_CONDITION = "boundaryCondition";
	public static final String ATTR_HAS_ONLY_SUBSTANCE_UNITS = "hasOnlySubstanceUnits";
	public static final String ATTR_REVERSIBLE = "reversible";
	public static final String ATTR_STOICHIOMETRY = "stoichiometry";
	public static final String ATTR_CONVERSION_FACTOR = "conversionFactor";
	public static final String ATTR_VALUE = "value";
	public static final String ATTR_FAST = "fast";
	public static final String ATTR_KINETIC_LAW = "kineticLaw";
	public static final String ATTR_SIZE = "size";
	public static final String ATTR_SPATIAL_DIMENSIONS = "spatialDimensions";
	
	public static final String ATTR_UNITS = "units";
	public static final String ATTR_DERIVED_UNITS = "derivedUnits";
	public static final String ATTR_AREA_UNITS = "areaUnits";
	public static final String ATTR_EXTENT_UNITS = "extentUnits";
	public static final String ATTR_LENGTH_UNITS = "lengthUnits";
	public static final String ATTR_SUBSTANCE_UNITS = "substanceUnits";
	public static final String ATTR_TIME_UNITS = "timeUnits";
	public static final String ATTR_VOLUME_UNITS = "volumeUnits";
	
	public static final String ATTR_MATH = "math";
	public static final String ATTR_INITIAL_ASSIGNMENT = "initialAssignment";
	
	public static final String ATTR_QUAL_MAX_LEVEL = "qual:maxLevel";
	public static final String ATTR_QUAL_INITIAL_LEVEL = "qual:initialLevel";
	public static final String ATTR_QUAL_SIGN = "qual:sign";
	public static final String ATTR_QUAL_THRESHOLD_LEVEL = "qual:tresholdLevel";
	public static final String ATTR_QUAL_TRANSITION_EFFECT = "qual:transitionEffect";
	public static final String ATTR_QUAL_QUALITATIVE_SPECIES = "qual:qualitativeSpecies";
	public static final String ATTR_QUAL_OUTPUT_LEVEL = "qual:outputLevel";
	public static final String ATTR_QUAL_RESULT_LEVELS = "qual:resultLevels";
	
	public static final String ATTR_FBC_STRICT = "fbc:strict";
	public static final String ATTR_FBC_CHARGE = "fbc:charge";
	public static final String ATTR_FBC_CHEMICAL_FORMULA = "fbc:chemicalFormula";
	public static final String ATTR_FBC_LOWER_FLUX_BOUND = "fbc:lowerFluxBound";
	public static final String ATTR_FBC_UPPER_FLUX_BOUND = "fbc:upperFluxBound";
	public static final String ATTR_FBC_OBJECTIVE_TEMPLATE = "fbc:objective-%1$s";
	
	public static final String ATTR_COMP_PORTREF = "comp:portRef";
	public static final String ATTR_COMP_IDREF = "comp:idRef";
	public static final String ATTR_COMP_UNITREF = "comp:unitRef";
	public static final String ATTR_COMP_METAIDREF = "comp:metaIdRef";
	
	
	/** Node types */
	// core
	public static final String NODETYPE_SPECIES = "species";
	public static final String NODETYPE_PARAMETER = "parameter";
	public static final String NODETYPE_COMPARTMENT = "compartment";
	public static final String NODETYPE_REACTION = "reaction";
	public static final String NODETYPE_RULE = "rule";
	public static final String NODETYPE_INITIAL_ASSIGNMENT = "initialAssignment";
	public static final String NODETYPE_KINETIC_LAW = "kineticLaw";
	public static final String NODETYPE_LOCAL_PARAMTER = "localParameter";
	public static final String NODETYPE_FUNCTION_DEFINITION = "functionDefinition";

	// qual
	public static final String NODETYPE_QUAL_SPECIES = "qual:species";
	public static final String NODETYPE_QUAL_TRANSITION = "qual:transition";
	// fbc
	public static final String NODETYPE_FBC_GENEPRODUCT = "fbc:geneProduct";
	public static final String NODETYPE_FBC_AND = "fbc:and";
	public static final String NODETYPE_FBC_OR = "fbc:or";
	// comp
	public static final String NODETYPE_COMP_PORT = "comp:port";
	// layout
	public static final String NODETYPE_LAYOUT_SPECIESGLYPH = "layout:speciesGlyph";
	public static final String NODETYPE_LAYOUT_REACTIONGLYPH = "layout:reactionGlyph";
	
	/** Edge types */
	// core
	public static final String INTERACTION_REACTION_REACTANT = "reactant-reaction";
	public static final String INTERACTION_REACTION_PRODUCT = "reaction-product";
	public static final String INTERACTION_REACTION_MODIFIER = "modifier-reaction";
	
	public static final String INTERACTION_REACTION_ACTIVATOR = "activator-reaction";
	public static final String INTERACTION_REACTION_INHIBITOR = "inhibitor-reaction";
	public static final String INTERACTION_REACTION_SIDEPRODUCT = "reaction-sideproduct";
	public static final String INTERACTION_REACTION_SIDEREACTANT = "sidereactant-reaction";
	public static final String INTERACTION_UNDEFINED = "undefined";
	
	
	public static final String INTERACTION_SPECIES_COMPARTMENT = "species-compartment";
	public static final String INTERACTION_REACTION_COMPARTMENT = "reaction-compartment";
	public static final String INTERACTION_PARAMETER_REACTION = "parameter-reaction";
	public static final String INTERACTION_VARIABLE_RULE = "variable-rule";
	public static final String INTERACTION_VARIABLE_INITIAL_ASSIGNMENT = "variable-initialAssignment";
	public static final String INTERACTION_REFERENCE_RULE = "reference-rule";
	public static final String INTERACTION_REFERENCE_INITIAL_ASSIGNMENT = "reference-initialAssignment";
	public static final String INTERACTION_REACTION_KINETICLAW = "reaction-kineticLaw";
	public static final String INTERACTION_REFERENCE_KINETICLAW = "reference-kineticLaw";
	public static final String INTERACTION_REFERENCE_FUNCTIONDEFINITION = "reference-functionDefinition";
	
	// qual
	public static final String INTERACTION_QUAL_TRANSITION_INPUT = "input-transition";
	public static final String INTERACTION_QUAL_TRANSITION_OUTPUT = "transition-output";
	// fbc
	public static final String INTERACTION_FBC_GENEPRODUCT_SPECIES = "species-geneProduct";
	public static final String INTERACTION_FBC_ASSOCIATION_REACTION = "association-reaction";
	public static final String INTERACTION_FBC_ASSOCIATION_ASSOCIATION = "association-association";
	// comp
	public static final String INTERACTION_COMP_PORT_ID = "port-id";
}
