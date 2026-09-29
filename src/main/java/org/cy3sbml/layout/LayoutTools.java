package org.cy3sbml.layout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.cy3sbml.SBML;
import org.cy3sbml.ServiceAdapter;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.View;
import org.cytoscape.view.presentation.property.BasicVisualLexicon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for working with layouts.
 * <p>
 * Saves the node positions of a network view to a file and applies them to a network view.
 * The nodes are matched by their {@code cyId}, which is unique for every node of an SBML
 * network and the same in every import of the model, also for the nodes of elements without
 * SBML id (rules, kinetic laws, units, math, ...). The positions of old layout files, which
 * have only the SBML id, are matched by the SBML id. The glyph nodes of a layout network, where
 * the glyphs of one element (aliases) have the same {@code cyId}, are matched by the glyph.
 */
public class LayoutTools {
    private static final Logger logger = LoggerFactory.getLogger(LayoutTools.class);

    private final ServiceAdapter adapter;

    public LayoutTools(ServiceAdapter adapter) {
        this.adapter = adapter;
    }

    // ------------------------------------------------------------
    // SAVE LAYOUTS
    // ------------------------------------------------------------

    /**
     * Save layout of current view in file.
     *
     * @throws IOException if the file could not be written
     */
    public void saveLayoutOfCurrentViewInFile(File file) throws IOException {
        CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
        if (view != null) {
            saveLayoutOfViewInFile(view, file);
        }
    }

    /**
     * Save layout of given view in file.
     * Nodes without cyId and SBML id (not created by cy3sbml) are not saved.
     *
     * @return the number of saved node positions
     * @throws IOException if the file could not be written
     */
    public int saveLayoutOfViewInFile(CyNetworkView view, File file) throws IOException {
        CyNetwork network = view.getModel();
        List<CyBoundingBox> boxes = new ArrayList<>();
        for (CyNode node : network.getNodeList()) {
            View<CyNode> nodeView = view.getNodeView(node);
            String cyId = attribute(network, node, SBML.ATTR_CYID);
            String sbmlId = attribute(network, node, SBML.ATTR_ID);
            if (nodeView == null || (cyId == null && sbmlId == null)) {
                continue;
            }
            boxes.add(new CyBoundingBox(
                    cyId,
                    sbmlId,
                    attribute(network, node, SBML.ATTR_LAYOUT_GLYPH),
                    nodeView.getVisualProperty(BasicVisualLexicon.NODE_X_LOCATION),
                    nodeView.getVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION),
                    nodeView.getVisualProperty(BasicVisualLexicon.NODE_HEIGHT),
                    nodeView.getVisualProperty(BasicVisualLexicon.NODE_WIDTH)));
        }
        XMLInterface.writeXMLFileForLayout(file, boxes);
        logger.info("Layout of {}/{} nodes saved in {}", boxes.size(), network.getNodeCount(), file);
        return boxes.size();
    }

    // ------------------------------------------------------------
    // LOAD LAYOUTS
    // ------------------------------------------------------------

    /**
     * Load layout of current view from file.
     */
    public void loadLayoutOfCurrentViewFromFile(File file) {
        CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
        if (view != null) {
            loadLayoutForViewFromFile(view, file);
        }
    }

    /**
     * Moves every node of the view with a position in the file to that position.
     *
     * @return number of positioned nodes
     */
    public int loadLayoutForViewFromFile(CyNetworkView view, File file) {
        List<CyBoundingBox> boxes = XMLInterface.readLayoutFromXML(file);
        Map<String, CyBoundingBox> byGlyph = new HashMap<>();
        Map<String, CyBoundingBox> byCyId = new HashMap<>();
        Map<String, CyBoundingBox> bySbmlId = new HashMap<>();
        for (CyBoundingBox box : boxes) {
            if (box.glyph() != null) {
                byGlyph.put(box.glyph(), box);
            } else if (box.cyId() != null) {
                byCyId.put(box.cyId(), box);
            } else {
                bySbmlId.put(box.sbmlId(), box);
            }
        }

        CyNetwork network = view.getModel();
        int positioned = 0;
        for (CyNode node : network.getNodeList()) {
            String glyph = attribute(network, node, SBML.ATTR_LAYOUT_GLYPH);
            CyBoundingBox box =
                    glyph != null ? byGlyph.get(glyph) : byCyId.get(attribute(network, node, SBML.ATTR_CYID));
            if (box == null && glyph == null) {
                box = bySbmlId.get(attribute(network, node, SBML.ATTR_ID));
            }
            View<CyNode> nodeView = view.getNodeView(node);
            if (box != null && nodeView != null) {
                nodeView.setVisualProperty(BasicVisualLexicon.NODE_X_LOCATION, box.x());
                nodeView.setVisualProperty(BasicVisualLexicon.NODE_Y_LOCATION, box.y());
                positioned++;
            }
        }
        view.updateView();
        logger.info("Layout of {}/{} nodes loaded from {}", positioned, network.getNodeCount(), file);
        return positioned;
    }

    /**
     * Returns the string attribute of the node. The attributes of group nodes are only set in
     * the root network, so they are read from there if the network has none.
     */
    private static String attribute(CyNetwork network, CyNode node, String column) {
        String value = value(network.getRow(node), column);
        if (value == null && network instanceof CySubNetwork subNetwork) {
            CyRootNetwork root = subNetwork.getRootNetwork();
            value = value(root.getRow(node), column);
        }
        return value;
    }

    /** Returns the value of the column, null if the column does not exist or the value is empty. */
    private static String value(CyRow row, String column) {
        if (row.getTable().getColumn(column) == null) {
            return null;
        }
        String value = row.get(column, String.class);
        // SBML L1 has no metaIds, so the cyIds of L1 models are empty
        return value == null || value.isEmpty() ? null : value;
    }
}
