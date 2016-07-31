package org.cy3sbml.gui;

import java.util.LinkedList;
import java.util.List;

import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;

import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;

import org.cy3sbml.SBMLManager;

/**
 * Updates the Panel information based on selection.
 */
public class UpdatePanel implements Runnable {

    private static final String TEMPLATE_NO_NODE = SBaseHTMLFactory.createHTMLText(
            "<h2>No information</h2>" +
            "<p>No SBML object registered for node in ObjectMapper.</p>" +
            "<p>Some nodes do not have SBase objects associated with them, e.g." +
            "the <code>AND</code> and <code>OR</code> nodes in the FBC package.</p>");

    private static final String TEMPLATE_NO_SBML = SBaseHTMLFactory.createHTMLText(
            "<h2>No information</h2>" +
            "<p>No SBML associated with current network.</p>" +
            "<p>Loading a SBML file will associate the respective SBML with the " +
            "CyNetwork and all CySubNetworks.</p>");

    private static final String TEMPLATE_LOAD_WEBSERVICE = SBaseHTMLFactory.createHTMLText(
            "<h2>Web Services</h2>" +
            "<p><i class=\"fa fa-spinner fa-spin fa-3x fa-fw\"></i>\n" +
            "Loading information from WebServices ...</p>");


    private SBMLPanel panel;
	private CyNetwork network;

    public UpdatePanel(SBMLPanel panel, CyNetwork network) {
        this.panel = panel;
        this.network = network;
    }

    /**
     * Here the node information update is performed.
     * If multiple nodes are selected only the information for the first node is displayed.
     */
    public void run() {
        SBMLManager sbmlManager = SBMLManager.getInstance();

        // selected node SUIDs
        LinkedList<Long> suids = new LinkedList<>();
        List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
        for (CyNode n : nodes){
            suids.add(n.getSUID());
        }
        // information for selected node(s)
        SBMLDocument document = sbmlManager.getCurrentSBMLDocument();
        if (document != null){
            List<String> objectIds = sbmlManager.getObjectIds(suids);
            if (objectIds.size() > 0){
                // use first node in selection
                String key = objectIds.get(0);
                SBase sbase = sbmlManager.getSBaseById(key);
                if (sbase != null){
                    panel.setText(TEMPLATE_LOAD_WEBSERVICE);
                    panel.showSBaseInfo(sbase);
                } else {
                    panel.setText(TEMPLATE_NO_NODE);
                }
            } else {
                // show model information
                panel.showSBaseInfo(document.getModel());
            }
        } else {
            panel.setText(TEMPLATE_NO_SBML);
        }
    }

}
