package org.cy3sbml.mapping;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cy3sbml.CyActivator;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Here the mapping between SBML documents and Cytoscape networks is stored.
 * When an SBML is read and networks are created, than the respective 
 * SBMLDocument is added to the Mapping for all created networks.
 * Networks are identified via their network suids.
 * 
 * This implements only the mapping store. 
 * The update of the current status has to be managed by the SBMLManager via listening
 * to NetworkSelected events.
 */
public class SBML2NetworkMapper {
	private static final Logger logger = LoggerFactory.getLogger(SBML2NetworkMapper.class);
	
	/* Currently used SBMLdocument and mapping information. */
	private Long currentSUID;
	
	/* Store of all SBMLdocuments and mapping information. */
	private Map<Long, SBMLDocument> documentMap;
	private Map<Long, OneToManyMapping> NSBToNodeMappingMap;
	private Map<Long, OneToManyMapping> nodeToNSBMappingMap;
	
	public SBML2NetworkMapper(){
		logger.info("SBML2NetworkMapper created");
		initMaps();
		initCurrent();
	}
	
	private void initCurrent(){
		currentSUID = null;
	}
	
	private void initMaps(){
		documentMap = new HashMap<Long, SBMLDocument>();
		NSBToNodeMappingMap = new HashMap<Long, OneToManyMapping>();
		nodeToNSBMappingMap = new HashMap<Long, OneToManyMapping>();
	}
	
	/** Updates the current SBMLDocument and mappings.
	 * This function has to be triggered to update the underlying mappings
	 * and SBMLDocuments used for display.
	 */
	public void setCurrent(Long suid){
		if (suid != null && documentMap.containsKey(suid)){
			currentSUID = suid;
		} else {
			initCurrent();
		}
	}
	
	public Set<Long> keySet(){
		return documentMap.keySet();
	}
	
	public void putDocument(Long suid, SBMLDocument doc, NamedSBaseToNodeMapping mapping){
		documentMap.put(suid,  doc);
		NSBToNodeMappingMap.put(suid, mapping);
		nodeToNSBMappingMap.put(suid, OneToManyMapping.createReverseMapping(mapping));
	}
	
	public void removeDocument(Long deletedNetworkSUID){
		if (currentSUID == deletedNetworkSUID){
			initCurrent();
		}
		documentMap.remove(deletedNetworkSUID);
		NSBToNodeMappingMap.remove(deletedNetworkSUID);
		nodeToNSBMappingMap.remove(deletedNetworkSUID);
	}
	
	public SBMLDocument getCurrentDocument(){
		if (currentSUID != null){
			logger.warn("No current SUID set. SBMLDocument can not be retrieved !");
			return null;
		} else {
			return documentMap.get(currentSUID);	
		}
	}
	
	public OneToManyMapping getCurrentNodeToNSBMapping(){
		if (currentSUID != null){
			logger.warn("No current SUID set. Mapping can not be retrieved !");
			return null;
		} else {
			return nodeToNSBMappingMap.get(currentSUID);	
		}
	}
	
	public OneToManyMapping getCurrentNSBToNodeMapping(){
		if (currentSUID != null){
			logger.warn("No current SUID set. Mapping can not be retrieved !");
			return null;
		} else {
			return NSBToNodeMappingMap.get(currentSUID);	
		}
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
}
