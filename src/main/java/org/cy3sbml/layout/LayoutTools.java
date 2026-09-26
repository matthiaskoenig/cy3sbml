package org.cy3sbml.layout;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.cy3sbml.SBML;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.util.AttributeUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for working with layouts.
 * <p>
 * Saving and storing node positions to file.
 * Applying layouts from files to network views, ...
 */
public class LayoutTools {
    private static final Logger logger = LoggerFactory.getLogger(LayoutTools.class);

    private ServiceAdapter adapter;

    public LayoutTools(ServiceAdapter adapter) {
        this.adapter = adapter;
    }

    ///////////////////////////////////////////////////////////////////////////
    // SAVE LAYOUTS
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Save layout of current view in file.
     */
    public void saveLayoutOfCurrentViewInFile(File file) {
        CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
        if (view != null) {
            saveLayoutOfViewInFile(view, file);
        }
    }

    /**
     * Save layout of given view in file.
     */
    public void saveLayoutOfViewInFile(CyNetworkView view, File file) {
        CyNetwork network = view.getModel();

        List<CyNode> nodes = network.getNodeList();
        List<CyBoundingBox> boxes = new ArrayList<CyBoundingBox>();
        for (CyNode node : nodes) {
            View<CyNode> nodeView = view.getNodeView(node);
            // id column is used for mapping positions
            String nodeId = AttributeUtil.get(network, node, SBML.ATTR_ID, String.class);
            Double x = nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION);
            Double y = nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION);
            Double height = nodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT);
            Double width = nodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH);
            CyBoundingBox box = new CyBoundingBox(nodeId, x, y, height, width);
            boxes.add(box);
        }
        // Creates the XML file
        XMLInterface.writeXMLFileForLayout(file, boxes);
    }

    ///////////////////////////////////////////////////////////////////////////
    // LOAD LAYOUTS

    /// ////////////////////////////////////////////////////////////////////////
    public void loadLayoutOfCurrentViewFromFile(File file) {
        CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
        if (view != null) {
            loadLayoutForViewFromFile(view, file);
        }
    }

    public void loadLayoutForViewFromFile(CyNetworkView view, File file) {
        CyNetwork network = view.getModel();

        Map<String, CyBoundingBox> boxesMap = XMLInterface.readLayoutFromXML(file);
        if (boxesMap != null) {

            List<CyNode> nodes = network.getNodeList();
            for (CyNode node : nodes) {
                // if position is stored
                String nodeId = AttributeUtil.get(network, node, SBML.ATTR_ID, String.class);
                if (boxesMap.containsKey(nodeId)) {
                    CyBoundingBox box = boxesMap.get(nodeId);
                    View<CyNode> nodeView = view.getNodeView(node);
                    nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, box.getXpos());
                    nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, box.getYpos());
                }
            }
            view.updateView();
        } else {
            logger.warn("Layout information could not be loaded from file.");
        }
    }
}
