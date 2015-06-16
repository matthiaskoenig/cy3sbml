package org.cy3sbml.biomodel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.swing.JOptionPane;

import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.work.Task;
import org.cytoscape.work.TaskMonitor;


public class LoadBioModelTask implements Task{
	private String id;
	private BioModelWSInterface bmInterface;

	public LoadBioModelTask(String id, ServiceAdaptor adapter) {
		this.id = id;		
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		taskMonitor.setTitle("Load BioModel " + id);
		taskMonitor.setStatusMessage("Loading BioModel SBML via WebInterface ...");
		taskMonitor.setProgress(0.0);
		
		try {
			
			
			String sbml = bmInterface.getBioModelSBMLById(id);
			
			if (sbml == null || sbml.equals("") || sbml.startsWith(id)){
				JOptionPane.showMessageDialog(Cytoscape.getDesktop(),
						String.format("<html>No SBML for BioModel Id : <b>%s</b></html>", id));
			}
			else{
				InputStream instream = new ByteArrayInputStream(sbml.getBytes("UTF-8"));
				taskMonitor.setPercentCompleted(40);
				taskMonitor.setStatus("Creating Cytoscape network from SBML ...");
				Cytoscape.createNetwork(new SBMLGraphReader(instream),true, null);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}	
		taskMonitor.setPercentCompleted(100);
	}

	@Override
	public void cancel() {
		// TODO Auto-generated method stub
		
	}
}