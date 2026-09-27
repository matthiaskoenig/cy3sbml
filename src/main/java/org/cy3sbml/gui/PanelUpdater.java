package org.cy3sbml.gui;

import java.util.ArrayList;
import java.util.List;
import org.cy3sbml.SBMLManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTableUtil;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Renders one previously-resolved render target (see {@link #resolveTarget}) into the
 * panel.
 * <p>
 * The coalescing decision (is this target worth rendering at all) is made by the
 * submitter, {@code WebViewPanel.updateInformation}, before this is even constructed;
 * this only marks the target as the render coalescer's completed one, via {@link
 * RenderCoalescer#markCompleted}, once (and only once) the render actually finished -
 * i.e. was not cancelled/interrupted partway through by a newer request. A cancelled
 * render therefore leaves the last completed target unchanged, so a later request for
 * the same target it failed to show still gets rendered rather than being wrongly
 * coalesced away as "already done".
 */
public class PanelUpdater implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PanelUpdater.class);

    static final String TEXT_NO_SBML_NODE = "<h2>No information</h2>"
            + "<p>No SBML object registered for node in ObjectMapper.</p>"
            + "<p>Some nodes do not have SBase objects associated, e.g. "
            + "the <code>AND</code> and <code>OR</code> nodes in the FBC package.</p>"
            + "<p>Other examples are the base units like <code>dimensionless</code> "
            + "or <code>mole</code> which are not part of the model.</p>";

    private static final String TEXT_LOAD_WEBSERVICE = "<h2>Web Services</h2>"
            + "<p><i class=\"fa fa-spinner fa-spin fa-3x fa-fw\"></i>\n"
            + "Loading information from WebServices ...</p>";

    static final String TEXT_NO_SBML =
            "<h2>No information</h2>" + "<p>No SBMLDocument associated with the current network.</p>";

    private final InfoPanel panel;
    private final Object target;
    private final SBaseHTMLFactory htmlFactory;
    private final RenderCoalescer renderCoalescer;

    public PanelUpdater(InfoPanel panel, Object target, SBaseHTMLFactory htmlFactory, RenderCoalescer renderCoalescer) {
        this.panel = panel;
        this.target = target;
        this.htmlFactory = htmlFactory;
        this.renderCoalescer = renderCoalescer;
    }

    /**
     * Resolves what should be rendered for the given network's current selection: the
     * model's {@code SBMLDocument} if nothing (that has a mapped {@code SBase}) is
     * selected, the selected node's {@code SBase}, or one of the two fixed "no
     * information" messages. Pure and side-effect-free (queries {@code sbmlManager} but
     * changes nothing), so a caller can use it to decide whether a render is actually
     * needed before submitting one.
     */
    static Object resolveTarget(CyNetwork network, SBMLManager sbmlManager) {
        SBMLDocument document = sbmlManager.getCurrentSBMLDocument();
        if (document == null) {
            logger.debug("No SBMLDocument for current network: " + network);
            return TEXT_NO_SBML;
        }

        List<Long> suids = new ArrayList<>();
        for (CyNode n : CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true)) {
            suids.add(n.getSUID());
        }
        List<String> cyIds = sbmlManager.getCyIdsFromSUIDs(suids);
        if (cyIds.isEmpty()) {
            return document;
        }
        SBase sbase = sbmlManager.getSBaseByCyId(cyIds.get(0));
        return sbase != null ? sbase : TEXT_NO_SBML_NODE;
    }

    /**
     * Renders {@link #target}, and marks it as the render coalescer's completed target
     * only if the render actually finished.
     */
    @Override
    public void run() {
        if (target instanceof SBase sbase) {
            panel.setText(htmlFactory.createHTMLText(TEXT_LOAD_WEBSERVICE));
            if (panel.showSBaseInfo(sbase)) {
                renderCoalescer.markCompleted(target);
            }
        } else if (target instanceof SBMLDocument document) {
            if (panel.showSBaseInfo(document)) {
                renderCoalescer.markCompleted(target);
            }
        } else {
            // one of the two fixed messages (a String); setText itself is never
            // interrupted, it only queues the actual display update
            panel.setText(htmlFactory.createHTMLText((String) target));
            renderCoalescer.markCompleted(target);
        }
    }
}
