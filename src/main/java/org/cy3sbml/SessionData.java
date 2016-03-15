package org.cy3sbml;

import java.io.File;
import java.io.IOException;
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
		
		// save SBMLDocuments
		for (Long networkSuid : documentMap.keySet()){
			SBMLDocument doc = documentMap.get(networkSuid);
			SBMLWriter writer = new SBMLWriter();
			
			
			String modelId = "";
			Model model = doc.getModel();
			if ((model != null) && (model.isSetId())){
				modelId = model.getId();
			}
			
			File temp;
			try {
				temp = File.createTempFile(modelId, ".xml");
				try {
					writer.write(doc, temp);
					files.add(temp);
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
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
		}
		
		try {
			e.addAppFiles("cy3sbml", files);
			logger.info("Save SBML files for session.");
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	/**
	 * Restore app state from a file.
	 */
	public void handleEvent(SessionLoadedEvent e){
		// check if there is app file data
		if (e.getLoadedSession().getAppFileListMap() == null || e.getLoadedSession().getAppFileListMap().size() ==0){
	       return;
	    }       
		List<File> files = e.getLoadedSession().getAppFileListMap().get("cy3sbml");
		for (File sbmlFile : files){
			System.out.println("SBML in session:" + sbmlFile.toString());
		}
    }
	
}
