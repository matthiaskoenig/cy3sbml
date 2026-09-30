package org.cy3sbml.gui;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.actions.*;
import org.cy3sbml.biomodel.BiomodelsDialog;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.util.GUIUtil;
import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.view.model.CyNetworkView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles the links clicked in the cy3sbml panel.
 * <p>
 * The special cy3sbml URLs run the actions of the app (e.g. {@link #URL_HELP}), select
 * nodes ({@link #URL_SELECT_ID}), load example models ({@link #EXAMPLE_SBML}) or export
 * the shown information; any other web ({@code http}, {@code https}, {@code ftp}) or
 * {@code mailto} link opens in the system browser. Links with other schemes, e.g.
 * {@code file} links in the notes of a model, are ignored, so a model cannot open or run
 * local files.
 */
public class BrowserHyperlinkListener {
    private static final Logger logger = LoggerFactory.getLogger(BrowserHyperlinkListener.class);

    public static final String URL_CHANGESTATE = "https://cy3sbml-changestate";
    public static final String URL_IMPORT = "https://cy3sbml-import";

    public static final String URL_EXAMPLES = "https://cy3sbml-examples";
    public static final String URL_BIOMODELS = "https://cy3sbml-biomodels";
    public static final String URL_HELP = "https://cy3sbml-help";
    public static final String URL_COFACTOR_SPLIT = "https://cy3sbml-cofactor-split";
    public static final String URL_COFACTOR_MERGE = "https://cy3sbml-cofactor-merge";
    public static final String URL_LOADLAYOUT = "https://cy3sbml-layoutload";
    public static final String URL_SAVELAYOUT = "https://cy3sbml-layoutsave";

    public static final String URL_SBMLFILE = "http://sbml-file";
    public static final String URL_HTML_SBASE = "http://html-sbase";

    public static final String URL_SELECT_METAID = "http://select-metaid/";
    public static final String URL_SELECT_ID = "http://select-id/";
    // the node of a comp reference in the network of its model: <model id>/<metaid>
    public static final String URL_SELECT_TARGET = "http://select-target/";

    /** The URL schemes of the links opened in the system browser. */
    static final Set<String> EXTERNAL_SCHEMES = Set.of("http", "https", "ftp", "mailto");

    public static final Map<String, String> EXAMPLE_SBML;
    public static final Set<String> URLS_ACTION;

    // Set all the URL actions
    static {
        HashMap<String, String> map = new HashMap<>();
        map.put("https://cy3sbml-glucose", "/models/Koenig_glucose_v1.xml");
        map.put("https://cy3sbml-glimepiride", "/models/glimepiride_body_flat.xml");
        map.put("https://cy3sbml-glimepiride-liver", "/models/glimepiride_liver.xml");
        map.put("https://cy3sbml-glimepiride-kidney", "/models/glimepiride_kidney.xml");
        map.put("https://cy3sbml-glimepiride-intestine", "/models/glimepiride_intestine.xml");
        map.put("https://cy3sbml-Faure2006", "/models/Faure2006_MammalianCellCycle.sbml");
        map.put("https://cy3sbml-pancreas", "/models/Maheshvare2023_pancreas_glucose.xml");
        map.put("https://cy3sbml-rivaroxaban", "/models/rivaroxaban_body_flat.xml");
        map.put("https://cy3sbml-rivaroxaban-liver", "/models/rivaroxaban_liver.xml");
        map.put("https://cy3sbml-rivaroxaban-kidney", "/models/rivaroxaban_kidney.xml");
        map.put("https://cy3sbml-rivaroxaban-intestine", "/models/rivaroxaban_intestine.xml");
        map.put("https://cy3sbml-HepatoNet1", "/models/HepatoNet1.xml");
        map.put("https://cy3sbml-e_coli_core", "/models/e_coli_core.xml");
        map.put("https://cy3sbml-iAB_RBC_283", "/models/iAB_RBC_283.xml");
        map.put("https://cy3sbml-iIT341", "/models/iIT341.xml");
        map.put("https://cy3sbml-RECON1", "/models/RECON1.xml");
        map.put("https://cy3sbml-BIOMD0000000001", "/models/BIOMD0000000001.xml");
        map.put("https://cy3sbml-BIOMD0000000012", "/models/BIOMD0000000012.xml");
        map.put("https://cy3sbml-BIOMD0000000016", "/models/BIOMD0000000016.xml");
        map.put("https://cy3sbml-BIOMD0000000084", "/models/BIOMD0000000084.xml");
        map.put("https://cy3sbml-hsa04360", "/models/hsa04360.xml");
        EXAMPLE_SBML = Collections.unmodifiableMap(map);

        Set<String> set = new HashSet<>();

        set.add(URL_CHANGESTATE);
        set.add(URL_IMPORT);
        set.add(URL_EXAMPLES);
        set.add(URL_BIOMODELS);
        set.add(URL_HELP);
        set.add(URL_COFACTOR_SPLIT);
        set.add(URL_COFACTOR_MERGE);
        set.add(URL_SAVELAYOUT);
        set.add(URL_LOADLAYOUT);
        URLS_ACTION = Collections.unmodifiableSet(set);
    }

    private final ServiceAdapter adapter;
    private final WebViewPanel webViewPanel;
    private final SBMLManager sbmlManager;
    private final CofactorManager cofactorManager;
    private final BiomodelsDialog biomodelsDialog;
    private final Executor dispatch;
    private final Consumer<String> externalBrowser;

    /**
     * Creates the listener, which runs the actions of the links on the Swing event dispatch
     * thread and opens the web links in the system browser.
     */
    public BrowserHyperlinkListener(
            ServiceAdapter adapter,
            WebViewPanel webViewPanel,
            SBMLManager sbmlManager,
            CofactorManager cofactorManager,
            BiomodelsDialog biomodelsDialog) {
        this(
                adapter,
                webViewPanel,
                sbmlManager,
                cofactorManager,
                biomodelsDialog,
                SwingUtilities::invokeLater,
                GUIUtil::openURLinExternalBrowser);
    }

    /**
     * @param dispatch runs the action of a clicked link; the Swing event dispatch thread
     *     outside of tests
     * @param externalBrowser opens a web link in the system browser
     */
    BrowserHyperlinkListener(
            ServiceAdapter adapter,
            WebViewPanel webViewPanel,
            SBMLManager sbmlManager,
            CofactorManager cofactorManager,
            BiomodelsDialog biomodelsDialog,
            Executor dispatch,
            Consumer<String> externalBrowser) {
        this.adapter = adapter;
        this.webViewPanel = webViewPanel;
        this.sbmlManager = sbmlManager;
        this.cofactorManager = cofactorManager;
        this.biomodelsDialog = biomodelsDialog;
        this.dispatch = dispatch;
        this.externalBrowser = externalBrowser;
    }

    /**
     * Called on the JavaFX thread when a link is clicked. The action of the link opens Swing
     * dialogs and changes Cytoscape networks and tables, so it runs on the Swing event
     * dispatch thread.
     *
     * @param url the absolute URL of the link
     */
    public void linkActivated(URL url) {
        logger.debug("Link activated: {}", url);
        String s = url.toString();
        dispatch.execute(() -> processURL(s));
    }

    /**
     * True for the locations of the pages the panel loads: the HTML text (an empty
     * location) and the bundled pages in the app directory.
     */
    static boolean isPanelLocation(String location, File appDirectory) {
        if (location == null || location.isEmpty() || location.equals("about:blank")) {
            return true;
        }
        try {
            URI uri = new URI(location);
            return "file".equalsIgnoreCase(uri.getScheme())
                    && Path.of(uri)
                            .normalize()
                            .startsWith(appDirectory.toPath().toAbsolutePath().normalize());
        } catch (URISyntaxException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * The absolute URL of a link: the href itself if it is an absolute URL, else the href
     * resolved against the base URI of the link.
     *
     * @param baseUri the base URI of the link, may be null
     * @param href the href attribute of the link
     * @return the URL, or null if there is none
     */
    static URL resolve(String baseUri, String href) {
        try {
            URI uri = URI.create(href.strip());
            if (!uri.isAbsolute() && baseUri != null) {
                uri = URI.create(baseUri).resolve(uri);
            }
            return uri.isAbsolute() ? uri.toURL() : null;
        } catch (IllegalArgumentException | MalformedURLException e) {
            return null;
        }
    }

    /**
     * Runs what the link of the URL stands for: an action, a selection, an example model,
     * an export or a web link.
     */
    private void processURL(String s) {
        if (URLS_ACTION.contains(s)) {
            createAction(s).actionPerformed(null);
        } else if (s.startsWith(URL_SELECT_TARGET)) {
            selectTarget(s.substring(URL_SELECT_TARGET.length()));
        } else if (s.startsWith(URL_SELECT_METAID) || s.startsWith(URL_SELECT_ID)) {
            // Only select if current network exists
            CyNetwork network = adapter.cyApplicationManager.getCurrentNetwork();
            if (network != null) {
                String[] tokens = s.split("/", -1);
                String identifier = tokens[tokens.length - 1];
                if (s.startsWith(URL_SELECT_ID)) {
                    NetworkUtil.selectById(network, identifier);
                } else if (s.startsWith(URL_SELECT_METAID)) {
                    NetworkUtil.selectByMetaId(network, identifier);
                }
            }
        }

        // Example networks
        else if (EXAMPLE_SBML.containsKey(s)) {
            String resource = EXAMPLE_SBML.get(s);
            logger.info("Loading: {}", s);
            GUIUtil.loadExampleFromResource(adapter, resource);
        }

        // SBML file
        else if (s.equals(URL_SBMLFILE)) {
            GUIUtil.openCurrentSBMLInBrowser(sbmlManager);
        }

        // SBase HTML
        else if (s.equals(URL_HTML_SBASE)) {
            GUIUtil.openSBaseHTMLInBrowser(webViewPanel.getHtml());
        }

        // web links
        else if (isExternalLink(s)) {
            externalBrowser.accept(s);
        } else {
            logger.warn("Link not opened, only web and mailto links open in the browser: {}", s);
        }
    }

    /** The action of an action URL, one of {@link #URLS_ACTION}. */
    private AbstractCyAction createAction(String url) {
        return switch (url) {
            case URL_COFACTOR_SPLIT -> new SplitCofactorsAction(adapter, sbmlManager, cofactorManager);
            case URL_COFACTOR_MERGE -> new MergeCofactorsAction(adapter, sbmlManager, cofactorManager);
            case URL_CHANGESTATE -> new ChangeStateAction(webViewPanel);
            case URL_IMPORT -> new ImportAction(adapter);
            case URL_EXAMPLES -> new ExamplesAction(webViewPanel);
            case URL_BIOMODELS -> new BiomodelsAction(biomodelsDialog);
            case URL_HELP -> new HelpAction(webViewPanel);
            case URL_SAVELAYOUT -> new SaveLayoutAction(adapter);
            case URL_LOADLAYOUT -> new LoadLayoutAction(adapter);
            default -> throw new IllegalArgumentException("No action for " + url);
        };
    }

    /** Whether the link opens in the system browser: a web or mailto link. */
    static boolean isExternalLink(String url) {
        int colon = url.indexOf(':');
        return colon > 0 && EXTERNAL_SCHEMES.contains(url.substring(0, colon).toLowerCase(Locale.ROOT));
    }

    /**
     * Makes the network of the collection current and selects the node of the element,
     * for a link {@code <root network SUID>/<metaid>} to the target of a comp reference,
     * which is usually in another network collection. Without metaid, only the base
     * network of the collection is made current.
     */
    private void selectTarget(String path) {
        int slash = path.indexOf('/');
        String cyId = slash < 0 ? "" : path.substring(slash + 1);
        Long rootSUID;
        try {
            rootSUID = Long.valueOf(slash < 0 ? path : path.substring(0, slash));
        } catch (NumberFormatException e) {
            logger.warn("Invalid target link: {}", path);
            return;
        }
        Set<CyNetwork> networks = adapter.cyNetworkManager.getNetworkSet();
        Optional<CyNetwork> target = cyId.isEmpty()
                ? NetworkUtil.findBaseNetwork(networks, rootSUID)
                : NetworkUtil.findTargetNetwork(networks, rootSUID, cyId);
        if (target.isEmpty()) {
            logger.warn("The network of the link '{}' is not open anymore.", path);
            return;
        }
        CyNetwork network = target.get();
        adapter.cyApplicationManager.setCurrentNetwork(network);
        Collection<CyNetworkView> views = adapter.cyNetworkViewManager.getNetworkViews(network);
        if (views != null && !views.isEmpty()) {
            adapter.cyApplicationManager.setCurrentNetworkView(views.iterator().next());
        }
        if (!cyId.isEmpty()) {
            NetworkUtil.selectByMetaId(network, cyId);
        }
    }
}
