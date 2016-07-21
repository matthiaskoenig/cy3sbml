package org.cy3sbml;

import java.io.IOException;
import java.io.InputStream;

import javax.swing.SwingUtilities;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskIterator;

import org.cy3sbml.util.IOUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SBMLReader class
 * 
 * Manages the reading of SBMLDocuments within
 * the SBMLReaderTasks and creates networks and views for the given
 * SBMLDocument.
 */
public class SBMLReader extends AbstractInputStreamTaskFactory implements NetworkViewAddedListener {
	public static final String SBML_LAYOUT = "force-directed";

    private static final Logger logger = LoggerFactory.getLogger(SBMLReader.class);
	private final ServiceAdapter cyServices;

	/** Constructor. */
	public SBMLReader(CyFileFilter filter, ServiceAdapter cyServices){
		super(filter);
		this.cyServices = cyServices;
	}
	
	@Override
	public TaskIterator createTaskIterator(InputStream is, String inputName) {		
		logger.debug("createTaskIterator: input stream name: " + inputName);
		try {
			return new TaskIterator(
				new SBMLReaderTask(IOUtil.copyInputStream(is), inputName, cyServices.cyNetworkFactory, cyServices.cyNetworkViewFactory,
						cyServices.cyNetworkViewManager)
			);
		} catch (IOException e) {
			throw new SBMLReaderError(e.toString());
		}
	}

    /**
     * Handles the cytoscape specific actions when adding views.
     * This are
     * - layout of networks
     * - setting of visual styles
     * Actions related to the ResultsPanel are handled in the ResultsPanel.
     */
	@Override
	public void handleEvent(NetworkViewAddedEvent e) {
		logger.debug("NetworkViewAddedEvent in SBMLReader");
		try {
			final CyNetworkView view = e.getNetworkView();
			final CyNetwork network = view.getModel();

            // check if SBML network
			if(SBMLManager.isSBMLNetwork(network)) {

				//apply style and layout
                // TODO: define constant
				String styleName = (String) cyServices.cy3sbmlProperty("cy3sbml.visualStyle");
				VisualMappingManager vmm = cyServices.visualMappingManager;
				VisualStyle style = SBMLStyleManager.getVisualStyleByName(vmm, styleName);
				VisualStyle currentStyle = vmm.getVisualStyle(view);
				logger.debug("Current VisualStyle: " + currentStyle.getTitle());
				logger.debug("VisualStyle to set: " + style.getTitle());
				
				if(style != null && !(style.getTitle()).equals(currentStyle.getTitle())){
					final VisualStyle vs = style;			
					//apply style and layout			
					SwingUtilities.invokeLater(new Runnable() {
							public void run() {			
								cyServices.visualMappingManager.setVisualStyle(vs, view);
								vs.apply(view);		
								layout(view);
								view.updateView();
								logger.info("Style set and updated: " + vs.getTitle());
							}
					});
				}
			}
		
		} catch(Throwable t){
			t.printStackTrace();
		}		
	}
	
	/** Apply layout to view. */
	private void layout(CyNetworkView view) {
		CyLayoutAlgorithm layout = cyServices.cyLayoutAlgorithmManager.getLayout(SBML_LAYOUT);
		if (layout == null) {
			layout = cyServices.cyLayoutAlgorithmManager.getLayout(CyLayoutAlgorithmManager.DEFAULT_LAYOUT_NAME);
			logger.warn(String.format("'{}' layout not found; will use the default one.", SBML_LAYOUT));
		}
		cyServices.taskManager.execute(layout.createTaskIterator(view,
                layout.getDefaultLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS,""));
	}
}
