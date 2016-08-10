package org.cy3sbml.cofactors;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cy3sbml.mapping.One2ManyMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Clone2cofactor and reverse mappings for the networks.
 * 
 * Lookup of the network via given (sub)network SUIDs.
 */
public class Network2CofactorMapper implements Serializable{
	private static final Logger logger = LoggerFactory.getLogger(Network2CofactorMapper.class);
	private static final long serialVersionUID = 1L;
	
	private Map<Long, One2ManyMapping<Long, Long>> cofactor2clone;
	private Map<Long, One2ManyMapping<Long, Long>> clone2cofactor;

	/** Constructor. */
	public Network2CofactorMapper(){
		logger.debug("Network2CofactorMapper created");
		cofactor2clone = new HashMap<>();
		clone2cofactor = new HashMap<>();
	}
	
	public boolean containsSUID(Long suid){
		return (cofactor2clone.containsKey(suid));
	}
	
	public Set<Long> keySet(){
		return cofactor2clone.keySet();
	}
	
	public One2ManyMapping<Long, Long> getCofactor2CloneMapping(Long networkSUID){
		return cofactor2clone.get(networkSUID);
	}
	
	public One2ManyMapping<Long, Long> getClone2CofactorMapping(Long networkSUID){
		return clone2cofactor.get(networkSUID);
	}
	
	/** Add a new mapping for given network. */
	public One2ManyMapping<Long, Long> newCofactor2CloneMapping(Long networkSUID){
		One2ManyMapping<Long, Long> cofactor2clones = new One2ManyMapping<Long, Long>();
		addCofactor2CloneMapping(networkSUID, cofactor2clones);
		return cofactor2clones;
	}
	
	public void addCofactor2CloneMapping(Long networkSUID, One2ManyMapping<Long, Long> cofactor2clones){
		cofactor2clone.put(networkSUID, cofactor2clones);
		// always add the reverse mapping
		clone2cofactor.put(networkSUID, cofactor2clones.createReverseMapping());
	}
	
	/** Use this function to add values. */
	public void put(Long networkSUID, Long cofactorSUID, Long cloneSUID){
		if (!cofactor2clone.containsKey(networkSUID)){
			newCofactor2CloneMapping(networkSUID);
		}
		cofactor2clone.get(networkSUID).put(cofactorSUID, cloneSUID);
		clone2cofactor.get(networkSUID).put(cloneSUID, cofactorSUID);
	}
	
	/** Use this function to remove values. */
	public void remove(Long networkSUID, Long cofactorSUID){
		HashSet<Long> cloneSUIDs = cofactor2clone.get(networkSUID).getValues(cofactorSUID);
		for (Long cloneSUID: cloneSUIDs){
			clone2cofactor.get(networkSUID).remove(cloneSUID);
		}
		cofactor2clone.get(networkSUID).remove(cofactorSUID);
	}
	
	/** 
	 * String representation.
	 * Lists the existing CofactorMappings for networks.
	 */
	public String toString(){
		String string = "------------------------\n"; 
		string += "Cofactor Mapping\n";
		string += "------------------------\n";
		for (Long suid : cofactor2clone.keySet()){
			string += "\n[network: " + suid + "]\n";
			One2ManyMapping<Long, Long> mapping = cofactor2clone.get(suid);
			string += mapping.toString();
			// add reverse mapping
			string += "\n";
			mapping = clone2cofactor.get(suid);
			string += mapping.toString();
		}
		return string;
	}
	
	
}
