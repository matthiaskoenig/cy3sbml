package org.cy3sbml.util;

import org.cy3sbml.SBML;
import org.cy3sbml.mapping.MetaIdSBaseMap;
import org.sbml.jsbml.*;

/**
 * Helper for mapping between Cytoscape and SBML objects.
 *
 * A key requirement is the unique identification of SBase
 * objects withing the SBMLDocument.
 * This is performed via the MetaId.
 */
public class MappingUtil {
    public static final String CYID_SEPARATOR = "_";
    public static final String CYID_UNITSID_PREFIX = "UnitSId__";
    public static final String CYID_PREFIX_CONSTRAINT = "constraint_";
    public static final String CYID_PREFIX_EVENT = "event_";


    /**
     * Set the unique metaId for the given sbase.
     *
     * @param doc SBMLDocument for the sbase
     * @param sbase
     */
    public static void setSBaseMetaId(SBMLDocument doc, SBase sbase){
        if (sbase.isSetMetaId()){
            return;
        }

        Model model;
        model.getListOf

        // separate namespace

        // UnitDefinition
        MetaIdSBaseMap.unitDefinitionCyId(ud);

        // Unit
        MetaIdSBaseMap.unitCyId(ud, unit)

        // FIXME: metaIds have to be unique
        if (sbase instanceof NamedSBase){
            NamedSBase nsb = (NamedSBase) sbase;
            if (nsb.isSetId()){
                String id = nsb.getId();
                // test if already registered and make unique
                SBase item = doc.getElementByMetaId(id);
                if (item == null){
                    nsb.setMetaId(id);
                }

            }
        }


        // handle all the case

        // Units

        // NamedSBases

        // TODO: all the SBases which do not have id



        MetaIdSBaseMap.kineticLawCyId(reaction);

        MetaIdSBaseMap.initialAssignmentCyId(variable);

        if (rule instanceof AlgebraicRule){
            cyId = "algebraicRule_" + k;
            ruleType = SBML.NODETYPE_ALGEBRAIC_RULE;
        } else {
            variable = SBMLUtil.getVariableFromRule(rule);
            cyId = MetaIdSBaseMap.ruleCyId(variable);
            if (rule instanceof AssignmentRule){
                ruleType = SBML.NODETYPE_ASSIGNMENT_RULE;
            } else if (rule instanceof RateRule){
                ruleType = SBML.NODETYPE_RATE_RULE;
            }
        }

        MetaIdSBaseMap.constraintCyId(k);

        String cyId = MetaIdSBaseMap.eventCyId(k);

    }


    /////////////////////////////////////////////////////////////////////////////////////////
    // CYID HELPER
    /////////////////////////////////////////////////////////////////////////////////////////
    /*
     * Necessary to create unique cyIds for all SBase objects.
     * For NamedSBases this are directly the ids, for SBases the ids have to be generated.
     */



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
