package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;

import javax.swing.ImageIcon;

import org.cy3sbml.SBMLManager;
import org.cy3sbml.gui.GUIConstants;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.sbml.jsbml.SBMLDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;

import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.cofactors.CofactorManager;

/**
 * Action to handle cofactor nodes.
 * <p>
 * Handle clones of network nodes for better visualization.
 * This allows to clone a selected set of nodes.
 */
public class CofactorAction extends AbstractCyAction implements SetCurrentNetworkListener {
    private static final Logger logger = LoggerFactory.getLogger(CofactorAction.class);
    private static final long serialVersionUID = 1L;
    private ServiceAdapter adapter;
    private SBMLEnableTaskFactory sbmlEnableTaskFactory;

    /**
     * Constructor.
     */
    public CofactorAction(Map<String, String> configProps, ServiceAdapter adapter, SBMLEnableTaskFactory sbmlEnableTaskFactory) {
        super(configProps, adapter.cyApplicationManager, adapter.cyNetworkViewManager, sbmlEnableTaskFactory);
        this.adapter = adapter;
        this.sbmlEnableTaskFactory = sbmlEnableTaskFactory;

        ImageIcon icon = new ImageIcon(getClass().getResource(GUIConstants.ICON_COFACTOR));
        putValue(LARGE_ICON_KEY, icon);

        this.putValue(SHORT_DESCRIPTION, GUIConstants.DESCRIPTION_COFACTOR);
        setToolbarGravity(GUIConstants.GRAVITY_LOCATION);
    }

    public boolean isInToolBar() {
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        logger.info("actionPerformed()");
        runCofactorAction(adapter);
    }

    /**
     * Performs the cofactor action.
     * @param adapter
     */
    public static void runCofactorAction(ServiceAdapter adapter){
        // Get the current network via the service adapter
        CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
        CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
        if (network == null || view == null) {
            return;
        }

        // Selected nodes are inputs to the cofactor handling
        // TODO: implement different inputs (from SBML, from List, cofactor files)
        //  (this has to be processed in the network generation)
        List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);

        CofactorManager cofactorManager = CofactorManager.getInstance();
        cofactorManager.processNodes(network, nodes);

        view.updateView();
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent event) {
        CyNetwork network = event.getNetwork();
        boolean ready = false;
        if (network != null){
            SBMLDocument doc = SBMLManager.getInstance().getSBMLDocument(network);
            if (doc != null) {
                ready = true;
            }
        }
        sbmlEnableTaskFactory.setReady(ready);
        updateEnableState();
    }

}

