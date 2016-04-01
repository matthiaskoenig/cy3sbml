package org.cy3sbml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import javax.swing.SwingUtilities;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyTable;
import org.cytoscape.view.layout.CyLayoutAlgorithm;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;
import org.cytoscape.work.TaskIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SBMLReader extends AbstractInputStreamTaskFactory implements NetworkViewAddedListener {
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
				new SBMLReaderTask(copy(is), inputName, cyServices.cyNetworkFactory, cyServices.cyNetworkViewFactory,
						cyServices.cyNetworkViewManager)
			);
		} catch (IOException e) {
			throw new SBMLReaderError(e.toString());
		}
	}

	@Override
	public void handleEvent(NetworkViewAddedEvent e) {
		logger.debug("NetworkViewAddedEvent in SBMLReader");
		try {
			// always apply the style and layout to new BioPAX views;
			// i.e., not only for the first time when one's created.
			final CyNetworkView view = e.getNetworkView();
			final CyNetwork network = view.getModel();	
			if(isSBMLNetwork(network)) {
				
				/*
				// TODO: Store some kind information for the network & use to switch visual styles
				String kind = cyNetwork.getRow(cyNetwork).get(SBML.SBML_NETWORK, String.class);
				if ("DEFAULT".equals(kind))
					style = visualStyleUtil.getBioPaxVisualStyle();
				else if ("SIF".equals(kind))
					style = visualStyleUtil.getBinarySifVisualStyle();
				*/
				
				//apply style and layout			
				String styleName = (String) cyServices.cy3sbmlProperty("cy3sbml.visualStyle");
				VisualStyle style = getVisualStyleByName(styleName);
				VisualStyle currentStyle = cyServices.visualMappingManager.getVisualStyle(view);
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
	
	private void layout(CyNetworkView view) {
		// do layout
		CyLayoutAlgorithm layout = cyServices.cyLayoutAlgorithmManager.getLayout("force-directed");
		if (layout == null) {
			layout = cyServices.cyLayoutAlgorithmManager.getLayout(CyLayoutAlgorithmManager.DEFAULT_LAYOUT_NAME);
			logger.warn("'force-directed' layout not found; will use the default one.");
		}
		cyServices.taskManager.execute(layout.createTaskIterator(view, 
				layout.getDefaultLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS,""));
	}	
	
	private boolean isSBMLNetwork(CyNetwork cyNetwork) {
		//true if the attribute column exists
		CyTable cyTable = cyNetwork.getDefaultNetworkTable();
		return cyTable.getColumn(SBML.NETWORKTYPE_ATTR) != null;
	}
	
	private VisualStyle getVisualStyleByName(String styleName){
		VisualMappingManager vmm = cyServices.visualMappingManager;
		Set<VisualStyle> styles = vmm.getAllVisualStyles();
		// another ugly fix because styles can not be get by name
		for (VisualStyle style: styles){
			if (style.getTitle().equals(styleName)){
				logger.debug("style found in VisualStyles: " + styleName + " == " + style.getTitle());
				return style;
			}
		}
		logger.warn("style [" + styleName +"] not in VisualStyles, default style used.");
		return vmm.getDefaultVisualStyle();
	}
	
		
	private InputStream copy(InputStream is) throws IOException {
		ByteArrayOutputStream copy = new ByteArrayOutputStream();
		int chunk = 0;
		byte[] data = new byte[1024*1024];
		while((-1 != (chunk = is.read(data)))) {
			copy.write(data, 0, chunk);
		}
		is.close();
		return new ByteArrayInputStream( copy.toByteArray() );
	}	
}
