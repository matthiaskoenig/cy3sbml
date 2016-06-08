package org.cy3sbml.util;

import java.util.HashSet;

import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.Parameter;

public class ASTNodeUtil {
	
	public static HashSet<Parameter> findReferencedGlobalParameters(ASTNode astNode){
		
		HashSet<Parameter> pSet = new HashSet<Parameter>();
	    if (astNode.getType().equals(ASTNode.Type.NAME)
	        && (astNode.getVariable() instanceof Parameter)
	        && (astNode.getParentSBMLObject().getModel().getParameter(astNode.getVariable().getId()) != null)) {
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
	public static HashSet<NamedSBase> findReferencedNamedSBases(ASTNode astNode){
		HashSet<NamedSBase> nsbSet = new HashSet<NamedSBase>();
	    if ((astNode.getType().equals(ASTNode.Type.NAME) || (astNode.getType().equals(ASTNode.Type.FUNCTION) ) 
	    		&& (astNode.getVariable() instanceof NamedSBase))){
	      nsbSet.add((NamedSBase) astNode.getVariable());
	    }
	    // recursive search
	    for (ASTNode child : astNode.getListOfNodes()) {
	      nsbSet.addAll(ASTNodeUtil.findReferencedNamedSBases(child));
	    }
	    return nsbSet;
	}
}
