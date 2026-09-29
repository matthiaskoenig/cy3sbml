package org.cy3sbml.cofactors;

import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyle;

/** The view update after a split or merge of cofactor nodes. */
public final class CofactorViews {

    private CofactorViews() {}

    /**
     * Applies the style of the view again, so the new nodes and edges get the mappings of the
     * style, and updates the view. Does nothing without view.
     */
    public static void update(VisualMappingManager visualMappingManager, CyNetworkView view) {
        if (view == null) {
            return;
        }
        VisualStyle style = visualMappingManager.getVisualStyle(view);
        if (style != null) {
            style.apply(view);
        }
        view.updateView();
    }
}
