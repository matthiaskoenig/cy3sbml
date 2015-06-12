package org.cy3sbml;
import java.util.Set;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SBMLNetworkViewProcessing implements NetworkViewAddedListener{
	private static final Logger logger = LoggerFactory.getLogger(SBMLNetworkViewProcessing.class);
	private ServiceAdapter adapter;
	
	public SBMLNetworkViewProcessing(ServiceAdapter adapter) {
		logger.info("SBMLNetworkViewProcessing created");
		this.adapter = adapter;
	}
	
	private void doProcessing(final CyNetworkView view){
		logger.info("doProcessing ... ");
		
		// apply layout
		CyLayoutAlgorithm layout = adapter.cyLayoutAlgorithmManager.getLayout("force-directed");
		layout.createLayoutContext();
		TaskIterator layoutTaskIterator = layout.createTaskIterator(view, layout.createLayoutContext(),
																	CyLayoutAlgorithm.ALL_NODE_VIEWS, layout.getName());
		
		
		// We use the synchronous task manager otherwise the visual style and updateView()
		// may occur before the view is relayed out:
		//adapter.taskManager.execute(layoutTaskIterator);
		adapter.synchronousTaskManager.execute(layoutTaskIterator);
		
		
		// TODO: Select SBML Attributes in Data Panel
		// selectSBMLTableAttributes();
		
		// TODO: Arrange Windows and fit views (for all networks)
		//CyDesktopManager.arrangeFrames(CyDesktopManager.Arrange.GRID);
		
		// set visual style
		String styleName = (String) adapter.cy3sbmlProperty("cy3sbml.visualStyle");
		final VisualStyle style = getVisualStyleByName(styleName);
		
		adapter.visualMappingManager.setVisualStyle(style, view);
    	style.apply(view);
    	
    	view.updateView();
		view.fitContent();
		logger.info("doProcessing finished");
	}
	
	private VisualStyle getVisualStyleByName(String styleName){
		VisualMappingManager vmm = adapter.visualMappingManager;
		Set<VisualStyle> styles = vmm.getAllVisualStyles();
		for (VisualStyle style: styles){
			if (style.getTitle().equals(styleName)){
				return style;
			}
		}
		logger.warn("cy3sbml style not found in VisualStyles, default style used.");
		return vmm.getDefaultVisualStyle();
	}
	
	
	/*
	protected void applyLayout(CyNetwork network) {
		if (nodeIds.size() > LAYOUT_NODE_NUMBER){
			CySBML.LOGGER.info(String.format("More than %d nodes, no layout applied.", LAYOUT_NODE_NUMBER));
		} else { 
			CyLayoutAlgorithm layout = CyLayouts.getLayout("force-directed");
			CyNetworkView view = Cytoscape.getNetworkView(network.getIdentifier());
			view.applyLayout(layout);
		}
	}
	*/

	/*
	private void selectSBMLTableAttributes(){
		String[] nAtts = {CySBMLConstants.ATT_TYPE,
						  CySBMLConstants.ATT_NAME,
						  CySBMLConstants.ATT_COMPARTMENT,
						  CySBMLConstants.ATT_METAID,
						  CySBMLConstants.ATT_SBOTERM};
		
		String[] eAtts = {Semantics.INTERACTION,
						  CySBMLConstants.ATT_STOICHIOMETRY,
						  CySBMLConstants.ATT_METAID,
						  CySBMLConstants.ATT_SBOTERM};
		AttributeUtils.selectTableAttributes(Arrays.asList(nAtts), Arrays.asList(eAtts));
	}
	*/
	
	
	@Override
	public void handleEvent(NetworkViewAddedEvent event) {
		logger.info("NetworkViewAddedEvent");
		CyNetworkView view = event.getNetworkView();
		CyNetwork network = view.getModel();
		
		// Check if SBML network view
		if  (SBMLManager.getInstance().networkIsSBML(network)){
			doProcessing(view);
		}
	}
	
}
