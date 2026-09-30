package org.cy3sbml.util;

import javax.swing.tree.TreeNode;
import org.sbml.jsbml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper for mapping between Cytoscape and SBML objects.
 * <p>
 * A key requirement is the unique identification of SBase
 * objects withing the SBMLDocument.
 * This is performed via the MetaId.
 */
public class MappingUtil {
    private static final Logger logger = LoggerFactory.getLogger(MappingUtil.class);

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
     * Sets a metaId, unique in the document, on the SBase if it has none: the cyId of its
     * node. It is derived from the id, or for elements without id from the parent and the
     * element type.
     *
     * @param doc the document of the SBase
     * @param sbase the SBase
     */
    public static void setSBaseMetaId(SBMLDocument doc, SBase sbase) {
        if (sbase.isSetMetaId()) {
            return;
        }
        String metaId = null;

        // Units (separate namespace) //
        if (sbase instanceof UnitDefinition unitDefinition) {
            metaId = unitDefinitionMetaId(unitDefinition);
        } else if (sbase instanceof Unit unit) {
            metaId = unitMetaId(unit);
        }

        // NamedSBases
        else if (sbase instanceof NamedSBase nsb) {
            if (nsb.isSetId()) {
                metaId = nsb.getId();
            } else {
                metaId = SBMLUtil.getUnqualifiedClassName(sbase);
            }
        }
        // Kinetic Law
        else if (sbase instanceof KineticLaw kineticLaw) {
            metaId = kineticLawMetaId(kineticLaw);
        }
        // Initial Assignment
        else if (sbase instanceof InitialAssignment initialAssignment) {
            metaId = initialAssignmentMetaId(initialAssignment);
        }
        // Rule
        else if (sbase instanceof Rule rule) {
            metaId = ruleMetaId(rule);
        }
        // Constraint
        else if (sbase instanceof Constraint) {
            metaId = constraintMetaId();
        }
        // Event
        else if (sbase instanceof Event event) {
            metaId = eventMetaId(event);
        } else if (sbase instanceof EventAssignment) {
            metaId = eventAssignmentMetaId();
        }
        // other elements without id, e.g. the replaced elements of the comp package
        else {
            metaId = elementMetaId(sbase);
        }

        // create unique and set
        metaId = createUniqueMetaId(doc, metaId);
        try {
            sbase.setMetaId(metaId);
        } catch (PropertyNotAvailableException e) {
            // L1V2 models do not support setting metaId on compartments
            // this is mainly for backwards compatibility
            logger.warn("Property metaId is not defined");
        }
    }

    /**
     * The metaId, with a number appended if the document has it already.
     *
     * @param doc the document
     * @param metaId the metaId
     * @return a metaId that is not in the document
     */
    public static String createUniqueMetaId(SBMLDocument doc, String metaId) {
        String unique = metaId;
        int suffix = 0;
        while (doc.containsMetaId(unique)) {
            unique = metaId + suffix;
            suffix++;
        }
        return unique;
    }

    /**
     * MetaId of an element without id: the id of the closest parent with an id and the element
     * name, e.g. {@code p_replacedElement}.
     */
    private static String elementMetaId(SBase sbase) {
        for (TreeNode parent = sbase.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof SBase parentSBase && !(parent instanceof ListOf<?>) && parentSBase.isSetId()) {
                return parentSBase.getId() + "_" + sbase.getElementName();
            }
        }
        return sbase.getElementName();
    }

    /**
     * The id of the node of a local parameter, unique in the model:
     * {@code <reaction id>_<parameter id>}.
     *
     * @param lp the local parameter of a kinetic law
     * @return the id
     */
    public static String localParameterId(LocalParameter lp) {
        KineticLaw law = (KineticLaw) lp.getParent().getParent();
        Reaction reaction = law.getParent();
        return String.format("%s%s%s", reaction.getId(), SEPARATOR, lp.getId());
    }

    private static String unitDefinitionMetaId(UnitDefinition ud) {
        return String.format("%s%s", PREFIX_UNITSID, ud.getId());
    }

    private static String unitMetaId(Unit unit) {
        return String.format("%s%s", PREFIX_UNITSID, unit.getKind().toString());
    }

    private static String kineticLawMetaId(KineticLaw law) {
        Reaction reaction = law.getParent();
        return String.format("%s_%s", PREFIX_KINETIC_LAW, reaction.getId());
    }

    private static String initialAssignmentMetaId(InitialAssignment assignment) {
        String variable = assignment.isSetVariable() ? assignment.getVariable() : "";
        return String.format("%s_%s", PREFIX_INITIAL_ASSIGNMENT, variable);
    }

    private static String ruleMetaId(Rule rule) {
        if (rule instanceof AlgebraicRule) {
            return PREFIX_ALGEBRAIC_RULE;
        } else {
            String variable = "";
            if (rule instanceof AssignmentRule r) {
                variable = r.isSetVariable() ? r.getVariable() : "";
            } else if (rule instanceof RateRule r) {
                variable = r.isSetVariable() ? r.getVariable() : "";
            }
            return String.format("%s_%s", PREFIX_RULE, variable);
        }
    }

    private static String constraintMetaId() {
        return PREFIX_CONSTRAINT;
    }

    private static String eventMetaId(Event event) {
        if (event.isSetId()) {
            return event.getId();
        } else {
            return PREFIX_EVENT;
        }
    }

    private static String eventAssignmentMetaId() {
        return PREFIX_EVENT_ASSIGNMENT;
    }
}
