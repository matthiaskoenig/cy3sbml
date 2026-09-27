package org.cy3sbml.util;

import java.util.HashSet;
import java.util.Set;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Parameter;

public class ASTNodeUtil {

    public static Set<Parameter> findReferencedGlobalParameters(ASTNode astNode) {

        HashSet<Parameter> pSet = new HashSet<Parameter>();
        if (astNode.getType().equals(ASTNode.Type.NAME)
                && (astNode.getVariable() instanceof Parameter)
                && (astNode.getParentSBMLObject()
                                .getModel()
                                .getParameter(astNode.getVariable().getId())
                        != null)) {
            pSet.add((Parameter) astNode.getVariable());
        }
        // recursive search
        for (ASTNode child : astNode.getListOfNodes()) {
            pSet.addAll(ASTNodeUtil.findReferencedGlobalParameters(child));
        }
        return pSet;
    }

    /*
     * Find all referenced NamedSBases in a given ASTNode.
     * Returns unique set (often multiple occurence of parameter, variable in equation.
     */
    public static Set<NamedSBase> findReferencedNamedSBases(ASTNode astNode) {
        HashSet<NamedSBase> nsbSet = new HashSet<NamedSBase>();
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
}
