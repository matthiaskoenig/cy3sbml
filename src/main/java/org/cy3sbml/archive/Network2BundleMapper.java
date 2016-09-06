package org.cy3sbml.archive;

import org.apache.taverna.robundle.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Mapping between CyNetworks and Taverna bundles.
 */
public class Network2BundleMapper implements Serializable{
	private static final Logger logger = LoggerFactory.getLogger(Network2BundleMapper.class);
	private static final long serialVersionUID = 1L;

	private Map<Long, Bundle> map;


	public Network2BundleMapper(){
		logger.debug("Network2BundleMapper created");
		initMaps();
    }

	private void initMaps(){
		map = new HashMap<>();
	}

    /**
     * The Bundle is stored with the SBML id to SUID (CyNode) mapping.
     */
    public void put(Long rootSUID, Bundle bundle){
        logger.debug("Network put: " + rootSUID.toString());
        map.put(rootSUID,  bundle);
    }

    /**
     * Removes bundle for a given root network SUID.
     * This removes the mapping for all subnetworks of the given root network SUID.
     *
     * @param rootSUID root network SUID
     */
    public void remove(Long rootSUID){
        logger.debug("Network remove:" + rootSUID.toString());
        map.remove(rootSUID);
    }

    /**
     * Get Bundle for given rootNetworkSUID.
     *
     * @param rootSUID root network SUID
     * @return SBMLDocument or null
     */
    public Bundle get(Long rootSUID){
        Bundle doc = null;
        if (rootSUID == null) {
            logger.debug("No SUID set. No bundle can be retrieved !");
            return null;
        }
        if (map.containsKey(rootSUID)){
            doc = map.get(rootSUID);
        }
        return doc;
    }

    /**
     * Exists bundle for given rootNetwork.
     *
     * @param rootSUID root network SUID
     * @return
     */
	public boolean contains(Long rootSUID){
	    return (map.containsKey(rootSUID));
	}

    /**
     * Get all rootNetwork SUIDs which have an association SBMLDocument.
     *
     * @return Set of root network SUIDs
     */
	public Set<Long> keySet(){
	    return map.keySet();
	}


    /**
     * Get map.
     *
     * @return
     */
    public Map<Long, Bundle> getMap(){
        return map;
    }


    /**
     * Create information string.
     *
     * @return
     */
	public String toString(){
		String info = String.format("\n--- %s ---\n", getClass().getName());
		for (Long key: map.keySet()){
			info += String.format("%s -> %s\n", key.toString(), map.get(key).toString());
		}
		info += "-------------------------------";
		return info;
	}
}