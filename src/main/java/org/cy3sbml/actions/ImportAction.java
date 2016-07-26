package org.cy3sbml.actions;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import javax.swing.ImageIcon;

import org.cy3sbml.gui.GUIConstants;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.work.TaskIterator;
import org.cy3sbml.ServiceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Importing SBML networks in Cytoscape.
 */
public class ImportAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(ImportAction.class);
	private static final long serialVersionUID = 1L;
	private ServiceAdapter adapter;

	public ImportAction(ServiceAdapter adapter){
		super("ImportAction");
		this.adapter = adapter;
		ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.IMAGE_IMPORT));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, "Import SBML");
		setToolbarGravity((float) 95.0);
	}
		
	public boolean isInToolBar() {
		return true;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		logger.debug("actionPerformed()"); 
		
		// open new file open dialog
		Collection<FileChooserFilter> filters = new HashSet<FileChooserFilter>();
		filters.add(new FileChooserFilter("SBML files (*.xml)", "xml"));
	
		File[] files = adapter.fileUtil.getFiles(adapter.cySwingApplication.getJFrame(), 
				"cy3sbml load SBML files", FileDialog.LOAD, filters);
		
		if ((files != null) && (files.length != 0)) {
			for (int i = 0; i < files.length; i++) {
				// TODO load the network file
				logger.info("Load: " + files[i].getName());
				TaskIterator iterator = adapter.loadNetworkFileTaskFactory.createTaskIterator(files[i]);
				adapter.synchronousTaskManager.execute(iterator);
			}
		}
	}
}
