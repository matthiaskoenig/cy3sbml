package org.cy3sbml.mapping;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.sbml.jsbml.SBMLDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Mapping between CyNetworks and SBMLDocuments.
 *
 * When an SBML is read and one or multiple networks are created, the links
 * between the SBMLDocument and these network(s) are stored in the 
 * Network2SBMLMapper for lookup. Networks are identified uniquely via their network SUID.
 */
public class Network2SBMLMapper implements Serializable{
	private static final Logger logger = LoggerFactory.getLogger(Network2SBMLMapper.class);
	private static final long serialVersionUID = 1L;

	private Map<Long, SBMLDocument> documentMap;
	private Map<Long, One2ManyMapping<String, Long>> NSBToNodeMappingMap;
	private Map<Long, One2ManyMapping<Long, String>> nodeToNSBMappingMap;
	
	public Network2SBMLMapper(){
		logger.debug("Network2SBMLMapper created");
		initMaps();
	}

	private void initMaps(){
		documentMap = new HashMap<>();
		NSBToNodeMappingMap = new HashMap<>();
		nodeToNSBMappingMap = new HashMap<>();
	}

	/**
     * Get SBMLDocument for network SUID.
     */
    public SBMLDocument getDocumentForSUID(Long SUID){
        SBMLDocument doc = null;
        if (SUID == null) {
            logger.debug("No SUID set. No SBMLDocument can be retrieved !");
            return null;
        }
        if (documentMap.containsKey(SUID)){
            doc = documentMap.get(SUID);
        }
        return doc;
    }

	public boolean containsNetwork(Long SUID){
	    return (documentMap.containsKey(SUID));
	}
	
	public Set<Long> keySet(){
	    return documentMap.keySet();
	}

    public Map<Long, SBMLDocument> getDocumentMap(){
        return documentMap;
    }

    /**
     * The SBMLDocument is stored with the SBML id to SUID mapping.
     * A reverse mapping from SUIDs to SBML ids is created in the proces.
     */
	public void putDocument(Long SUID, SBMLDocument doc, One2ManyMapping<String, Long> mapping){
        logger.debug("Network put: " + SUID.toString());
	    documentMap.put(SUID,  doc);
		NSBToNodeMappingMap.put(SUID, mapping);
		nodeToNSBMappingMap.put(SUID, mapping.createReverseMapping());
	}

	public void removeDocument(Long deletedNetworkSUID){
		logger.debug("Network remove:" + deletedNetworkSUID.toString());
		documentMap.remove(deletedNetworkSUID);
		NSBToNodeMappingMap.remove(deletedNetworkSUID);
		nodeToNSBMappingMap.remove(deletedNetworkSUID);
	}


    public One2ManyMapping<Long, String> getCyNode2NSBMapping(Long SUID){
        if (SUID == null) {
            logger.warn("No current SUID set. Mapping can not be retrieved !");
            return null;
        }
        return nodeToNSBMappingMap.get(SUID);
    }

    public One2ManyMapping<String, Long> getNSB2CyNodeMapping(Long SUID){
        if (SUID == null){
            logger.warn("No current SUID set. Mapping can not be retrieved !");
            return null;
        }
        return NSBToNodeMappingMap.get(SUID);
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