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
import org.cy3sbml.util.NetworkUtil;

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
    private static final Logger logger = LoggerFactory.getLogger(SBMLReader.class);
	private final ServiceAdapter adapter;


	/** Constructor. */
	public SBMLReader(CyFileFilter filter, ServiceAdapter adapter){
		super(filter);
		this.adapter = adapter;
	}

	@Override
	public TaskIterator createTaskIterator(InputStream is, String inputName) {		
		logger.debug("createTaskIterator: input stream name: " + inputName);
		try {
			return new TaskIterator(
				new SBMLReaderTask(IOUtil.copyInputStream(is), inputName,
                        adapter.cyNetworkFactory,
                        adapter.cyNetworkViewFactory,
						adapter.visualMappingManager,
                        adapter.cy3sbmlProperties)
			);
		} catch (IOException e) {
			throw new SBMLReaderError(e.toString());
		}
	}

    /**
     * Handles the cytoscape specific actions when adding views.
     * This consists of applying the layout to the view.
     */
	@Override
	public void handleEvent(NetworkViewAddedEvent e) {
		logger.debug("NetworkViewAddedEvent in SBMLReader");
        try {
            final CyNetworkView view = e.getNetworkView();
            final CyNetwork network = view.getModel();
            final CyLayoutAlgorithmManager cyLayoutAlgorithmManager = adapter.cyLayoutAlgorithmManager;

            if(NetworkUtil.isSBMLNetwork(network)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        layout(view, cyLayoutAlgorithmManager);
                    }
                });
            }
        } catch(Throwable t){
            t.printStackTrace();
        }
	}

	/**
     * Applies SBML layout to view.
     */
	private void layout(CyNetworkView view, CyLayoutAlgorithmManager cyLayoutAlgorithmManager) {
		CyLayoutAlgorithm layout = cyLayoutAlgorithmManager.getLayout(SBML.SBML_LAYOUT);
		if (layout == null) {
			layout = adapter.cyLayoutAlgorithmManager.getLayout(CyLayoutAlgorithmManager.DEFAULT_LAYOUT_NAME);
			logger.warn(String.format("'{}' layout not found; will use the default one.", SBML.SBML_LAYOUT));
		}
		adapter.taskManager.execute(layout.createTaskIterator(view,
                layout.getDefaultLayoutContext(), CyLayoutAlgorithm.ALL_NODE_VIEWS,""));
	}
}
