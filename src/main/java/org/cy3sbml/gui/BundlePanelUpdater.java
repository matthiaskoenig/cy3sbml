package org.cy3sbml.gui;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.taverna.robundle.Bundle;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;

import org.cy3sbml.archive.ArchiveReaderTask;
import org.cy3sbml.archive.BundleAnnotation;
import org.cy3sbml.archive.BundleManager;
import org.cy3sbml.util.AttributeUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Updates the Panel information based on selection.
 */
public class BundlePanelUpdater implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(BundlePanelUpdater.class);

    private static final String TEMPLATE_NO_BUNDLE =
            "<h2>No information</h2>" +
            "<p>No bundle associated with current network.</p>";

    private BundlePanel panel;
	private CyNetwork network;

    public BundlePanelUpdater(BundlePanel panel, CyNetwork network) {
        this.panel = panel;
        this.network = network;
    }

    /**
     * Here the node information update is performed.
     * If multiple nodes are selected only the information for the first node is displayed.
     */
    public void run() {
        BundleManager bundleManager = BundleManager.getInstance();

        // selected nodes
        List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);

        // information for selected node(s)
        Bundle bundle = bundleManager.getCurrentBundle();
        if (bundle == null){
            System.out.println("No bundle for current network");
            panel.setText(TEMPLATE_NO_BUNDLE);
        } else {
            BundleAnnotation bundleAnnotation = bundleManager.getCurrentBundleAnnotation();
            Map<String, List<String>> pathAnnotations = bundleAnnotation.getPathAnnotations();

            // Get annotation for node (default to root)
            String path = "/";
            // TODO: get root node

            if (nodes != null && nodes.size() > 0) {
                CyNode n = nodes.get(0);
                path = AttributeUtil.get(network, n, ArchiveReaderTask.NODE_ATTR_PATH, String.class);
            }

            // create html

            String text = String.format("<h1><small>%s</small></h1>\n", path);
            // TODO: link to file, image, path, mediatype from node attributes
            if (pathAnnotations != null && pathAnnotations.containsKey(path)){
                for (String s : pathAnnotations.get(path)) {
                    text += String.format("<p><code>%s</code></p>",
                            StringEscapeUtils.escapeHtml(s));
                }
            }

            // add links
            text = text.replaceAll("&quot;(http://.*?)&quot;", "<a href=\"$1\">$1</a>");
            // pack in html
            text = SBaseHTMLFactory.createHTMLText(text);
            panel.setText(text);
        }

    }

}
