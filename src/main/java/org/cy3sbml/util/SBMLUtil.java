package org.cy3sbml.util;

import org.cy3sbml.SBML;
import org.sbml.jsbml.Rule;
import org.sbml.jsbml.AssignmentRule;
import org.sbml.jsbml.LocalParameter;
import org.sbml.jsbml.RateRule;

/**
 * Some utils to work with SBML and SBML naming.
 */
public class SBMLUtil {

    public static String localParameterId(String reactionId, LocalParameter lp){
        return String.format("%s_%s", reactionId, lp.getId());
    }

    public static String kineticLawId(String reactionId){
        return String.format("%s_%s", reactionId, SBML.SUFFIX_KINETIC_LAW);
    }

    public static String initialAssignmentId(String variable){
        return String.format("%s_%s", variable, SBML.SUFFIX_INITIAL_ASSIGNMENT);
    }

    public static String ruleId(String variable){
        return String.format("%s_%s", variable, SBML.SUFFIX_RULE);
    }

    /**
     * Get the variable from AssignmentRule and RateRule.
     * Returns the variable string if set, returns null if not set or
     * if the rule is an AlgebraicRule.
     */
    public static String getVariableFromRule(Rule rule){
        String variable = null;
        if (rule.isAssignment()){
            AssignmentRule assignmentRule = (AssignmentRule) rule;
            if (assignmentRule.isSetVariable()) {
                variable = assignmentRule.getVariable();
            }
        } else if (rule.isRate()){
            RateRule rateRule = (RateRule) rule;
            if (rateRule.isSetVariable()){
                variable = rateRule.getVariable();
            }
            variable = rateRule.getVariable();
        }
        return variable;
    }

}
