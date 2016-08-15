package org.cy3sbml.util;
import org.sbml.jsbml.*;

/**
 * Helper for mapping between Cytoscape and SBML objects.
 *
 * A key requirement is the unique identification of SBase
 * objects withing the SBMLDocument.
 * This is performed via the MetaId.
 */
public class MappingUtil {

    public static final String SEPARATOR = "_";
    public static final String PREFIX_UNITSID = "UnitSId" + SEPARATOR + SEPARATOR;

    public static final String PREFIX_INITIAL_ASSIGNMENT = "assignment" + SEPARATOR;
    public static final String PREFIX_KINETIC_LAW = "law" + SEPARATOR;
    public static final String PREFIX_RULE = "rule" + SEPARATOR;
    public static final String PREFIX_ALGEBRAIC_RULE = "algebraicRule" + SEPARATOR;
    public static final String PREFIX_CONSTRAINT = "constraint" + SEPARATOR;
    public static final String PREFIX_EVENT = "event" + SEPARATOR;
    public static final String PREFIX_EVENT_ASSIGNMENT = "event" + SEPARATOR;

    /**
     * Set the unique metaId for the given sbase.
     *
     * @param doc SBMLDocument for the sbase
     * @param sbase
     */
    public static void setSBaseMetaId(SBMLDocument doc, SBase sbase){
        if (sbase.isSetMetaId()){ return; }
        String metaId = null;

        // Units (separate namespace) //
        if (sbase instanceof UnitDefinition){
            metaId = unitDefinitionMetaId((UnitDefinition) sbase);
        }
        else if (sbase instanceof Unit) {
            metaId = unitMetaId((Unit) sbase);
        }

        // NamedSBases
        else if (sbase instanceof NamedSBase){
            NamedSBase nsb = (NamedSBase) sbase;
            if (nsb.isSetId()){
                metaId = nsb.getId();
            } else {
                metaId = "nsb";
            }
        }
        // Kinetic Law
        else if (sbase instanceof KineticLaw){
            metaId = kineticLawMetaId((KineticLaw) sbase);
        }
        // Initial Assignment
        else if (sbase instanceof InitialAssignment){
            metaId = initialAssignmentMetaId((InitialAssignment) sbase);
        }
        // Rule
        else if (sbase instanceof Rule){
            metaId = ruleMetaId((Rule) sbase);
        }
        // Constraint
        else if (sbase instanceof Constraint){
            metaId = constraintMetaId((Constraint) sbase);
        }
        // Event
        else if (sbase instanceof Event){
            metaId = eventMetaId((Event) sbase);
        }
        else if (sbase instanceof EventAssignment){
            metaId = eventAssignmentMetaId((EventAssignment) sbase);
        }

        // create unique and set
        metaId = createUniqueMetaId(doc, metaId);
        sbase.setMetaId(metaId);
    }

    /**
     * Creates unique metaId in the model.
     * @param doc
     * @param metaId
     * @return metaId not in the SBMLDocument.
     */
    public static String createUniqueMetaId(SBMLDocument doc, String metaId) {
        Integer suffix = 0;
        while(doc.containsMetaId(metaId)) {
            metaId = String.format("%s%s", metaId, suffix);
            suffix++;
        }
        return metaId;
    }

    /////////////////////////////////////////////////////////////////////////////////////////
    // METAID FACTORIES
    /////////////////////////////////////////////////////////////////////////////////////////

    public static String localParameterId(LocalParameter lp){
        Reaction reaction = (Reaction) lp.getParent().getParent();
        return String.format("%s%s%s",
                reaction.getId(), SEPARATOR, lp.getId());
    }

    private static String unitDefinitionMetaId(UnitDefinition ud){
        return String.format("%s%s",
                PREFIX_UNITSID, ud.getId());
    }

    private static String unitMetaId(Unit unit){
        return String.format("%s%s",
                PREFIX_UNITSID, unit.getKind().toString());
    }

    private static String kineticLawMetaId(KineticLaw law){
        Reaction reaction = law.getParent();
        return String.format("%s_%s", PREFIX_KINETIC_LAW, reaction.getId());
    }

    private static String initialAssignmentMetaId(InitialAssignment assignment){
        String variable = (assignment.isSetVariable()) ? assignment.getVariable() : "";
        return String.format("%s_%s",
                PREFIX_INITIAL_ASSIGNMENT, variable);
    }

    private static String ruleMetaId(Rule rule){
        if (rule instanceof AlgebraicRule){
            return PREFIX_ALGEBRAIC_RULE;
        } else {
            String variable = "";
            if (rule instanceof AssignmentRule){
                AssignmentRule r = (AssignmentRule) rule;
                variable = (r.isSetVariable()) ? r.getVariable() : "";
            } else if (rule instanceof RateRule){
                RateRule r = (RateRule) rule;
                variable = (r.isSetVariable()) ? r.getVariable() : "";
            }
            return String.format("%s_%s",
                    PREFIX_RULE, variable);
        }
    }

    private static String constraintMetaId(Constraint constraint){
        return PREFIX_CONSTRAINT;
    }

    private static String eventMetaId(Event event){
        if (event.isSetId()){
            return event.getId();
        } else {
            return PREFIX_EVENT;
        }
    }

    private static String eventAssignmentMetaId(EventAssignment ea){
        return PREFIX_EVENT_ASSIGNMENT;
    }

}
