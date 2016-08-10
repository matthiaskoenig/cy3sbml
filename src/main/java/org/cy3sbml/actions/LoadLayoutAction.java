package org.cy3sbml.actions;

import java.io.File;
import java.util.HashSet;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.util.swing.FileChooserFilter;
import org.cytoscape.util.swing.FileUtil;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.layout.LayoutTools;
import org.cy3sbml.gui.GUIConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Load node positions from file.
 */
public class LoadLayoutAction extends AbstractCyAction{
	private static final Logger logger = LoggerFactory.getLogger(LoadLayoutAction.class);
	private static final long serialVersionUID = 1L;
	
	private ServiceAdapter adapter;

    /** Constructor. */
	public LoadLayoutAction(ServiceAdapter adapter){
		super(LoadLayoutAction.class.getSimpleName());
		this.adapter = adapter;
		
		ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_LOADLAYOUT));
		putValue(LARGE_ICON_KEY, icon);
		
		this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_LOADLAYOUT);
		setToolbarGravity(GUIConstants.GRAVITY_LOADLAYOUT);
		insertSeparatorAfter = true;
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
	    org.cytoscape.util.swing.FileUtil fileUtil = adapter.fileUtil;
		File xmlFile = fileUtil.getFile(frame, "Load Layout for current CyNetworkView", FileUtil.LOAD, filters);
		
		LayoutTools layoutTools = new LayoutTools(adapter);
		layoutTools.loadLayoutOfCurrentViewFromFile(xmlFile);
	}
}
