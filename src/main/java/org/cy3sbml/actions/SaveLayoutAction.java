package org.cy3sbml.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.HashSet;
import javax.swing.ImageIcon;

import org.cy3sbml.gui.GUIConstants;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.layout.LayoutTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** 
 * Save node positions to file. 
 */
public class SaveLayoutAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(SaveLayoutAction.class);
	private static final long serialVersionUID = 1L;
	
	private ServiceAdapter adapter;
	
	/** Constructor. */
	public SaveLayoutAction(ServiceAdapter adapter){
		super(SaveLayoutAction.class.getSimpleName());
		this.adapter = adapter;
		
		ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_SAVELAYOUT));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_SAVELAYOUT);
		setToolbarGravity(GUIConstants.GRAVITY_SAVELAYOUT);
	}
	
	public boolean insertSeparatorBefore(){
		return true;
	}
	
	public boolean isInToolBar() {
		return true;
	}
	
	public boolean isInMenuBar() {
		return false;
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		logger.debug("actionPerformed()");
		
		Component frame = adapter.cySwingApplication.getJFrame();
		HashSet<FileChooserFilter> filters = new HashSet<>();
		FileChooserFilter filter = new FileChooserFilter("Layout File", "xml");
		filters.add(filter);
	    FileUtil fileUtil = adapter.fileUtil;
	    File xmlFile = fileUtil.getFile(frame, "Save Layout in XML", FileUtil.SAVE, filters);
	    
		LayoutTools layoutTools = new LayoutTools(adapter);
		layoutTools.saveLayoutOfCurrentViewInFile(xmlFile);
	}
}
