package org.cy3sbml.mapping;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Mapping between SBMLDocuments and CyNetworks. 
 * When an SBML is read and one or multiple networks are created, the links
 * between the SBMLDocument and these network(s) are stored in the 
 * SBML2NetworkMapper for lookup. Networks are identified uniquely via their network SUIDs..
 * 
 * This implements only the mapping store. 
 * The update of the current status has to be managed by the SBMLManager via listening
 * to NetworkSelected events.
 * 
 * Changes to the data structure have to be reflected in the SessionData
 * which reads this information from the session file.
 */
public class SBML2NetworkMapper implements Serializable{
	private static final Logger logger = LoggerFactory.getLogger(SBML2NetworkMapper.class);
	private static final long serialVersionUID = 1L;
	

	// current networkSUID if in mapping
	private Long currentSUID;
	
	/* Store of all SBMLdocuments and mapping information. */
	private Map<Long, SBMLDocument> documentMap;
	private Map<Long, One2ManyMapping<String, Long>> NSBToNodeMappingMap;
	private Map<Long, One2ManyMapping<Long, String>> nodeToNSBMappingMap;
	
	public SBML2NetworkMapper(){
		logger.debug("SBML2NetworkMapper created");
		initMaps();
		initCurrent();
	}
	
	
	private void initCurrent(){
		currentSUID = null;
	}
	
	private void initMaps(){
		documentMap = new HashMap<Long, SBMLDocument>();
		NSBToNodeMappingMap = new HashMap<Long, One2ManyMapping<String, Long>>();
		nodeToNSBMappingMap = new HashMap<Long, One2ManyMapping<Long, String>>();
	}
	
	/** Updates the current SBMLDocument and mappings.
	 * This function has to be triggered to update the underlying mappings
	 * and SBMLDocuments used for display.
	 */
	public void setCurrentSUID(Long suid){
		if (suid != null && documentMap.containsKey(suid)){
			currentSUID = suid;
		} else {
			initCurrent();
		}
		logger.debug("Current network set to: " + currentSUID);
	}
	
	public boolean containsSUID(Long suid){
		return (documentMap.containsKey(suid));
	}
	
	public Set<Long> keySet(){
		return documentMap.keySet();
	}
	
	public void putDocument(Long suid, SBMLDocument doc, One2ManyMapping<String, Long> mapping){
		documentMap.put(suid,  doc);
		NSBToNodeMappingMap.put(suid, mapping);
		nodeToNSBMappingMap.put(suid, mapping.createReverseMapping());
		logger.debug("Network put: " + suid.toString());
	}
	
	public void removeDocument(Long deletedNetworkSUID){
		logger.debug("Network remove:" + deletedNetworkSUID.toString());
		if (currentSUID == deletedNetworkSUID){
			initCurrent();
		}
		documentMap.remove(deletedNetworkSUID);
		NSBToNodeMappingMap.remove(deletedNetworkSUID);
		nodeToNSBMappingMap.remove(deletedNetworkSUID);
	}
	
	public Long getCurrentSUID(){
		return currentSUID;
	}
	
	public SBMLDocument getCurrentDocument(){
		if (currentSUID == null){
			logger.debug("No current SUID set. No SBMLDocument can be retrieved !");
			return null;
		} else {
			return documentMap.get(currentSUID);	
		}
	}
	
	public One2ManyMapping<Long, String> getCurrentCyNode2NSBMapping(){
		if (currentSUID == null){
			logger.warn("No current SUID set. Mapping can not be retrieved !");
			return null;
		} else {
			return nodeToNSBMappingMap.get(currentSUID);	
		}
	}
	
	public One2ManyMapping<Long, String> getCyNode2NSBMapping(Long suid){
			return nodeToNSBMappingMap.get(suid);
	}
	
	public One2ManyMapping<String, Long> getCurrentNSBToCyNodeMapping(){
		if (currentSUID == null){
			logger.warn("No current SUID set. Mapping can not be retrieved !");
			return null;
		} else {
			return NSBToNodeMappingMap.get(currentSUID);	
		}
	}
	
	public One2ManyMapping<String, Long> getNSB2CyNodeMapping(Long suid){
		return NSBToNodeMappingMap.get(suid);
	}

	
	public SBMLDocument getDocumentForSUID(Long suid){
		SBMLDocument doc = null;
		if (documentMap.containsKey(suid)){
			doc = documentMap.get(suid);
		}
		return doc;
	}
	
	public Map<Long, SBMLDocument> getDocumentMap(){
		return documentMap;
	}
	
	public String toString(){
		String info = "\n--- SBML2NetworkMapping ---\n";
		for (Long key: documentMap.keySet()){
			info += String.format("%s -> %s\n", key.toString(), documentMap.get(key).toString());
		}
		info += "-------------------------------";
		return info;
	}
}
