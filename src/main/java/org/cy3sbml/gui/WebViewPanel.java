package org.cy3sbml.gui;

import java.awt.*;
import java.util.Set;
import java.util.function.Consumer;
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
 * The cy3sbml panel in the Cytoscape results panel (east): a JavaFX {@link Browser} that
 * shows the SBML information of the current network and the selected node, or the help
 * and examples pages.
 * <p>
 * The HTML is rendered on the {@link LatestTaskExecutor} (it may wait for web services)
 * and published to the browser through the {@link PageLoader}.
 */
public final class WebViewPanel extends JFXPanel
        implements CytoPanelComponent2,
                InfoPanel,
                RowsSetListener,
                SetCurrentNetworkListener,
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
    private static final int PREFERRED_WIDTH = 400;
    private static final int PREFERRED_HEIGHT = 600;
    private final PageLoader pages = new PageLoader();
    private volatile String html;

    /**
     * Creates the panel, which shows the help page until the first render.
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

        // shown once the browser exists, unless a render is requested before
        setHelp();
        JFXPanel fxPanel = this;
        Platform.runLater(() -> pages.attach(initFX(fxPanel)));
    }

    /**
     * Initialize the JavaFX components.
     * This creates the browser and adds it to the scene.
     */
    private Browser initFX(JFXPanel fxPanel) {
        // This method is invoked on the JavaFX thread
        Browser browser = new Browser(
                adapter.cy3sbmlDirectory,
                new BrowserHyperlinkListener(adapter, this, sbmlManager, cofactorManager, biomodelsDialog));
        Scene scene = new Scene(browser, PREFERRED_WIDTH, PREFERRED_HEIGHT);
        fxPanel.setScene(scene);
        // necessary to support the detached mode
        Platform.setImplicitExit(false);
        return browser;
    }

    /** The HTML text shown last, or null if no rendered text has been shown yet. */
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

    /** Whether the results panel is shown, so the information is updated. */
    public boolean isActive() {
        return (cytoPanelEast.getState() != CytoPanelState.HIDE);
    }

    // //////////////// ACTIVATION HANDLING

    /**
     * Shows the results panel with this panel selected, and the information of the current
     * selection, which was not updated while the panel was hidden.
     */
    public void activate() {
        if (cytoPanelEast.getState() == CytoPanelState.HIDE) {
            cytoPanelEast.setState(CytoPanelState.DOCK);
        }
        select();
        updateInformation();
    }

    /** Hides the results panel, unless it has other components. */
    public void deactivate() {
        if (cytoPanelEast.getCytoPanelComponentCount() == 1) {
            cytoPanelEast.setState(CytoPanelState.HIDE);
        }
    }

    /** Hides the panel if it is shown, else shows it. */
    public void changeState() {
        if (isActive()) {
            deactivate();
        } else {
            activate();
        }
    }

    /** Selects this panel in the results panel. */
    public void select() {
        int index = cytoPanelEast.indexOfComponent(this);
        if (index == -1) {
            return;
        }
        if (cytoPanelEast.getSelectedIndex() != index) {
            cytoPanelEast.setSelectedIndex(index);
        }
    }

    // //////////////// INFORMATION DISPLAY

    /**
     * Shows the static help page.
     * <p>
     * Submitted on the {@link #renderExecutor} under a fresh key (never equal to any
     * other key, including a previous call's), so it is never coalesced away and always
     * cancels whatever render is still pending or running (e.g. a slow web-service lookup
     * for a previous selection): that render can no longer overwrite the help page once
     * it is requested, and the two run in the order they were requested. The page is
     * loaded via {@link #publish}, the same publication path the SBase renders use.
     */
    public void setHelp() {
        renderExecutor.submit(
                new Object(), () -> publish(p -> p.loadPageFromResource(GUIConstants.HTML_HELP_RESOURCE)));
    }

    /**
     * Shows the static examples page. Submitted on the {@link #renderExecutor} under a
     * fresh key, same as {@link #setHelp}, so an in-flight render cannot overwrite it.
     */
    public void setExamples() {
        renderExecutor.submit(
                new Object(), () -> publish(p -> p.loadPageFromResource(GUIConstants.HTML_EXAMPLE_RESOURCE)));
    }

    /**
     * Shows the given HTML text.
     * <p>
     * Must be called from a render task running on the {@link #renderExecutor}; the text
     * is only shown if that task is still the current render (see {@link #publish}).
     */
    @Override
    public void setText(String text) {
        publish(p -> {
            html = text;
            p.loadText(text);
        });
    }

    /**
     * Publishes a page to the browser, but only if the calling render task is still the
     * current one ({@link LatestTaskExecutor#publishIfCurrent}): a superseded render that
     * already passed its last interrupt check is dropped here, so it can never overwrite
     * the result of a newer render or the help/examples page.
     * <p>
     * An accepted publication goes to the {@link PageLoader} while the executor's lock is
     * held, so the pages reach the browser in the order their publications were accepted,
     * and the page accepted last is the page shown.
     */
    private void publish(Consumer<PageLoader> publication) {
        renderExecutor.publishIfCurrent(() -> publication.accept(pages));
    }

    /**
     * Shows the information of the SBase, see {@link #showSBaseInfo(Set)}.
     */
    @Override
    public void showSBaseInfo(Object obj) {
        showSBaseInfo(Set.of(obj));
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
     */
    @Override
    public void showSBaseInfo(Set<Object> objSet) {
        new SBaseHTMLThread(objSet, this, htmlFactory).run();
    }

    // //////////////// EVENT HANDLING

    /**
     * Shows the information of the selected node when the selection in the node table of the
     * current network changes. Rows are set very often, e.g. while a network is created or
     * laid out, so all other row changes are ignored right away.
     */
    @Override
    public void handleEvent(RowsSetEvent event) {
        CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
        if ((network != null && !event.getSource().equals(network.getDefaultNodeTable()))
                || !event.containsColumn(CyNetwork.SELECTED)) {
            return;
        }
        updateInformation();
    }

    /**
     * Makes the SBML document of the new current network the current document and shows its
     * information.
     */
    @Override
    public void handleEvent(SetCurrentNetworkEvent event) {
        CyNetwork network = event.getNetwork();
        // network changed, update of the current SBMLDocument and bundle
        sbmlManager.updateCurrent(network);
        updateInformation();
    }

    /** Shows the information of the current network once it has a view. */
    @Override
    public void handleEvent(NetworkViewAddedEvent event) {
        updateInformation();
    }

    /**
     * Shows the help page when the view of the current network is closed; the view of
     * another network leaves the information of the current network.
     */
    @Override
    public void handleEvent(NetworkViewAboutToBeDestroyedEvent event) {
        if (PanelUpdater.isViewOfCurrentNetwork(
                event.getNetworkView(), adapter.cyApplicationManager.getCurrentNetwork())) {
            setHelp();
        }
    }

    /**
     * Updates the panel information on the render executor.
     * <p>
     * Resolves the render target (see {@link PanelUpdater#resolveTarget}) and submits it
     * as the render executor's key: several Cytoscape events fired while loading one
     * model can resolve to the same target (e.g. a model's subnetworks, taken current in
     * turn, share one {@code SBMLDocument}), and {@code LatestTaskExecutor} coalesces a
     * resubmission of the target it is already rendering rather than cancelling and
     * restarting it, so one load renders once.
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
        logger.debug("current view: {}", view);
        logger.debug("current network: {}", network);
        if (network == null || view == null) {
            return;
        }

        Object target = PanelUpdater.resolveTarget(network, sbmlManager);
        renderExecutor.submit(target, new PanelUpdater(this, target, htmlFactory));
    }

    /**
     * Stops the render executor. Called from {@code CyActivator.shutDown} so the
     * daemon render thread and any in-flight web-service call are stopped on app shutdown.
     */
    public void close() {
        renderExecutor.close();
    }
}
