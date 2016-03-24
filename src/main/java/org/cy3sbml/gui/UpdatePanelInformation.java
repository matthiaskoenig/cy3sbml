package org.cy3sbml.gui;

import java.util.LinkedList;
import java.util.List;

import org.cy3sbml.SBMLManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBMLDocument;

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
    
	/**
	 * Here the node information update is performed.
	 */
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
			List<String> selectedNSBIds = sbmlManager.getNSBIds(suids);
		
			if (selectedNSBIds.size() > 0){

				// TODO: How to handle multiple selections? Currently only first node in selection used
				String nsbId = selectedNSBIds.get(0);
				NamedSBase nsb = sbmlManager.getNamedSBaseById(nsbId);
				if (nsb != null){
					textPane.showNSBInfo(nsb);	
				} else {
					textPane.setText("No SBML object registered for node.");
				}
						
			} else {
				textPane.showNSBInfo(document.getModel());
			}
		} else {
			textPane.setText("No SBML associated with current network.");
		}
	}	
    
    
}
