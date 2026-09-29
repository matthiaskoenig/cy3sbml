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
 * Merges the clones of split nodes back into the nodes: the nodes of the selected
 * clones, or all split nodes of the current network if no clone is selected.
 */
public final class MergeCofactorsAction extends AbstractCofactorAction {
    private static final long serialVersionUID = 1L;

    public MergeCofactorsAction(ServiceAdapter adapter, SBMLManager sbmlManager, CofactorManager cofactorManager) {
        super(
                adapter,
                sbmlManager,
                cofactorManager,
                new SBMLEnableTaskFactory(),
                GUIConstants.ICON_COFACTOR_MERGE,
                GUIConstants.DESCRIPTION_COFACTOR_MERGE,
                GUIConstants.GRAVITY_COFACTOR_MERGE);
    }

    @Override
    void apply(CyNetwork network, CyNetworkView view, List<CyNode> selected) {
        List<CyNode> clones = selected.stream()
                .filter(node -> cofactorManager.isClone(network, node))
                .toList();
        if (clones.isEmpty()) {
            cofactorManager.mergeAll(network, view);
        } else {
            cofactorManager.merge(network, view, clones);
        }
    }
}
