package org.cy3sbml.cofactors;

import java.util.HashMap;
import java.util.Map;

import org.cy3sbml.mapping.SBML2NetworkMapper;
import org.cytoscape.model.CyNetwork;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to manage the cofactor nodes in a given network.
 * 
 * Has to manage for a given network the cofactor nodes.
 *
 */
public class CofactorManager {
	private static final Logger logger = LoggerFactory.getLogger(CofactorManager.class);
	
	// store of CofactorMappings for given network
	private Map<Long, CofactorMapping> network2CofactorMappings;
	
	public CofactorManager(){
		logger.info("CofactorManager created");
		network2CofactorMappings = new HashMap<Long, CofactorMapping>();
	}
	
	/*
	 * Splits the given node.
	 */
	public void splitCofactor(Long suid){
		System.out.println("split: " + suid.toString());
		// TODO: implement
		
	}
	

}



