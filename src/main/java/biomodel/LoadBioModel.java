package biomodel;

import cytoscape.Cytoscape;
import cytoscape.task.ui.JTaskConfig;
import cytoscape.task.util.TaskManager;

public class LoadBioModel {
	public static void loadBioModelById(String id){
		System.out.println("CySBML[INFO] -> Load BioModel: " + id);		
		
		LoadBioModelTask task = new LoadBioModelTask(id);
		JTaskConfig jTaskConfig = new JTaskConfig();
		jTaskConfig.setOwner(Cytoscape.getDesktop());
		jTaskConfig.displayCloseButton(false);
		jTaskConfig.displayCancelButton(false);
		jTaskConfig.displayStatus(true);
		jTaskConfig.setAutoDispose(true);
		TaskManager.executeTask(task, jTaskConfig);
	}
}
