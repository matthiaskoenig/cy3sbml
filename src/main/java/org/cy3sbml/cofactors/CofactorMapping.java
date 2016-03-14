package org.cy3sbml.cofactors;

import java.util.Map;

import org.cytoscape.model.CyNetwork;

/* Information which nodes map to which cofactor nodes.
 * 
 * The cofactor nodes are created and existing in parallel to the
 * original nodes (depending on the selected view (with or without
 * cofactors) these have to be removed from the network temporarily.
 */
public class CofactorMapping {
	// CyNode SUID -> CyNodes SUIDs
	private Map<Long, Long> cofactor2Nodes;
	
}
