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

import org.cy3sbml.mapping.SBML2NetworkMapper;
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
 * 
 * Which data has to be saved and restored?
 * - SBMLManager (SBMLDocuments & network2sbml mapper)
 * TODO: write restore function, which create the mapping from the rest
 * - CofactorManager
 * 
 * TODO: look into serializing the key singleton classes which store
 * 			the cy3sbml information.
 */
public class SessionData implements SessionAboutToBeSavedListener, SessionLoadedListener {
	private static final Logger logger = LoggerFactory.getLogger(SessionData.class);
	// File names for serialization
	private static final String DOCUMENT_MAP = "documentMap.ser";
	private File directory;
	
	/**
	 * Session data is locally saved in given directory.
	 * Normally this is the CytoscapeConfiguration/cy3sbml directory.
	 */
	public SessionData(File dir){
		directory = dir;
	}
	
	// Save app state in a file
	public void handleEvent(SessionAboutToBeSavedEvent e){
		// save app state file "myAppStateFile"
		logger.info("SessionAboutToBeSaved Event: Save cy3sbml session state");
		
		// Files to save
		List<File> files = new LinkedList<File>();
		
		// Save SBMLManager
		// Save all the SBML files & the sbml2networkMapping
		SBMLManager sbmlManager = SBMLManager.getInstance();
		SBML2NetworkMapper mapper = sbmlManager.getSBML2NetworkMapper();
		Map<Long, SBMLDocument> documentMap = mapper.getDocumentMap();
		
		// add xml files for all SBMLDocuments
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
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (XMLStreamException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
				
		// Serialize the documentMap
		logger.info("Serializing documentMap");
		try {
			File mapFile = new File(directory, DOCUMENT_MAP);
	        FileOutputStream fileOut;
			try {
				fileOut = new FileOutputStream(mapFile.getAbsolutePath());
				ObjectOutputStream out = new ObjectOutputStream(fileOut);
		        out.writeObject(documentMap);
		        out.close();
		        fileOut.close();
		        files.add(mapFile);
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} catch (IOException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		// Write the files in the session file
		try {
			e.addAppFiles("cy3sbml", files);
			logger.info("Save SBML files for session.");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
			
	}
	
	/**
	 * Restore app state from session files.
	 */
	public void handleEvent(SessionLoadedEvent e){
		// check if there is app file data
		if (e.getLoadedSession().getAppFileListMap() == null || e.getLoadedSession().getAppFileListMap().size() ==0){
	       return;
	    }       
		List<File> files = e.getLoadedSession().getAppFileListMap().get("cy3sbml");
		for (File f : files){
			logger.info("cy3sbml file in session:" + f.toString());
			logger.info(f.getName());

			// deserialize the documentMap
			if (DOCUMENT_MAP.equals(f.getName())){
				logger.info("Deserialize documentMap");
			    File mapFile = new File(directory, DOCUMENT_MAP);
				
			    InputStream file;
			    ObjectInput input;
				try {
					file = new FileInputStream(mapFile.getAbsolutePath());
					InputStream buffer = new BufferedInputStream(file);
					input = new ObjectInputStream (buffer);
					SBML2NetworkMapper documentMap = (SBML2NetworkMapper)input.readObject();
					
					// TODO: set the DocumentMap
					
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} catch (IOException e2) {
					// TODO Auto-generated catch block
					e2.printStackTrace();
				} catch (ClassNotFoundException e3) {
					// TODO Auto-generated catch block
					e3.printStackTrace();
				}
						
			}
		}
    }
	
}
