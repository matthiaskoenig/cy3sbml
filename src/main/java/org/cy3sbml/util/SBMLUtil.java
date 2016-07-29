package org.cy3sbml.util;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;

import org.sbml.jsbml.*;
import org.cy3sbml.SBML;

/**
 * Some utils to work with SBML and SBML naming.
 */
public class SBMLUtil {

    /**
     * Read the SBMLDocument from given SBML file resource.
     */
    public static SBMLDocument readSBMLDocument(String resource) {
        InputStream instream = SBMLUtil.class.getResourceAsStream(resource);
        SBMLDocument doc = null;
        try {
            String xml = IOUtil.inputStream2String(instream);
            doc = JSBML.readSBMLFromString(xml);
        } catch (IOException | XMLStreamException e) {
            e.printStackTrace();
        }
        return doc;
    }

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


    /**
     * Returns unqualified class name of a given object.
     */
    public static String getUnqualifiedClassName(Object obj){
        String name = obj.getClass().getName();
        if (name.lastIndexOf('.') > 0) {
            name = name.substring(name.lastIndexOf('.')+1);
        }
        // The $ can be converted to a .
        name = name.replace('$', '.');
        return name;
    }

}
