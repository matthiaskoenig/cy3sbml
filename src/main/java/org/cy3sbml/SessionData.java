package org.cy3sbml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;

import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.cofactors.Network2CofactorMapper;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.mapping.SBML2NetworkMapper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.session.CySession;
import org.cytoscape.session.events.SessionAboutToBeSavedEvent;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLException;
import org.sbml.jsbml.SBMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Save cy3sbml data in session file and restore from session file.
 */
public class SessionData implements SessionAboutToBeSavedListener, SessionLoadedListener {
	private static final Logger logger = LoggerFactory.getLogger(SessionData.class);
	// File names for serialization
	// private static final String DOCUMENT_MAP = "documentMap.ser";
	private static final String SBML2NETWORK_SERIALIZATION = "SBML2NetworkMapper.ser";
	private static final String NETWORK2COFACTOR_SERIALIZATION = "Network2Cofactors.ser";
	private File directory;
	
	/**
	 * Session data is locally saved in given directory.
	 * Normally this is the CytoscapeConfiguration/cy3sbml directory.
	 */
	public SessionData(File dir){
		directory = dir;
	}
	
	// Save app state in a file
	public void handleEvent(SessionAboutToBeSavedEvent event){
		logger.info("SessionAboutToBeSaved Event: Save cy3sbml session state");
		
		// Files to save
		List<File> files = new LinkedList<File>();
		
		// get SBMLManager for serialization
		SBMLManager sbmlManager = SBMLManager.getInstance();
		SBML2NetworkMapper mapper = sbmlManager.getSBML2NetworkMapper();
		Map<Long, SBMLDocument> documentMap = mapper.getDocumentMap();
		
		// Save all the SBML files
		for (Long networkSuid : documentMap.keySet()){
			SBMLDocument doc = documentMap.get(networkSuid);
			SBMLWriter writer = new SBMLWriter();
				
			String modelId = "";
			Model model = doc.getModel();
			if ((model != null) && (model.isSetId())){
				modelId = model.getId();
			}
			
			File sbmlFile = new File(directory, modelId + ".xml");
			try {
				writer.write(doc, sbmlFile);
				files.add(sbmlFile);
			} catch (SBMLException e1) {
				e1.printStackTrace();
			} catch (XMLStreamException e2) {
				e2.printStackTrace();
			} catch (IOException e3) {
				e3.printStackTrace();
			}
		}
				
		// Serialize
		logger.info("Serializing SBMl2NetworkMapper");
		try {
			File file = new File(directory, SBML2NETWORK_SERIALIZATION);
	        FileOutputStream fileOut;
			
			fileOut = new FileOutputStream(file.getAbsolutePath());
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
	        out.writeObject(mapper);
	        out.close();
	        fileOut.close();
	        files.add(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		// Serialize
		logger.info("Serializing Network2CofactorMapper");
		CofactorManager cofactorManager = CofactorManager.getInstance();
		Network2CofactorMapper network2cofactorMapper = cofactorManager.getNetwork2CofactorMapper();
		
		try {
			File file = new File(directory, NETWORK2COFACTOR_SERIALIZATION);
	        FileOutputStream fileOut;
			
			fileOut = new FileOutputStream(file.getAbsolutePath());
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
	        out.writeObject(network2cofactorMapper);
	        out.close();
	        fileOut.close();
	        files.add(file);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
		
		// Write files in session file
		try {
			event.addAppFiles("cy3sbml", files);
			logger.info("Save SBML files for session.");
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	/**
	 * Restore app state from session files.
	 */
	public void handleEvent(SessionLoadedEvent event){
		CySession session = event.getLoadedSession();
		// check if there is app file data
		if (session.getAppFileListMap() == null || session.getAppFileListMap().size() ==0){
	       return;
	    }       
		List<File> files = event.getLoadedSession().getAppFileListMap().get("cy3sbml");
		for (File f : files){
			logger.info("cy3sbml file in session: " + f.toString());
			logger.info(f.getName());

			try {
			
			// deserialize the documentMap
			if (SBML2NETWORK_SERIALIZATION.equals(f.getName())){
				logger.info("Deserialize documentMap");
			    File mapFile = new File(directory, SBML2NETWORK_SERIALIZATION);
				
			    InputStream inputStream;
			    ObjectInput input;
				try {
					inputStream = new FileInputStream(mapFile.getAbsolutePath());
					InputStream buffer = new BufferedInputStream(inputStream);
					input = new ObjectInputStream (buffer);
					
					// read mapper
					SBML2NetworkMapper mapper = (SBML2NetworkMapper)input.readObject();
					// update suids in mapper & set in manager
					SBML2NetworkMapper updatedMapper = updateSUIDsInMapper(session, mapper);
					SBMLManager sbmlManager = SBMLManager.getInstance();
					// set updated mapper
					sbmlManager.setSBML2NetworkMapper(updatedMapper);
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e2) {
					e2.printStackTrace();
				} catch (ClassNotFoundException e3) {
					e3.printStackTrace();
				}
			}
			
			// deserialize 
			else if (NETWORK2COFACTOR_SERIALIZATION.equals(f.getName())){
				logger.info("Deserialize Network2CofactorMapper");
			    File mapFile = new File(directory, NETWORK2COFACTOR_SERIALIZATION);
				
			    InputStream inputStream;
			    ObjectInput input;
				try {
					inputStream = new FileInputStream(mapFile.getAbsolutePath());
					InputStream buffer = new BufferedInputStream(inputStream);
					input = new ObjectInputStream (buffer);
					
					// read mapper
					Network2CofactorMapper m = (Network2CofactorMapper)input.readObject();
					CofactorManager cofactorManager = CofactorManager.getInstance();
					
					// update suids in mapper & set in manager
					Network2CofactorMapper updatedMapper = updateSUIDsInCofactorMapper(session, m);
					// set updated mapper
					cofactorManager.setNetwork2CofactorMapper(updatedMapper);
				
					System.out.println(cofactorManager.toString());
					
					
				} catch (FileNotFoundException e1) {
					e1.printStackTrace();
				} catch (IOException e2) {
					e2.printStackTrace();
				} catch (ClassNotFoundException e3) {
					e3.printStackTrace();
				}
			}
			
			} catch (Throwable e){
				logger.error("Errors in deserialization", e);
				e.printStackTrace();
			}
			
		}
    }
	
	/**
	 * Updates the changed SUIDs in the data structure.
	 * 
	 * SUIDs are not persistent across sessions.
	 * Consequently the mappings have to be updated.
	 * 
	 * The network, node and edge SUIDs can be updated via:
	 * 		Long newSUID = s.getObject(oldSUID, CyIdentifiable.class).getSUID();
	 */
	public SBML2NetworkMapper updateSUIDsInMapper(CySession s, SBML2NetworkMapper m){
		// mapper with updated SUIDS
		SBML2NetworkMapper newM = new SBML2NetworkMapper();
		
		// documentMap
		Map<Long, SBMLDocument> documentMap = m.getDocumentMap();
		for (Long networkSuid: documentMap.keySet()){
			Long newNetworkSuid = s.getObject(networkSuid, CyNetwork.class).getSUID();
			SBMLDocument doc = documentMap.get(networkSuid);
			One2ManyMapping<String, Long> nsb2node = m.getNSB2CyNodeMapping(networkSuid);
			
			// replace keys in nsb2node mapping
			One2ManyMapping<String, Long> newNsb2node = new One2ManyMapping<String, Long>();
			for (String key: nsb2node.keySet()){
				for (Long suid : nsb2node.getValues(key)){
					Long newSuid = s.getObject(suid, CyNode.class).getSUID();
					newNsb2node.put(key, newSuid);	
				}
			}
			newM.putDocument(newNetworkSuid, doc, newNsb2node);
		}
		if (m.getCurrentSUID() != null){
			Long newCurrentSuid = s.getObject(m.getCurrentSUID(), CyNetwork.class).getSUID();
			newM.setCurrentSUID(newCurrentSuid);
		}
		return newM;
	}
	
	/**
	 * Updates the changed SUIDs in the data structure.
	 * 
	 * SUIDs are not persistent across sessions.
	 * Consequently the mappings have to be updated.
	 * 
	 * The network, node and edge SUIDs can be updated via:
	 * 		Long newSUID = s.getObject(oldSUID, CyIdentifiable.class).getSUID();
	 */
	public Network2CofactorMapper updateSUIDsInCofactorMapper(CySession s, Network2CofactorMapper m){
		logger.debug("Update SUIDs in Network2CofactorMapper");
		// mapper with updated SUIDS
		Network2CofactorMapper newM = new Network2CofactorMapper();
	
		// update all network & node SUIDs
		for (Long networkSUID: m.keySet()){
			Long newNetworkSUID = s.getObject(networkSUID, CyNetwork.class).getSUID();
			One2ManyMapping<Long, Long> cofactor2clones = m.getCofactor2CloneMapping(networkSUID);
			for (Long cofactorSUID: cofactor2clones.keySet()){
				// update cofactor SUID
				Long newCofactorSUID = s.getObject(cofactorSUID, CyNode.class).getSUID();
				for (Long cloneSUID: cofactor2clones.getValues(cofactorSUID)){
					// update clone SUID
					Long newCloneSUID = s.getObject(cloneSUID, CyNode.class).getSUID();
					newM.put(newNetworkSUID, newCofactorSUID, newCloneSUID);
				}
			}
		}
		return newM;
	}
	
}
