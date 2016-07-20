package org.cy3sbml;

/**
 * Use identical fields to the SBMLCoreReader.
 * As long as these are not exported, they have to be redefined here.
 *
 * https://github.com/matthiaskoenig/cy3sbml/issues/106
 * http://code.cytoscape.org/redmine/issues/3638
 */
public class SBMLCoreReader {
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
    public static final String KINETIC_LAW_UNITS_ATTR_TEMPLATE = "kineticLaw-%1$s-units"; //$NON-NLS-1$

    private SBMLCoreReader(){}
}
