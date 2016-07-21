package org.cy3sbml.gui;

import java.util.LinkedList;
import java.util.List;

import org.cy3sbml.SBMLManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;

public class UpdatePanelInformation implements Runnable {
	private ResultsPanel panel;
	private CyNetwork network;

    public UpdatePanelInformation(ResultsPanel panel, CyNetwork network) {
        this.panel = panel;
        this.network = network;
    }

    public void run() {
        updateInformation();
    }
    
	/** Here the node information update is performed. */
	public void updateInformation(){
		SBMLManager sbmlManager = SBMLManager.getInstance();
		JEditorPaneSBML textPane = panel.getTextPane();
		
		if (!panel.isActive()){
			textPane.setText("");
			return;
		}

		// selected node SUIDs
		LinkedList<Long> suids = new LinkedList<Long>();
		List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
		for (CyNode n : nodes){
			suids.add(n.getSUID());
		}
		// information for selected node(s)
		SBMLDocument document = sbmlManager.getCurrentSBMLDocument();
		if (document != null){
			List<String> objectIds = sbmlManager.getObjectIds(suids);
		
			if (objectIds.size() > 0){
				// TODO: How to handle multiple selections? 
				// Currently only first node in selection used
				String key = objectIds.get(0);
				SBase sbase = sbmlManager.getSBaseById(key);
				if (sbase != null){
					textPane.showSBaseInfo(sbase);	
				} else {
					textPane.setText("No SBML object registered for node.");
				}
						
			} else {
				textPane.showSBaseInfo(document.getModel());
			}
		} else {
			textPane.setText("No SBML associated with current network.");
		}
	}	
    
    
}
