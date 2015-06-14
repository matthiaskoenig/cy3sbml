package biomodel;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import javax.swing.JOptionPane;

import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;

import cysbml.SBMLGraphReader;
import cysbml.tools.ProxyTools;

public class LoadBioModelTask implements Task{
	private TaskMonitor taskMonitor;
	private String id;

	public LoadBioModelTask(String id) {
		this.id = id;		
	}

	public void setTaskMonitor(TaskMonitor monitor)
			throws IllegalThreadStateException {
		taskMonitor = monitor;
	}

	public void halt() {}

	public String getTitle() {
		return "Load BioModel " + id;
	}

	public void run() {
		taskMonitor.setStatus("Loading BioModel SBML via WebInterface ...");
		taskMonitor.setPercentCompleted(-1);
		
		try {
			String proxyHost = ProxyTools.getCytoscapeProxyHost();
			String proxyPort = ProxyTools.getCytoscapeProxyPort();
			
			BioModelWSInterface bmInterface = new BioModelWSInterface(proxyHost, proxyPort);
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
}