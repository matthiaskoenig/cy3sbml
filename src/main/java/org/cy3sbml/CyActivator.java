package org.cy3sbml;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.function.Supplier;
import org.cy3sbml.actions.*;
import org.cy3sbml.archive.ArchiveDirectories;
import org.cy3sbml.archive.CombineArchiveFileFilter;
import org.cy3sbml.archive.CombineArchiveReaderTaskFactory;
import org.cy3sbml.biomodel.BiomodelLoader;
import org.cy3sbml.biomodel.BiomodelsDialog;
import org.cy3sbml.biomodel.BiomodelsQuery;
import org.cy3sbml.biomodel.SearchBioModel;
import org.cy3sbml.chebi.ChebiAccess;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.commands.CommandServices;
import org.cy3sbml.commands.Commands;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.cy3sbml.gui.WebViewPanel;
import org.cy3sbml.layout.LayoutTools;
import org.cy3sbml.miriam.MiriamRegistry;
import org.cy3sbml.ols.OlsClient;
import org.cy3sbml.reader.JsbmlSetup;
import org.cy3sbml.styles.LayoutStyleFactory;
import org.cy3sbml.styles.StyleManager;
import org.cy3sbml.uniprot.UniprotAccess;
import org.cy3sbml.util.HttpJson;
import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkListener;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CySwingApplication;
import org.cytoscape.application.swing.CytoPanelComponent;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.group.CyGroupFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.events.NetworkAboutToBeDestroyedListener;
import org.cytoscape.model.events.RowsSetListener;
import org.cytoscape.property.CyProperty;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.session.events.SessionAboutToBeSavedListener;
import org.cytoscape.session.events.SessionLoadedListener;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.task.read.LoadNetworkURLTaskFactory;
import org.cytoscape.task.read.LoadVizmapFileTaskFactory;
import org.cytoscape.util.swing.FileUtil;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.view.layout.CyLayoutAlgorithmManager;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.events.NetworkViewAboutToBeDestroyedListener;
import org.cytoscape.view.model.events.NetworkViewAddedListener;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualStyleFactory;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.swing.DialogTaskManager;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point to cy3sbml.
 * <p>
 * The CyActivator registers the cy3sbml services with OSGI. This is the class
 * used for startup of the app by Cytoscape 3.
 * <p>
 * {@link #start} runs in two phases. {@link #startCore} registers the SBML
 * reader and the core services and listeners they depend on; nothing in that phase
 * touches an optional, cy3sbml-bundled resource (a template, image, or the JavaScript
 * extension jar), so it should never fail in a correctly packaged build. Everything
 * that does depend on such a resource - the JavaScript extension bundle, the GUI
 * resource extraction, and the WebView panel/actions/styles themselves - runs
 * afterwards, each guarded on its own, so that a missing or broken optional resource
 * degrades only that feature and never unregisters the reader. Without this, an
 * aborted activator leaves no cy3sbml reader registered at all, and Cytoscape's own
 * bundled SBML app (whose JSBML has no biojava) silently takes over `.xml` imports
 * and fails with {@code NoClassDefFoundError: org.sbml.jsbml.SBO}.
 */
public class CyActivator extends AbstractCyActivator {
    public static final String PROPERTIES_FILE = "cy3sbml.props";

    /** The system property with the log file, read by logback.xml. */
    private static final String LOGFILE_PROPERTY = "cy3sbml.logfile";

    private static final String APP_NAME = "cy3sbml";

    private static final String EXTENSION_BUNDLE_RESOURCE = "extension/org.cy3javascript.extension-0.0.1.jar";

    /**
     * Holds the logger, which is created on first use.
     * <p>
     * The first logger configures logback, which writes to the file in the
     * system property "cy3sbml.logfile". start sets the property before it logs,
     * so the logger must not be created when the class is loaded.
     */
    private static final class Log {
        private static final Logger logger = LoggerFactory.getLogger(CyActivator.class);
    }

    /**
     * The services built by {@link #startCore} that the optional GUI phase needs.
     */
    private record CoreServices(
            File appDirectory,
            ServiceAdapter adapter,
            SBMLManager sbmlManager,
            CofactorManager cofactorManager,
            BiomodelsQuery biomodelsQuery,
            BiomodelLoader biomodelLoader) {}

    private WebViewPanel webViewPanel;
    // the directories the COMBINE archives are unpacked into, deleted in shutDown
    private volatile ArchiveDirectories archiveDirectories;
    // the temporary files of the last saved session, deleted in shutDown
    private volatile SessionData sessionData;

    public CyActivator() {
        super();
    }

    /**
     * Starts the cy3sbml OSGI bundle.
     */
    @Override
    public void start(BundleContext bc) {
        // before anything can fail and log: the first logger reads the property
        File logFile = logFile(
                bc, () -> getService(bc, CyApplicationConfiguration.class).getConfigurationDirectoryLocation());
        logFile.getParentFile().mkdirs();
        System.setProperty(LOGFILE_PROPERTY, logFile.getAbsolutePath());

        CoreServices core;
        try {
            core = startCore(bc);
        } catch (Throwable e) {
            Log.logger.error("Could not start cy3sbml core services and SBML reader!", e);
            return;
        }

        // Everything below is optional GUI/extension/resource setup. Each step is
        // guarded on its own: a failure here disables or degrades that feature but
        // never touches the reader and listeners startCore already registered.
        startExtensionBundle(bc);
        startResourceExtraction(bc, core.appDirectory());
        startGui(bc, core);
    }

    /**
     * The log file of the app, {@code <configuration directory>/<bundle name>/<bundle name>-v<version>.log}.
     * Never throws, so the log file is known before anything can fail: without the
     * Cytoscape configuration directory, the default {@code ~/CytoscapeConfiguration} is
     * used, and without bundle information the app name.
     *
     * @param configurationDirectory the Cytoscape configuration directory
     */
    static File logFile(BundleContext bc, Supplier<File> configurationDirectory) {
        String name = APP_NAME;
        String info = APP_NAME;
        try {
            BundleInformation bundleInfo = new BundleInformation(bc);
            name = bundleInfo.getName();
            info = bundleInfo.getInfo();
        } catch (RuntimeException e) {
            // no bundle information, use the app name
        }
        File cyDirectory;
        try {
            cyDirectory = configurationDirectory.get();
        } catch (RuntimeException e) {
            cyDirectory = null;
        }
        if (cyDirectory == null) {
            cyDirectory = new File(System.getProperty("user.home"), "CytoscapeConfiguration");
        }
        return new File(new File(cyDirectory, name), info + ".log");
    }

    /**
     * Registers the SBML reader and the core Cytoscape services and listeners
     * they need. Nothing here depends on a cy3sbml-bundled resource (a template, image,
     * or jar shipped under {@code src/main/resources}), so a packaging mistake in those
     * resources cannot make this phase fail.
     */
    private CoreServices startCore(BundleContext bc) {
        BundleInformation bundleInfo = new BundleInformation(bc);

        // Default configuration directory used for all cy3sbml files
        // Used for retrieving
        CyApplicationConfiguration configuration = getService(bc, CyApplicationConfiguration.class);
        File cyDirectory = configuration.getConfigurationDirectoryLocation();
        File appDirectory = new File(cyDirectory, bundleInfo.getName());

        if (appDirectory.exists() == false) {
            appDirectory.mkdir();
        }

        Log.logger.info("----------------------------");
        Log.logger.info("Start {}", bundleInfo.getInfo());
        Log.logger.info("----------------------------");
        Log.logger.info("directory = {}", appDirectory.getAbsolutePath());
        Log.logger.info("logfile = {}", System.getProperty(LOGFILE_PROPERTY));

        // cy3sbml properties. Registering it under every interface it implements - notably
        // CyProperty, with the "cyPropertyName" service property below - is what makes
        // Cytoscape list the "cy3sbml" group in Edit > Preferences > Properties and expose
        // it at CyREST's /v1/properties/cy3sbml.props; SavePolicy.CONFIG_DIR (set in the
        // PropsReader constructor) is what makes edits there persist to cy3sbml.props.
        PropsReader propsReader = new PropsReader(bundleInfo.getName(), PROPERTIES_FILE);
        registerAllServices(bc, propsReader, cyPropertyServiceProperties(PROPERTIES_FILE));

        /* Get services */
        CySwingApplication cySwingApplication = getService(bc, CySwingApplication.class);

        CyApplicationManager cyApplicationManager = getService(bc, CyApplicationManager.class);
        CyNetworkManager cyNetworkManager = getService(bc, CyNetworkManager.class);
        CyNetworkViewManager cyNetworkViewManager = getService(bc, CyNetworkViewManager.class);
        VisualMappingManager visualMappingManager = getService(bc, VisualMappingManager.class);
        CyLayoutAlgorithmManager cyLayoutAlgorithmManager = getService(bc, CyLayoutAlgorithmManager.class);

        DialogTaskManager dialogTaskManager = getService(bc, DialogTaskManager.class);

        CyNetworkFactory cyNetworkFactory = getService(bc, CyNetworkFactory.class);
        CyNetworkViewFactory cyNetworkViewFactory = getService(bc, CyNetworkViewFactory.class);

        @SuppressWarnings("unchecked")
        CyProperty<Properties> cyProperties = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
        @SuppressWarnings("unchecked")
        CyProperty<Properties> appProperties = getService(bc, CyProperty.class, "(cyPropertyName=cy3sbml.props)");
        StreamUtil streamUtil = getService(bc, StreamUtil.class);
        OpenBrowser openBrowser = getService(bc, OpenBrowser.class);
        FileUtil fileUtil = getService(bc, FileUtil.class);

        CyGroupFactory cyGroupFactory = getService(bc, CyGroupFactory.class);

        LoadNetworkFileTaskFactory loadNetworkFileTaskFactory = getService(bc, LoadNetworkFileTaskFactory.class);

        // Use Cytoscape properties to set proxy for webservices
        new ConnectionProxy(cyProperties).setSystemProxyFromCyProperties();

        /* Create ServiceAdapter */
        ServiceAdapter adapter = new ServiceAdapter(
                cySwingApplication,
                cyApplicationManager,
                cyNetworkManager,
                cyNetworkViewManager,
                visualMappingManager,
                cyLayoutAlgorithmManager,
                dialogTaskManager,
                cyNetworkFactory,
                cyGroupFactory,
                cyNetworkViewFactory,
                appProperties,
                appDirectory,
                openBrowser,
                loadNetworkFileTaskFactory,
                fileUtil);

        // SBMLManager
        SBMLManager sbmlManager = new SBMLManager(cyApplicationManager);
        registerService(bc, sbmlManager, NetworkAboutToBeDestroyedListener.class, new Properties());
        // register services for other apps
        registerService(bc, sbmlManager, SBMLManager.class, new Properties());

        // Cofactor manager
        CofactorManager cofactorManager = new CofactorManager(sbmlManager, getService(bc, CyEventHelper.class));
        registerService(bc, cofactorManager, NetworkAboutToBeDestroyedListener.class, new Properties());

        // Session loading & saving
        sessionData = new SessionData(sbmlManager, cofactorManager);
        registerService(bc, sessionData, SessionAboutToBeSavedListener.class, new Properties());
        registerService(bc, sessionData, SessionLoadedListener.class, new Properties());

        // SBML file reader. Registered here, before any optional GUI/extension/resource
        // setup runs: Cytoscape only routes .xml imports to its own bundled SBML app
        // (whose jsbml has no biojava) when this factory is not registered.
        // before the reader is registered, i.e. before imports can run in parallel
        JsbmlSetup.initialize();
        SBMLFileFilter sbmlFilter = new SBMLFileFilter(streamUtil);
        SBMLReaderTaskFactory sbmlReaderTaskFactory = new SBMLReaderTaskFactory(sbmlFilter, adapter, sbmlManager);
        Properties sbmlReaderProps = new Properties();
        sbmlReaderProps.setProperty("readerDescription", "SBML file reader (cy3sbml)");
        sbmlReaderProps.setProperty("readerId", "cy3sbmlNetworkReader");
        registerAllServices(bc, sbmlReaderTaskFactory, sbmlReaderProps);

        // COMBINE archive (OMEX) reader: imports the SBML models of the archive (#116)
        archiveDirectories = ArchiveDirectories.temporary();
        CombineArchiveReaderTaskFactory archiveReaderTaskFactory = new CombineArchiveReaderTaskFactory(
                new CombineArchiveFileFilter(streamUtil), adapter, sbmlManager, archiveDirectories);
        Properties archiveReaderProps = new Properties();
        archiveReaderProps.setProperty("readerDescription", "COMBINE archive reader (cy3sbml)");
        archiveReaderProps.setProperty("readerId", "cy3sbmlArchiveReader");
        registerAllServices(bc, archiveReaderTaskFactory, archiveReaderProps);

        // BioModels search and download, used by the commands and the BioModels dialog
        BiomodelsQuery biomodelsQuery = BiomodelsQuery.createDefault();
        BiomodelLoader biomodelLoader = new BiomodelLoader(
                biomodelsQuery, appDirectory.toPath().resolve("biomodels"), loadNetworkFileTaskFactory);

        // automation commands (namespace cy3sbml, CyREST /v1/commands/cy3sbml/...), #18
        CommandServices commandServices = new CommandServices(
                cyApplicationManager,
                cyNetworkManager,
                cyNetworkViewManager,
                visualMappingManager,
                loadNetworkFileTaskFactory,
                getService(bc, LoadNetworkURLTaskFactory.class),
                getService(bc, SynchronousTaskManager.class),
                sbmlManager,
                cofactorManager,
                biomodelsQuery,
                biomodelLoader,
                new LayoutTools(adapter));
        for (Commands.Command command : Commands.create(commandServices)) {
            registerService(bc, command.factory(), TaskFactory.class, command.properties());
        }

        Log.logger.info("cy3sbml core services, SBML reader and commands registered");

        return new CoreServices(appDirectory, adapter, sbmlManager, cofactorManager, biomodelsQuery, biomodelLoader);
    }

    /**
     * Installs the bundled netscape.javascript extension bundle used by the WebView
     * panel's JavaScript integration. Guarded on its own: a missing or unreadable jar
     * (e.g. an {@code Include-Resource} packaging mistake) disables the extension but
     * must not stop the rest of startup, and above all not the SBML reader already
     * registered by {@link #startCore}.
     */
    private void startExtensionBundle(BundleContext bc) {
        Bundle bundle = bc.getBundle();
        URL jarUrl = findBundleResource(
                bundle,
                EXTENSION_BUNDLE_RESOURCE,
                "cy3sbml panel disabled: extension jar " + EXTENSION_BUNDLE_RESOURCE + " missing from the bundle");
        if (jarUrl == null) {
            return;
        }
        try (InputStream input = jarUrl.openStream()) {
            bc.installBundle(jarUrl.getPath(), input);
        } catch (Exception e) {
            Log.logger.error(
                    "cy3sbml panel disabled: could not install extension bundle {}", EXTENSION_BUNDLE_RESOURCE, e);
        }
    }

    /**
     * Looks up a resource entry in the given bundle by path.
     * <p>
     * Never throws: if the bundle does not contain the resource (for example, the
     * packaged jar is missing it because of an {@code Include-Resource} mistake), this
     * logs the given message as a single, clear ERROR naming the resource and the
     * consequence, and returns null instead of leaving the caller to dereference a
     * null URL.
     *
     * @param bundle the bundle to look the resource up in
     * @param resource the bundle-relative resource path, e.g. "extension/....jar"
     * @param logMessageIfMissing the ERROR message to log if the resource is missing
     * @return the resource URL, or null if the bundle does not contain it
     */
    static URL findBundleResource(Bundle bundle, String resource, String logMessageIfMissing) {
        URL url = bundle.getEntry(resource);
        if (url == null) {
            Log.logger.error(logMessageIfMissing);
        }
        return url;
    }

    /**
     * Builds the OSGi service properties a {@link org.cytoscape.property.CyProperty}
     * needs to be picked up by Cytoscape's Properties editor and by CyREST's
     * {@code /v1/properties} endpoint: the {@code cyPropertyName} service property,
     * set to the property file name (e.g. {@code "cy3sbml.props"}).
     */
    static Properties cyPropertyServiceProperties(String propertiesFile) {
        Properties serviceProps = new Properties();
        serviceProps.setProperty("cyPropertyName", propertiesFile);
        return serviceProps;
    }

    /**
     * Extracts the bundled GUI resources into the app directory, since
     * JavaFX cannot read {@code bundle:} URIs directly. Guarded on its own: if this fails,
     * the WebView panel may show incomplete pages, but the SBML reader already
     * registered by {@link #startCore} are unaffected.
     */
    private void startResourceExtraction(BundleContext bc, File appDirectory) {
        try {
            new ResourceExtractor(bc, appDirectory).extract();
        } catch (Throwable e) {
            Log.logger.error(
                    "cy3sbml panel may be incomplete: could not extract bundled GUI resources into {}",
                    appDirectory,
                    e);
        }
    }

    /**
     * Builds and registers the cy3sbml WebView panel, its actions, and the visual styles.
     * Guarded as a whole: if any part of this fails (e.g. a template or image the panel
     * needs is missing from the bundle), the panel and its actions are disabled, but the
     * SBML reader registered by {@link #startCore} keeps working.
     */
    private void startGui(BundleContext bc, CoreServices core) {
        try {
            ServiceAdapter adapter = core.adapter();
            SBMLManager sbmlManager = core.sbmlManager();
            CofactorManager cofactorManager = core.cofactorManager();
            File appDirectory = core.appDirectory();

            // HTML generation for SBases; the baseDir allows the dynamically generated HTML
            // to resolve the gui resources, the OLS, UniProt and ChEBI clients resolve
            // identifiers for display
            HttpJson httpJson = HttpJson.createDefault();
            // the bundled MIRIAM registry is used until the current one is downloaded,
            // the download must not block the bundle start
            MiriamRegistry miriamRegistry = MiriamRegistry.bundled();
            // the refresh logs its outcome, nothing waits for it
            var unused = miriamRegistry.refreshInBackground(MiriamRegistry.ONLINE_REGISTRY);
            SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory(
                    SBaseHTMLFactory.baseDirFromAppDir(appDirectory),
                    miriamRegistry,
                    new OlsClient(httpJson),
                    new UniprotAccess(httpJson),
                    new ChebiAccess(httpJson),
                    sbmlManager,
                    sbmlManager::getArchive);

            // load visual styles
            final String[] styles = {SBML.STYLE_CY3SBML, SBML.STYLE_CY3SBML_DARK};
            LoadVizmapFileTaskFactory loadVizmapFileTaskFactory = getService(bc, LoadVizmapFileTaskFactory.class);
            // the layout networks get a layout variant of every style (#71)
            LayoutStyleFactory layoutStyleFactory = new LayoutStyleFactory(
                    getService(bc, VisualStyleFactory.class),
                    getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=passthrough)"),
                    getService(bc, VisualMappingFunctionFactory.class, "(mapping.type=discrete)"));
            StyleManager styleManager = new StyleManager(
                    loadVizmapFileTaskFactory, adapter.visualMappingManager, styles, layoutStyleFactory);
            styleManager.loadStyles();
            registerService(bc, styleManager, SessionLoadedListener.class, new Properties());

            // BioModels search and import dialog
            BiomodelsQuery biomodelsQuery = core.biomodelsQuery();
            BiomodelLoader biomodelLoader = core.biomodelLoader();
            BiomodelsDialog biomodelsDialog = new BiomodelsDialog(
                    adapter.cySwingApplication.getJFrame(),
                    new SearchBioModel(biomodelsQuery),
                    adapter.openBrowser::openURL,
                    ids -> adapter.dialogTaskManager.execute(biomodelLoader.createTaskIterator(ids)));

            // panels
            webViewPanel = new WebViewPanel(adapter, sbmlManager, htmlFactory, cofactorManager, biomodelsDialog);
            registerService(bc, webViewPanel, CytoPanelComponent.class, new Properties());
            registerService(bc, webViewPanel, RowsSetListener.class, new Properties());
            registerService(bc, webViewPanel, SetCurrentNetworkListener.class, new Properties());
            registerService(bc, webViewPanel, NetworkViewAddedListener.class, new Properties());
            registerService(bc, webViewPanel, NetworkViewAboutToBeDestroyedListener.class, new Properties());
            // the panel renders the network events of a loaded session before the mapping is restored
            sbmlManager.addSessionRestoredListener(webViewPanel::updateInformation);

            // GUI frames

            // init actions [100 - 120]
            ChangeStateAction changeStateAction = new ChangeStateAction(webViewPanel);
            registerService(bc, changeStateAction, CyAction.class, new Properties());

            ImportAction importAction = new ImportAction(adapter);
            registerService(bc, importAction, CyAction.class, new Properties());

            ExamplesAction examplesAction = new ExamplesAction(webViewPanel);
            registerService(bc, examplesAction, CyAction.class, new Properties());

            SplitCofactorsAction splitCofactorsAction = new SplitCofactorsAction(adapter, sbmlManager, cofactorManager);
            registerService(bc, splitCofactorsAction, CyAction.class, new Properties());
            registerService(bc, splitCofactorsAction, SetCurrentNetworkListener.class, new Properties());

            MergeCofactorsAction mergeCofactorsAction = new MergeCofactorsAction(adapter, sbmlManager, cofactorManager);
            registerService(bc, mergeCofactorsAction, CyAction.class, new Properties());
            registerService(bc, mergeCofactorsAction, SetCurrentNetworkListener.class, new Properties());

            BiomodelsAction biomodelsAction = new BiomodelsAction(biomodelsDialog);
            registerService(bc, biomodelsAction, CyAction.class, new Properties());

            // init actions

            HelpAction helpAction = new HelpAction(webViewPanel);
            registerService(bc, helpAction, CyAction.class, new Properties());

            SaveLayoutAction saveLayoutAction = new SaveLayoutAction(adapter);
            registerService(bc, saveLayoutAction, CyAction.class, new Properties());

            LoadLayoutAction loadLayoutAction = new LoadLayoutAction(adapter);
            registerService(bc, loadLayoutAction, CyAction.class, new Properties());

            // cy3sbml panels
            webViewPanel.activate();

            Log.logger.info("----------------------------");

        } catch (Throwable e) {
            Log.logger.error("cy3sbml panel disabled: could not initialize the WebView panel and actions", e);
        }
    }

    /**
     * Stops the WebViewPanel's render executor (its daemon thread and any in-flight
     * web-service call) and deletes the temporary directories of the COMBINE archives and
     * of the last saved session, before AbstractCyActivator unregisters the OSGi services.
     */
    @Override
    public void shutDown() {
        if (webViewPanel != null) {
            webViewPanel.close();
        }
        if (archiveDirectories != null) {
            archiveDirectories.deleteAll();
        }
        if (sessionData != null) {
            sessionData.dispose();
        }
        super.shutDown();
    }
}
