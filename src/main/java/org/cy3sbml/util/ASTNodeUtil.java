package org.cy3sbml.util;

import java.util.HashSet;
import java.util.Set;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.NamedSBase;

/**
 * Helpers for the math (ASTNode) of SBML elements.
 */
public class ASTNodeUtil {

    /**
     * The NamedSBases the math references by name or as function, each once.
     *
     * @param astNode the math
     * @return the referenced NamedSBases
     */
    public static Set<NamedSBase> findReferencedNamedSBases(ASTNode astNode) {
        Set<NamedSBase> nsbSet = new HashSet<>();
        if ((astNode.getType().equals(ASTNode.Type.NAME) || astNode.getType().equals(ASTNode.Type.FUNCTION))
                && astNode.getVariable() != null) {
            nsbSet.add(astNode.getVariable());
        }
        // recursive search
        for (ASTNode child : astNode.getListOfNodes()) {
            nsbSet.addAll(ASTNodeUtil.findReferencedNamedSBases(child));
        }
        return nsbSet;
    }

    /**
     * Infix formula of the math, with the inline units of numbers ({@link UnitsFormulaCompiler}).
     * Use this instead of {@link ASTNode#toFormula()}, which drops the units.
     */
    public static String toFormula(ASTNode astNode) {
        return astNode.toFormula(new UnitsFormulaCompiler());
    }
}
