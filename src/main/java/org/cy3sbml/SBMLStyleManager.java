package org.cy3sbml;

import java.io.InputStream;
import java.util.Set;

import org.cytoscape.session.events.SessionLoadedEvent;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.task.read.LoadVizmapFileTaskFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to manage the loading of the visual styles.
 * The style manager is a singleton class.
 */
public class SBMLStyleManager implements SessionLoadedListener{
	private static final Logger logger = LoggerFactory.getLogger(SBMLStyleManager.class);
	private static SBMLStyleManager uniqueInstance;
	
	private LoadVizmapFileTaskFactory loadVizmapFileTaskFactory;
	private VisualMappingManager vmm;
	
	public static final String STYLE_CY3SBML = "cy3sbml";
	public static final String STYLE_CY3SBML_DARK = "cy3sbml-dark";
	public static final String[] STYLES = {STYLE_CY3SBML, STYLE_CY3SBML_DARK};
	
	public static synchronized SBMLStyleManager getInstance(LoadVizmapFileTaskFactory loadVizmapFileTaskFactory,
															VisualMappingManager vmm){
		if (uniqueInstance == null){
			uniqueInstance = new SBMLStyleManager(loadVizmapFileTaskFactory, vmm);
		}
		return uniqueInstance;
	}
	
	/** Constructor. */
	private SBMLStyleManager(LoadVizmapFileTaskFactory loadVizmapFileTaskFactory, VisualMappingManager vmm){
		logger.debug("SBMLStyleManager created");
		this.loadVizmapFileTaskFactory = loadVizmapFileTaskFactory;
		this.vmm = vmm;
	}
	
	/** 
	 * Load the visual styles of the app. 
	 */
	public void loadStyles(){
		for (String styleName: STYLES){
			logger.info("Load visual style: " + styleName);
			String resource = String.format("/styles/%s.xml", styleName);
			InputStream styleStream = getClass().getResourceAsStream(resource);
			// Check if already existing
			VisualStyle style = getVisualStyleByName(vmm, styleName);
			if (styleName.equals(style.getTitle())){
				continue;
			} else {
				loadVizmapFileTaskFactory.loadStyles(styleStream);	
			}
		}
	}
	
	/** 
	 * Get the visual style by name.
	 * If no style for given styleName exists, the default style is returned.
	 * 
	 * This is a fix until the function is implemented on the vmm
	 * https://code.cytoscape.org/redmine/issues/2174
	 */
	public static VisualStyle getVisualStyleByName(VisualMappingManager vmm, String styleName){
		Set<VisualStyle> styles = vmm.getAllVisualStyles();
		for (VisualStyle style: styles){
			if (style.getTitle().equals(styleName)){
				return style;
			}
		}
		logger.debug("style [" + styleName +"] not in VisualStyles, default style used.");
		return vmm.getDefaultVisualStyle();
	}

	@Override
	public void handleEvent(SessionLoadedEvent e) {
		logger.debug("SessionAboutToBeLoadedEvent");
		loadStyles();
	}
	
}
