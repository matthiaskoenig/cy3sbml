package org.cy3sbml.gui;

import java.util.ArrayList;
import java.util.List;
import org.cy3sbml.SBMLManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.cytoscape.view.model.CyNetworkView;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders one previously-resolved render target (see {@link #resolveTarget}) into the
 * panel.
 * <p>
 * Whether this is worth running at all - is a render for this exact target already
 * pending or running - is decided by the submitter, {@code WebViewPanel.
 * updateInformation}, via {@code LatestTaskExecutor.submit(target, this)}: the target
 * itself is the task's key, so a resubmission of the same target is coalesced away by
 * the executor before this ever runs again. Re-rendering a target that already
 * completed earlier is acceptable: the underlying OLS/UniProt/ChEBI/document lookups are
 * themselves cached, so it is cheap.
 */
public class PanelUpdater implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PanelUpdater.class);

    static final String TEXT_NO_SBML_NODE = "<h2>No information</h2>"
            + "<p>The node has no SBML element, for example a base unit like "
            + "<code>dimensionless</code> or <code>mole</code>, which is not defined in the model.</p>";

    private static final String TEXT_LOAD_WEBSERVICE = "<h2>Web Services</h2>"
            + "<p>" + GUIConstants.ICON_SPINNER + "\n"
            + "Loading information from WebServices ...</p>";

    static final String TEXT_NO_SBML =
            "<h2>No information</h2>" + "<p>No SBMLDocument associated with the current network.</p>";

    private final InfoPanel panel;
    private final Object target;
    private final SBaseHTMLFactory htmlFactory;

    public PanelUpdater(InfoPanel panel, Object target, SBaseHTMLFactory htmlFactory) {
        this.panel = panel;
        this.target = target;
        this.htmlFactory = htmlFactory;
    }

    /**
     * Resolves what should be rendered for the given network's current selection: if
     * nothing (that has a mapped {@code SBase}) is selected the {@code SBMLDocument}, or
     * the {@code Model} of the network collection if it is not the main model of the
     * document (e.g. a comp model definition); the selected node's {@code SBase}; or one
     * of the two fixed "no information" messages. Pure and side-effect-free (queries {@code sbmlManager} but
     * changes nothing), so a caller can use it as the key for {@code LatestTaskExecutor.
     * submit} before submitting a render for it.
     */
    // reason: the model of the collection is compared by identity, JSBML's equals compares the content
    @SuppressWarnings("ReferenceEquality")
    static Object resolveTarget(CyNetwork network, SBMLManager sbmlManager) {
        SBMLDocument document = sbmlManager.getCurrentSBMLDocument();
        if (document == null) {
            logger.debug("No SBMLDocument for current network: {}", network);
            return TEXT_NO_SBML;
        }

        List<Long> suids = new ArrayList<>();
        for (CyNode n : CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true)) {
            suids.add(n.getSUID());
        }
        List<String> cyIds = sbmlManager.getCyIdsFromSUIDs(suids);
        if (cyIds.isEmpty()) {
            // the model of the collection, e.g. a comp model definition, with its document
            Model model = sbmlManager.getCurrentModel();
            return model != null && model != document.getModel() ? model : document;
        }
        SBase sbase = sbmlManager.getSBaseByCyId(cyIds.get(0));
        return sbase != null ? sbase : TEXT_NO_SBML_NODE;
    }

    /** True if the view is a view of the current network, which may be null. */
    static boolean isViewOfCurrentNetwork(CyNetworkView view, CyNetwork currentNetwork) {
        return currentNetwork != null && currentNetwork.equals(view.getModel());
    }

    /**
     * Renders {@link #target}.
     */
    @Override
    public void run() {
        // SBMLDocument is itself an SBase in JSBML, so it must be checked first: a
        // selected document (nothing mapped is selected) is shown directly, without the
        // "Loading..." placeholder that is only for a genuinely selected SBase.
        if (target instanceof SBMLDocument || target instanceof Model) {
            // the document or the model of the collection, no node is a model
            panel.showSBaseInfo(target);
        } else if (target instanceof SBase sbase) {
            panel.setText(htmlFactory.createHTMLText(TEXT_LOAD_WEBSERVICE));
            panel.showSBaseInfo(sbase);
        } else {
            // one of the two fixed messages (a String)
            panel.setText(htmlFactory.createHTMLText((String) target));
        }
    }
}
