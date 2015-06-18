package org.cy3sbml.biomodel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.cy3sbml.SBMLFileFilter;
import org.cy3sbml.SBMLReader;
import org.cy3sbml.ServiceAdapter;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TaskMonitor;


public class LoadBioModelTask implements Task{
	private String id;
	private BioModelWSInterface bmInterface;
	private ServiceAdapter adapter;

	
	// TODO: the SBMLReader is the correct factory
	public LoadBioModelTask(String id, BioModelWSInterface bmInterface, ServiceAdapter adapter) {
		this.id = id;
		this.bmInterface = bmInterface;
		this.adapter = adapter;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Load BioModel " + id);
		taskMonitor.setStatusMessage("Loading BioModel SBML via WebInterface ...");
		taskMonitor.setProgress(0.0);
		
		try {
			
			String sbml = bmInterface.getBioModelSBMLById(id);
			
			if (sbml == null || sbml.equals("") || sbml.startsWith(id)){
				JOptionPane.showMessageDialog(adapter.cySwingApplication.getJFrame(),
						String.format("<html>No SBML for BioModel Id : <b>%s</b></html>", id));
			}
			else{
				InputStream instream = new ByteArrayInputStream(sbml.getBytes("UTF-8"));
				// TODO: convert to tmp file and use the core-task read Network from file task
				
				taskMonitor.setProgress(0.4);
				taskMonitor.setStatusMessage("Creating Cytoscape network from SBML ...");
				
				// Create the SBML network
				// TODO: only one global file filter (remove code doubling)
				SBMLFileFilter sbmlFilter = new SBMLFileFilter("SBML files (*.xml)", adapter.streamUtil);
				SBMLReader sbmlReader = new SBMLReader(sbmlFilter, adapter);
				TaskIterator iterator = sbmlReader.createTaskIterator(instream, "BioModel SBML");
				adapter.taskManager.execute(iterator);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
		taskMonitor.setProgress(1.0);
	}

	@Override
	public void cancel() {
		// TODO: implement
		
	}
}