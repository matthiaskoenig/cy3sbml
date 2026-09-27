package org.cy3sbml.gui;

import java.awt.*;
import java.util.HashSet;
import java.util.Set;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javax.swing.*;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.biomodel.BiomodelsDialog;
import org.cy3sbml.cofactors.CofactorManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.*;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.events.NetworkAddedEvent;
import org.cytoscape.model.events.NetworkAddedListener;
import org.cytoscape.model.events.RowsSetEvent;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedEvent;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.NetworkViewAddedEvent;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebView panel based on javafx.
 * <p>
 * The panel is registered as Cytoscape Results Panel.
 * This panel is the main area for displaying SBML information for the
 * network.
 */
public final class WebViewPanel extends JFXPanel
        implements CytoPanelComponent2,
                InfoPanel,
                RowsSetListener,
                SetCurrentNetworkListener,
                NetworkAddedListener,
                NetworkViewAddedListener,
                NetworkViewAboutToBeDestroyedListener {
    private static final Logger logger = LoggerFactory.getLogger(WebViewPanel.class);
    private static final long serialVersionUID = 1L;

    private final ServiceAdapter adapter;
    private final SBMLManager sbmlManager;
    private final SBaseHTMLFactory htmlFactory;
    private final CofactorManager cofactorManager;
    private final BiomodelsDialog biomodelsDialog;
    private final CytoPanel cytoPanelEast;
    private final LatestTaskExecutor renderExecutor = new LatestTaskExecutor();
    private final RenderCoalescer renderCoalescer = new RenderCoalescer();
    private static final int PREFERRED_WIDTH = 400;
    private static final int PREFERRED_HEIGHT = 600;
    private Browser browser;
    private volatile String html;

    /**
     * Constructor
     */
    public WebViewPanel(
            ServiceAdapter adapter,
            SBMLManager sbmlManager,
            SBaseHTMLFactory htmlFactory,
            CofactorManager cofactorManager,
            BiomodelsDialog biomodelsDialog) {
        this.adapter = adapter;
        this.sbmlManager = sbmlManager;
        this.htmlFactory = htmlFactory;
        this.cofactorManager = cofactorManager;
        this.biomodelsDialog = biomodelsDialog;
        this.cytoPanelEast = adapter.cySwingApplication.getCytoPanel(CytoPanelName.EAST);

        setLayout(new BorderLayout());
        // the JavaFX scene is attached later, the docked panel takes its width from here
        setPreferredSize(new Dimension(PREFERRED_WIDTH, PREFERRED_HEIGHT));

        JFXPanel fxPanel = this;
        Platform.runLater(() -> {
            initFX(fxPanel);
            setHelp();
        });
    }

    /**
     * Initialize the JavaFX components.
     * This creates the browser and adds it to the scene.
     */
    private void initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        browser = new Browser(
                adapter.cy3sbmlDirectory,
                new BrowserHyperlinkListener(adapter, this, sbmlManager, cofactorManager, biomodelsDialog));
        Scene scene = new Scene(browser, PREFERRED_WIDTH, PREFERRED_HEIGHT);
        fxPanel.setScene(scene);
        // necessary to support the detached mode
        Platform.setImplicitExit(false);
    }

    public String getHtml() {
        return html;
    }

    @Override
    public CytoPanelName getCytoPanelName() {
        return CytoPanelName.EAST;
    }

    @Override
    public Component getComponent() {
        return this;
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getIdentifier() {
        return "cy3sbml";
    }

    @Override
    public String getTitle() {
        return "cy3sbml";
    }

    public boolean isActive() {
        return (cytoPanelEast.getState() != CytoPanelState.HIDE);
    }

    /// //////////////// ACTIVATION HANDLING ///////////////////////////////////

    public void activate() {
        // If the state of the cytoPanelWest is HIDE, show it
        if (cytoPanelEast.getState() == CytoPanelState.HIDE) {
            cytoPanelEast.setState(CytoPanelState.DOCK);
        }
        // Select panel
        select();
    }

    public void deactivate() {
        // Test if still other Components in Panel, otherwise hide the complete panel
        if (cytoPanelEast.getCytoPanelComponentCount() == 1) {
            cytoPanelEast.setState(CytoPanelState.HIDE);
        }
    }

    public void changeState() {
        if (isActive()) {
            deactivate();
        } else {
            activate();
        }
    }

    public void select() {
        int index = cytoPanelEast.indexOfComponent(this);
        if (index == -1) {
            return;
        }
        if (cytoPanelEast.getSelectedIndex() != index) {
            cytoPanelEast.setSelectedIndex(index);
        }
    }

    /// //////////////// INFORMATION DISPLAY ///////////////////////////////////

    /**
     * Shows the static help page.
     * <p>
     * Submitted on the {@link #renderExecutor} rather than called directly: submitting
     * cancels whatever render is still pending or running (e.g. a slow web-service lookup
     * for a previous selection), so that render can no longer overwrite the help page
     * once it is requested, and runs the two in the order they were requested.
     * <p>
     * The actual load, and with it the coalescer invalidation, happens inside the
     * submitted task (via {@link RenderCoalescer#resetThenRun}) rather than here on the
     * calling thread: invalidating here, before the task is even queued, would still
     * invalidate even if this particular help request then itself got superseded and
     * cancelled before ever running, which is the race this used to have.
     */
    public void setHelp() {
        renderExecutor.submit(() ->
                renderCoalescer.resetThenRun(() -> browser.loadPageFromResource(GUIConstants.HTML_HELP_RESOURCE)));
    }

    public void setExamples() {
        renderCoalescer.resetThenRun(() -> browser.loadPageFromResource(GUIConstants.HTML_EXAMPLE_RESOURCE));
    }

    /**
     * Set text.
     * <p>
     * The single gateway for loading text/HTML content into the browser (used directly
     * by {@link PanelUpdater} for its two fixed "no information" messages, and by {@link
     * SBaseHTMLThread} for the actual SBase/document content), so invalidating the
     * render coalescer here, via {@link RenderCoalescer#resetThenRun}, covers every
     * caller without any of them having to remember to do it themselves. A caller that
     * wants the target it is rendering tracked as completed (only {@link PanelUpdater})
     * calls {@link RenderCoalescer#markCompleted} itself immediately afterwards, once it
     * knows the render actually finished.
     */
    @Override
    public void setText(String text) {
        renderCoalescer.resetThenRun(() -> {
            html = text;
            // Necessary to use invokeLater to handle the Swing GUI update
            SwingUtilities.invokeLater(() -> browser.loadText(text));
        });
    }

    /**
     * Create information string for SBML Node and display.
     */
    @Override
    public boolean showSBaseInfo(Object obj) {
        Set<Object> objSet = new HashSet<>();
        objSet.add(obj);
        return showSBaseInfo(objSet);
    }

    /**
     * Display information for set of nodes.
     * <p>
     * Runs the HTML generation (the OLS/UniProt/ChEBI web-service lookups) inline, on
     * whatever thread is calling this. The only caller is {@link PanelUpdater}, itself
     * running as the single task {@link #updateInformation()} submits to the
     * {@link #renderExecutor} for the current selection. This must stay a plain call and
     * never become a nested {@code renderExecutor.submit(...)}: the executor cancels
     * whatever task it is currently running when a new one is submitted, so a nested
     * submit from inside a task that is itself about to be cancelled would race with, and
     * could cancel, a newer selection's already-submitted task instead of its own.
     * Keeping exactly one submit per selection is what makes that cancellation correct.
     *
     * @return true if the information was actually shown; false if this render was
     *     cancelled (interrupted) partway through.
     */
    @Override
    public boolean showSBaseInfo(Set<Object> objSet) {
        return new SBaseHTMLThread(objSet, this, htmlFactory).render();
    }

    @Override

    /////////////////// EVENT HANDLING ///////////////////////////////////

    /*
     * Handle node selection events in the table/network.
     * <p>
     * The RowsSet event is quit broad (happens a lot in network generation and layout, so
     * make sure to minimize the unnecessary action here.
     * I.e. only act on the Event if everything in the right state.
     * <p>
     * RowSetEvent:
     * An Event object generated when an event occurs to a RowSet object. A RowSetEvent object is
     * generated when a single row in a rowset is changed, the whole rowset is changed, or the
     * rowset cursor moves.
     * When an event occurs on a RowSet object, one of the RowSetListener methods will be sent
     * to all registered listeners to notify them of the event. An Event object is supplied to the
     * RowSetListener method so that the listener can use it to find out which RowSet object is
     * the source of the event.
     * <p>
     * http://chianti.ucsd.edu/cytoscape-3.2.1/API/org/cytoscape/model/package-summary.html
     */
    public void handleEvent(RowsSetEvent event) {
        CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
        if ((network != null && !event.getSource().equals(network.getDefaultNodeTable()))
                || !event.containsColumn(CyNetwork.SELECTED)) {
            return;
        }
        updateInformation();
    }

    /**
     * Listening to changes in Networks and NetworkViews.
     * When must the SBMLDocument store be updated.
     * - NetworkViewAddedEvent
     * - NetworkViewDestroyedEvent
     * <p>
     * An event indicating that a network view has been set to current.
     * SetCurrentNetworkViewEvent
     * <p>
     * An event signaling that the a network has been set to current.
     * SetCurrentNetworkEvent
     */
    @Override
    public void handleEvent(SetCurrentNetworkEvent event) {
        CyNetwork network = event.getNetwork();
        // network changed, update of the current SBMLDocument and bundle
        sbmlManager.updateCurrent(network);
        updateInformation();
    }

    /**
     * If networks are added check if they are subnetworks
     * of SBML networks and add the respective SBMLDocument
     * to them in the mapping.
     * Due to the mapping based on the RootNetworks sub-networks
     * automatically can use the mappings of the parent networks.
     */
    @Override
    public void handleEvent(NetworkAddedEvent event) {}

    @Override
    public void handleEvent(NetworkViewAddedEvent event) {
        updateInformation();
    }

    @Override
    public void handleEvent(NetworkViewAboutToBeDestroyedEvent event) {
        setHelp();
    }

    /**
     * Updates panel information within a separate thread.
     * <p>
     * Resolves the render target (see {@link PanelUpdater#resolveTarget}) and coalesces
     * here, before submitting anything: a target already pending/running or already
     * showing (the render coalescer's last completed one) is skipped outright, rather
     * than being submitted and left to the {@code PanelUpdater} task to discover it is
     * redundant. Deciding this only after a render for it has already started (and been
     * cancelled by this very submission) was the bug: the newer request would then see
     * the same target already "accepted" and coalesce itself away too, so neither render
     * ever finished.
     */
    public void updateInformation() {
        logger.debug("updateInformation()");

        // Only update if active
        if (!this.isActive()) {
            return;
        }

        // Only update if current network and view
        CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
        CyNetworkView view = adapter.cyApplicationManager.getCurrentNetworkView();
        logger.debug("current view: " + view);
        logger.debug("current network: " + network);
        if (network == null || view == null) {
            return;
        }

        Object target = PanelUpdater.resolveTarget(network, sbmlManager);
        if (renderCoalescer.isRedundant(target)) {
            logger.debug("Skipping redundant render request for {}", target);
            return;
        }

        // Update the information on the render executor; submitting cancels
        // whatever render is still pending or running for a previous selection.
        renderCoalescer.markPending(target);
        renderExecutor.submit(new PanelUpdater(this, target, htmlFactory, renderCoalescer));
    }

    /**
     * Stops the render executor. Called from {@code CyActivator.shutDown} so the
     * daemon render thread and any in-flight web-service call are stopped on app shutdown.
     */
    public void close() {
        renderExecutor.close();
    }
}
