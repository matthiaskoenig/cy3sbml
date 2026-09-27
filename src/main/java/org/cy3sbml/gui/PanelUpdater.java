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
 * Updates the Panel information based on selection.
 */
public class PanelUpdater implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(PanelUpdater.class);

    private static final String TEXT_NO_SBML_NODE = "<h2>No information</h2>"
            + "<p>No SBML object registered for node in ObjectMapper.</p>"
            + "<p>Some nodes do not have SBase objects associated, e.g. "
            + "the <code>AND</code> and <code>OR</code> nodes in the FBC package.</p>"
            + "<p>Other examples are the base units like <code>dimensionless</code> "
            + "or <code>mole</code> which are not part of the model.</p>";

    private static final String TEXT_LOAD_WEBSERVICE = "<h2>Web Services</h2>"
            + "<p><i class=\"fa fa-spinner fa-spin fa-3x fa-fw\"></i>\n"
            + "Loading information from WebServices ...</p>";

    private static final String TEXT_NO_SBML =
            "<h2>No information</h2>" + "<p>No SBMLDocument associated with the current network.</p>";

    private final InfoPanel panel;
    private final CyNetwork network;
    private final SBMLManager sbmlManager;
    private final SBaseHTMLFactory htmlFactory;
    private final RenderCoalescer renderCoalescer;

    public PanelUpdater(
            InfoPanel panel,
            CyNetwork network,
            SBMLManager sbmlManager,
            SBaseHTMLFactory htmlFactory,
            RenderCoalescer renderCoalescer) {
        this.panel = panel;
        this.network = network;
        this.sbmlManager = sbmlManager;
        this.htmlFactory = htmlFactory;
        this.renderCoalescer = renderCoalescer;
    }

    /**
     * Here the node information update is performed.
     * Depending of the kind of network different updates are performed
     * If multiple nodes are selected only the information for the first node is displayed.
     * <p>
     * A request that resolves to the very same target (the same {@code SBMLDocument} or
     * {@code SBase} instance, or the same fixed message) as the last one actually
     * rendered is skipped via {@link #renderCoalescer}: several Cytoscape events fired
     * while loading a single model can resolve to the same thing to display (e.g. a
     * model's several subnetworks, taken current in turn, share one {@code SBMLDocument}),
     * and re-rendering it every time would re-run the OLS/UniProt/ChEBI lookups and flicker
     * the WebView for no visible change.
     */
    @Override
    public void run() {

        // associated SBMLDocument
        SBMLDocument document = sbmlManager.getCurrentSBMLDocument();

        if (document != null) {
            updateSBMLPanel(document);
        } else {
            logger.debug("No SBMLDocument for current network: " + network);
            if (renderCoalescer.accept(TEXT_NO_SBML)) {
                panel.setText(htmlFactory.createHTMLText(TEXT_NO_SBML));
            }
        }
    }

    /**
     * Updates the panel information for an SBMLDocument.
     */
    private void updateSBMLPanel(SBMLDocument document) {
        // selected node SUIDs
        List<Long> suids = new ArrayList<>();
        List<CyNode> nodes = CyTableUtil.getNodesInState(network, CyNetwork.SELECTED, true);
        for (CyNode n : nodes) {
            suids.add(n.getSUID());
        }
        // information for selected node(s)

        List<String> cyIds = sbmlManager.getCyIdsFromSUIDs(suids);

        if (cyIds.size() > 0) {
            // use first SBase
            String cyId = cyIds.get(0);
            SBase sbase = sbmlManager.getSBaseByCyId(cyId);

            if (sbase != null) {
                if (renderCoalescer.accept(sbase)) {
                    panel.setText(htmlFactory.createHTMLText(TEXT_LOAD_WEBSERVICE));
                    panel.showSBaseInfo(sbase);
                }
            } else if (renderCoalescer.accept(TEXT_NO_SBML_NODE)) {
                panel.setText(htmlFactory.createHTMLText(TEXT_NO_SBML_NODE));
            }
        } else if (renderCoalescer.accept(document)) {
            // show document/model information
            panel.showSBaseInfo(document);
        }
    }
}
