package org.cy3sbml;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.cy3sbml.mapping.NamedSBaseToNodeMapping;
import org.cy3sbml.mapping.OneToManyMapping;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.sbml.jsbml.SBMLDocument;


/** Storing of the SBMLDocument information. */
public class SBMLDocuments {
	
	private SBMLDocument currentDocument;
	private OneToManyMapping currentNodeToNSBMapping;
	private OneToManyMapping currentNSBToNodeMapping;
	
	private Map<String, SBMLDocument> documentMap;
	private Map<String, OneToManyMapping> NSBToNodeMappingMap;
	private Map<String, OneToManyMapping> nodeToNSBMappingMap;
	

	public SBMLDocuments(){
		documentMap = new HashMap<String, SBMLDocument>();
		NSBToNodeMappingMap = new HashMap<String, OneToManyMapping>();
		nodeToNSBMappingMap = new HashMap<String, OneToManyMapping>();
	}

	public SBMLDocument getCurrentDocument(){
		updateCurrentDocument();
		return currentDocument;
	}
	
	public SBMLDocument getDocumentForNetworkId(String networkId){
		SBMLDocument doc = null;
		if (documentMap.containsKey(networkId)){
			doc = documentMap.get(networkId);
		}
		return doc;
	}
	
	public Map<String, SBMLDocument> getDocumentMap(){
		return documentMap;
	}
	
	public OneToManyMapping getCurrentNodeToNSBMapping(){
		updateCurrentDocument();
		return currentNodeToNSBMapping;
	}
	public OneToManyMapping getCurrentNSBToNodeMapping(){
		updateCurrentDocument();
		return currentNSBToNodeMapping;
	}
	
	public void putDocument(String networkName, SBMLDocument doc, NamedSBaseToNodeMapping mapping){
		documentMap.put(networkName,  doc);
		NSBToNodeMappingMap.put(networkName, mapping);
		nodeToNSBMappingMap.put(networkName, OneToManyMapping.createReverseMapping(mapping));
		updateCurrentDocument();
	}
	
	public void removeDocument(String deletedNetworkKey){
		documentMap.remove(deletedNetworkKey);
		NSBToNodeMappingMap.remove(deletedNetworkKey);
		nodeToNSBMappingMap.remove(deletedNetworkKey);
		updateCurrentDocument();
	}
	
	public void updateDocuments(){
		HashSet<String> networkIds = new HashSet<String>();
		CyNetworkManager cyNetworkManager = SBMLManager.getInstance(null, null).getCyNetworkManager();
		for (CyNetwork network : cyNetworkManager.getNetworkSet()){
			// TODO: handle the ids as long SUID
			String id = network.getSUID().toString();
			networkIds.add(id);
		}
		for (String key : documentMap.keySet()){
			if (!networkIds.contains(key)){
				removeDocument(key);
			}
		}
		updateCurrentDocument();
	}
	
	public void updateCurrentDocument() {
		CyApplicationManager cyApplicationManager = SBMLManager.getInstance(null, null).getCyApplicationManager();
		
		CyNetwork network = cyApplicationManager.getCurrentNetwork();
		
		if (network != null){
			String key = network.getSUID().toString();
			if (documentMap.containsKey(key)){
				currentDocument = documentMap.get(key);
				currentNSBToNodeMapping = NSBToNodeMappingMap.get(key);
				currentNodeToNSBMapping = nodeToNSBMappingMap.get(key);
			} else {
				currentDocument = null;
				currentNSBToNodeMapping = new OneToManyMapping();
				currentNodeToNSBMapping = new OneToManyMapping();
			}
		}
	}
}
