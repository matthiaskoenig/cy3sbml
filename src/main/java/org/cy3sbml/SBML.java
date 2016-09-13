package org.cy3sbml;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Definition of cy3sbml constants.
 *
 * Here the attribute names for nodes, edges and networks are defined.
 * All code depending on cy3sbml attributes should access these via the
 * SBML constants.
 *
 * There is no guarantee that the Strings will remain identical, but the
 * SBML fields will remain the same.
 */

public class SBML {

    // -------------------------------------------------------------------------
    public static final String NETWORKTYPE_ATTR = "sbmlNetwork";

    public static final String NETWORKTYPE_SBML = "sbml";
    public static final String NETWORKTYPE_LAYOUT = "sbmlLayout";

    public static final String LEVEL_VERSION = "sbmlVersion";

    public static final String NODETYPE_ATTR = SBMLCore.SBML_TYPE_ATTR;

    public static final String INTERACTION_ATTR = SBMLCore.INTERACTION_TYPE_ATTR;

    // unique id created for SBase objects (used for mapping)
    public static final String ATTR_CYID = "cyId";

    // -----------------------
    // Node & edge attributes
    // -----------------------
    public static final String LABEL = "label";
    public static final String ATTR_ID = SBMLCore.SBML_ID_ATTR;
    public static final String ATTR_UNIT_SID = "unitSid";
    public static final String ATTR_PORT_SID = "portSid";
    // The name must be assigned to "shared name" to make sure it is a NetworkCollection Column.
    // it is available as name for the subnetwork.
    public static final String ATTR_NAME = "shared name"; // SBMLCore.NODE_NAME_ATTR_LABEL;
    public static final String ATTR_SBOTERM = "sbo";
    public static final String ATTR_METAID = "metaId";

    public static final String ATTR_COMPARTMENT = SBMLCore.SBML_COMPARTMENT_ATTR;
    public static final String ATTR_INITIAL_CONCENTRATION = SBMLCore.SBML_INITIAL_CONCENTRATION_ATTR;
    public static final String ATTR_INITIAL_AMOUNT = SBMLCore.SBML_INITIAL_AMOUNT_ATTR;
    public static final String ATTR_CHARGE = SBMLCore.SBML_CHARGE_ATTR;
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
    public static final String ATTR_VARIABLE = "variable";
    public static final String ATTR_SPATIAL_DIMENSIONS = "spatialDimensions";
    public static final String ATTR_MESSAGE = "message";
    public static final String ATTR_USE_VALUES_FROM_TRIGGER_TIME = "useValuesFromTriggerTime";

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

    public static final String ATTR_UNIT_KIND = "kind";
    public static final String ATTR_UNIT_EXPONENT = "exponent";
    public static final String ATTR_UNIT_SCALE = "scale";
    public static final String ATTR_UNIT_MULTIPLIER = "multiplier";

    // qual
    public static final String ATTR_QUAL_MAX_LEVEL = "qual_maxLevel";
    public static final String ATTR_QUAL_INITIAL_LEVEL = "qual_initialLevel";
    public static final String ATTR_QUAL_SIGN = "qual_sign";
    public static final String ATTR_QUAL_THRESHOLD_LEVEL = "qual_tresholdLevel";
    public static final String ATTR_QUAL_TRANSITION_EFFECT = "qual_transitionEffect";
    public static final String ATTR_QUAL_QUALITATIVE_SPECIES = "qual_qualitativeSpecies";
    public static final String ATTR_QUAL_OUTPUT_LEVEL = "qual_outputLevel";
    public static final String ATTR_QUAL_RESULT_LEVELS = "qual_resultLevels";

    // fbc
    public static final String ATTR_FBC_STRICT = "fbc_strict";
    public static final String ATTR_FBC_CHARGE = "fbc_charge";
    public static final String ATTR_FBC_CHEMICAL_FORMULA = "fbc_chemicalFormula";
    public static final String ATTR_FBC_LOWER_FLUX_BOUND = "fbc_lowerFluxBound";
    public static final String ATTR_FBC_UPPER_FLUX_BOUND = "fbc_upperFluxBound";
    public static final String ATTR_FBC_OBJECTIVE_TEMPLATE = "fbc_objective-%1$s";

    // comp
    public static final String ATTR_COMP_PORTREF = "comp_portRef";
    public static final String ATTR_COMP_IDREF = "comp_idRef";
    public static final String ATTR_COMP_UNITREF = "comp_unitRef";
    public static final String ATTR_COMP_METAIDREF = "comp_metaIdRef";

    public static final String ATTR_COMP_MODELREF = "comp_modelRef";
    public static final String ATTR_COMP_TIME_CONVERSION_FACTOR = "comp_timeConversionFactor";
    public static final String ATTR_COMP_EXTENT_CONVERSION_FACTOR = "comp_extentConversionFactor";

	// ----------------------
    // Node types
    // ----------------------
    public static final String NODETYPE_SPECIES = SBMLCore.SBML_TYPE_SPECIES;
    public static final String NODETYPE_PARAMETER = "parameter";
    public static final String NODETYPE_COMPARTMENT = "compartment";
    public static final String NODETYPE_REACTION = SBMLCore.SBML_TYPE_REACTION;
    public static final String NODETYPE_RATE_RULE = "rateRule";
    public static final String NODETYPE_ASSIGNMENT_RULE = "assignmentRule";
    public static final String NODETYPE_ALGEBRAIC_RULE = "algebraicRule";
    public static final String NODETYPE_INITIAL_ASSIGNMENT = "initialAssignment";
    public static final String NODETYPE_KINETIC_LAW = "kineticLaw";
    public static final String NODETYPE_LOCAL_PARAMETER = "localParameter";
    public static final String NODETYPE_FUNCTION_DEFINITION = "functionDefinition";
    public static final String NODETYPE_UNIT_DEFINITION = "unitDefinition";
    public static final String NODETYPE_UNIT = "unit";
    public static final String NODETYPE_EVENT = "event";
    public static final String NODETYPE_EVENT_ASSIGNMENT = "eventAssignment";
    public static final String NODETYPE_CONSTRAINT = "constraint";

	// qual
    public static final String NODETYPE_QUAL_SPECIES = "qual_species";
    public static final String NODETYPE_QUAL_TRANSITION = "qual_transition";

    // fbc
    public static final String NODETYPE_FBC_GENEPRODUCT = "fbc_geneProduct";
    public static final String NODETYPE_FBC_AND = "fbc_and";
    public static final String NODETYPE_FBC_OR = "fbc_or";

    // comp
    public static final String NODETYPE_COMP_SUBMODEL = "comp_submodel";
    public static final String NODETYPE_COMP_DELETION = "comp_deletion";
    public static final String NODETYPE_COMP_PORT = "comp_port";
    public static final String NODETYPE_COMP_REPLACED_BY = "comp_replacedBy";
    public static final String NODETYPE_COMP_REPLACED_ELEMENT = "comp_replacedElement";

    // layout
    public static final String NODETYPE_LAYOUT_SPECIESGLYPH = "layout:speciesGlyph";
    public static final String NODETYPE_LAYOUT_REACTIONGLYPH = "layout:reactionGlyph";

    // group
    public static final String NODETYPE_GROUP = "group";

    // ----------------------
    // Edge types
    // ----------------------
    public static final String INTERACTION_REACTION_REACTANT = SBMLCore.INTERACTION_TYPE_REACTION_REACTANT; // "reactant_reaction";
    public static final String INTERACTION_REACTION_PRODUCT = SBMLCore.INTERACTION_TYPE_REACTION_PRODUCT;   // "reaction_product";
    public static final String INTERACTION_REACTION_MODIFIER = SBMLCore.INTERACTION_TYPE_REACTION_MODIFIER; // "modifier_reaction";
    public static final String INTERACTION_REACTION_ACTIVATOR = "reaction-activator";
    public static final String INTERACTION_REACTION_INHIBITOR = "reaction-inhibitor";

    public static final String INTERACTION_SPECIES_COMPARTMENT = "species_compartment";
    public static final String INTERACTION_REACTION_COMPARTMENT = "reaction_compartment";
    public static final String INTERACTION_PARAMETER_REACTION = "parameter_reaction";
    public static final String INTERACTION_VARIABLE_RULE = "variable_rule";
    public static final String INTERACTION_VARIABLE_INITIAL_ASSIGNMENT = "variable_initialAssignment";


    public static final String INTERACTION_REFERENCE_RULE = "reference_rule";
    public static final String INTERACTION_REFERENCE_INITIAL_ASSIGNMENT = "reference_initialAssignment";
    public static final String INTERACTION_REFERENCE_KINETICLAW = "reference_kineticLaw";
    public static final String INTERACTION_REFERENCE_FUNCTIONDEFINITION = "reference_functionDefinition";
    public static final String INTERACTION_REFERENCE_EVENT_ASSIGNMENT = "reference_eventAssignment";


    public static final String INTERACTION_REACTION_KINETICLAW = "reaction_kineticLaw";
    public static final String INTERACTION_LOCALPARAMETER_KINETICLAW = "localParameter_kineticLaw";

    public static final String INTERACTION_UNIT_UNITDEFINITION = "unit_unitDefinition";
    public static final String INTERACTION_SBASE_UNITDEFINITION = "sbase_unitDefinition";

    public static final String INTERACTION_TRIGGER_EVENT = "trigger_event";
    public static final String INTERACTION_PRIORITY_EVENT = "priority_event";
    public static final String INTERACTION_DELAY_EVENT = "delay_event";

    public static final String INTERACTION_EVENT_EVENT_ASSIGNMENT = "reference_eventAssignment";
    public static final String INTERACTION_VARIABLE_EVENT_ASSIGNMENT = "variable_eventAssignment";

	// qual
    public static final String INTERACTION_QUAL_TRANSITION_INPUT = "input_transition";
    public static final String INTERACTION_QUAL_TRANSITION_OUTPUT = "transition_output";

    // fbc
    public static final String INTERACTION_FBC_GENEPRODUCT_SPECIES = "species_geneProduct";
    public static final String INTERACTION_FBC_ASSOCIATION_REACTION = "association_reaction";
    public static final String INTERACTION_FBC_ASSOCIATION_ASSOCIATION = "association_association";

    // comp
    public static final String INTERACTION_COMP_SBASEREF_ID = "sbaseRef-id";
    public static final String INTERACTION_COMP_SBASEREF_PORT = "sbaseRef-port";
    public static final String INTERACTION_COMP_SBASEREF_UNIT = "sbaseRef-unit";
    public static final String INTERACTION_COMP_SBASEREF_METAID = "sbaseRef-metaId";



    // -------------------------------------------------------------------------
    // Visualization attributes
    // Some derived attributes are calculated for visual representation.
    // -------------------------------------------------------------------------
    public static final String NODETYPE_ATTR_EXTENDED = SBMLCore.SBML_TYPE_ATTR + " ext";
    public static final String INTERACTION_ATTR_EXTENDED = "shared interaction";

    public static final String ATTR_COMPARTMENT_CODE = "compartmentCode";
    public static final String NODETYPE_REACTION_REVERSIBLE = NODETYPE_REACTION + " reversible";
    public static final String NODETYPE_REACTION_IRREVERSIBLE = NODETYPE_REACTION + " irreversible";

    // -------------------------------------------------------------------------
    // Important SBO terms used in styles
    // -------------------------------------------------------------------------
    public static final Set<String> SBO_MODIFIERS;
    public static final Set<String> SBO_ACTIVATORS;
    public static final Set<String> SBO_INHIBITORS;

    static {
        Set<String> mSet = new HashSet<String>();
        mSet.add("SBO:0000019"); // modfier
        mSet.add("SBO:0000595"); // dual-activity modifier
        mSet.add("SBO:0000596"); // modifier of unknown activity
        SBO_MODIFIERS = Collections.unmodifiableSet(mSet);

        Set<String> aSet = new HashSet<String>();
        aSet.add("SBO:0000459"); // stimulator
        aSet.add("SBO:0000013"); // catalyst
        aSet.add("SBO:0000460"); // enzymatic catalyst
        aSet.add("SBO:0000462"); // non-essential activator
        aSet.add("SBO:0000021"); // potentiator
        aSet.add("SBO:0000461"); // essential activator
        aSet.add("SBO:0000535"); // binding activator
        aSet.add("SBO:0000534"); // catalytic activator
        aSet.add("SBO:0000533"); // specific activator
        SBO_ACTIVATORS = Collections.unmodifiableSet(aSet);

        Set<String> iSet = new HashSet<String>();
        iSet.add("SBO:0000020"); //inhibitor
        iSet.add("SBO:0000206"); //competitive inhibitor
        iSet.add("SBO:0000597"); //silencer
        iSet.add("SBO:0000207"); //non-competitive inhibitor
        iSet.add("SBO:0000537"); //complete-inhibitor
        iSet.add("SBO:0000536"); //partial-inhibitor
        SBO_INHIBITORS = Collections.unmodifiableSet(iSet);
    }

    // -------------------------------------------------------------------------
    // Core Network
    // -------------------------------------------------------------------------
    public static final String[] coreNodeTypes = {
            SBML.NODETYPE_SPECIES,
            SBML.NODETYPE_REACTION,

            SBML.NODETYPE_QUAL_SPECIES,
            SBML.NODETYPE_QUAL_TRANSITION,

            SBML.NODETYPE_FBC_GENEPRODUCT,
            SBML.NODETYPE_FBC_AND,
            SBML.NODETYPE_FBC_OR
    };

    public static final String[] coreEdgeTypes = {
            SBML.INTERACTION_REACTION_REACTANT,
            SBML.INTERACTION_REACTION_PRODUCT,
            SBML.INTERACTION_REACTION_MODIFIER,
            SBML.INTERACTION_QUAL_TRANSITION_INPUT,
            SBML.INTERACTION_QUAL_TRANSITION_OUTPUT,
            SBML.INTERACTION_FBC_GENEPRODUCT_SPECIES,
            SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION,
            SBML.INTERACTION_FBC_ASSOCIATION_REACTION
    };

    // -------------------------------------------------------------------------
    // Kinetic Network
    // -------------------------------------------------------------------------
    public static final String[] kineticNodeTypes = {
            SBML.NODETYPE_SPECIES,
            SBML.NODETYPE_PARAMETER,
            SBML.NODETYPE_COMPARTMENT,
            SBML.NODETYPE_REACTION,
            SBML.NODETYPE_RATE_RULE,
            SBML.NODETYPE_ASSIGNMENT_RULE,
            SBML.NODETYPE_ALGEBRAIC_RULE,
            SBML.NODETYPE_INITIAL_ASSIGNMENT,
            SBML.NODETYPE_KINETIC_LAW,
            SBML.NODETYPE_LOCAL_PARAMETER,
            SBML.NODETYPE_FUNCTION_DEFINITION,

            SBML.NODETYPE_QUAL_SPECIES,
            SBML.NODETYPE_QUAL_TRANSITION,

            SBML.NODETYPE_FBC_GENEPRODUCT,
            SBML.NODETYPE_FBC_AND,
            SBML.NODETYPE_FBC_OR,

            SBML.NODETYPE_COMP_PORT,
    };

    public static final String[] kineticEdgeTypes = {
            SBML.INTERACTION_REACTION_REACTANT,
            SBML.INTERACTION_REACTION_PRODUCT,
            SBML.INTERACTION_REACTION_MODIFIER,
            SBML.INTERACTION_SPECIES_COMPARTMENT,
            SBML.INTERACTION_REACTION_COMPARTMENT,
            SBML.INTERACTION_PARAMETER_REACTION,
            SBML.INTERACTION_VARIABLE_RULE,
            SBML.INTERACTION_VARIABLE_INITIAL_ASSIGNMENT,
            SBML.INTERACTION_REFERENCE_RULE,
            SBML.INTERACTION_REFERENCE_INITIAL_ASSIGNMENT,
            SBML.INTERACTION_REFERENCE_KINETICLAW,
            SBML.INTERACTION_REACTION_KINETICLAW,
            SBML.INTERACTION_LOCALPARAMETER_KINETICLAW,

            SBML.INTERACTION_QUAL_TRANSITION_INPUT,
            SBML.INTERACTION_QUAL_TRANSITION_OUTPUT,

            SBML.INTERACTION_FBC_GENEPRODUCT_SPECIES,
            SBML.INTERACTION_FBC_ASSOCIATION_ASSOCIATION,
            SBML.INTERACTION_FBC_ASSOCIATION_REACTION,

            SBML.INTERACTION_COMP_SBASEREF_ID,
            SBML.INTERACTION_COMP_SBASEREF_UNIT,
            SBML.INTERACTION_COMP_SBASEREF_PORT,
            SBML.INTERACTION_COMP_SBASEREF_METAID,
    };

    // -------------------------------------------------------------------------
    // Default Layout
    // -------------------------------------------------------------------------
    public static final String SBML_LAYOUT = "force-directed";

    // Styles
    public static final String STYLE_CY3SBML = "cy3sbml";
    public static final String STYLE_CY3SBML_DARK = "cy3sbml-dark";

    // -------------------------------------------------------------------------
    // cy3sbml properties
    // -------------------------------------------------------------------------
    public static final String PROPERTY_VISUAL_STYLE = "cy3sbml.visualStyle";

    // -------------------------------------------------------------------------
    // cy3sbml folder
    // -------------------------------------------------------------------------

    private SBML() {};

}
