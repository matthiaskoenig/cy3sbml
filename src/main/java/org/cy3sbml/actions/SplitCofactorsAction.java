package org.cy3sbml.actions;

import java.util.List;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.gui.GUIConstants;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkView;

/**
 * Splits the selected nodes of the current network into one clone per edge, e.g. the
 * nodes of cofactors like ATP or water that take part in many reactions.
 */
public final class SplitCofactorsAction extends AbstractCofactorAction {
    private static final long serialVersionUID = 1L;

    public SplitCofactorsAction(ServiceAdapter adapter, SBMLManager sbmlManager, CofactorManager cofactorManager) {
        super(
                adapter,
                sbmlManager,
                cofactorManager,
                new SBMLEnableTaskFactory(),
                GUIConstants.ICON_COFACTOR_SPLIT,
                GUIConstants.DESCRIPTION_COFACTOR_SPLIT,
                GUIConstants.GRAVITY_COFACTOR_SPLIT);
    }

    @Override
    void apply(CyNetwork network, CyNetworkView view, List<CyNode> selected) {
        cofactorManager.split(network, view, selected);
    }
}
