package org.cy3sbml.actions;

import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import javax.swing.ImageIcon;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.cofactors.CofactorViews;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;

/**
 * Toolbar action on the cofactor nodes of the current network, enabled for the networks
 * imported by cy3sbml.
 */
abstract class AbstractCofactorAction extends AbstractCyAction implements SetCurrentNetworkListener {
    private static final long serialVersionUID = 1L;

    protected final CofactorManager cofactorManager;
    private final ServiceAdapter adapter;
    private final SBMLManager sbmlManager;
    private final SBMLEnableTaskFactory enableTaskFactory;

    AbstractCofactorAction(
            ServiceAdapter adapter,
            SBMLManager sbmlManager,
            CofactorManager cofactorManager,
            SBMLEnableTaskFactory enableTaskFactory,
            String icon,
            String description,
            float gravity) {
        super(
                Map.of("title", description),
                adapter.cyApplicationManager,
                adapter.cyNetworkViewManager,
                enableTaskFactory);
        this.adapter = adapter;
        this.sbmlManager = sbmlManager;
        this.cofactorManager = cofactorManager;
        this.enableTaskFactory = enableTaskFactory;

        putValue(LARGE_ICON_KEY, new ImageIcon(getClass().getResource(icon)));
        putValue(SHORT_DESCRIPTION, description);
        setToolbarGravity(gravity);
        this.inToolBar = true;
        this.inMenuBar = false;
    }

    /**
     * Changes the cofactor nodes of the network.
     *
     * @param selected the selected nodes of the network
     */
    abstract void apply(CyNetwork network, CyNetworkView view, List<CyNode> selected);

    @Override
    public void actionPerformed(ActionEvent event) {
        CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
        CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
        if (network == null || sbmlManager.getSBMLDocument(network) == null) {
            return;
        }
        if (view != null && !view.getModel().equals(network)) {
            view = null;
        }
        apply(network, view, CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true));
        CofactorViews.update(adapter.visualMappingManager, view);
    }

    @Override
    public void handleEvent(SetCurrentNetworkEvent event) {
        CyNetwork network = event.getNetwork();
        enableTaskFactory.setReady(network != null && sbmlManager.getSBMLDocument(network) != null);
        updateEnableState();
    }
}
