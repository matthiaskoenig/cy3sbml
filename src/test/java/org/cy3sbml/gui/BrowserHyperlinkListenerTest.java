package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import org.cy3sbml.SBML;
import org.cy3sbml.SBMLManager;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.TestUtils;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.sbml4humans.Sbml4HumansClient;
import org.cy3sbml.sbml4humans.Sbml4HumansConsent;
import org.cy3sbml.sbml4humans.Sbml4HumansTask;
import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.property.CyProperty;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.swing.DialogTaskManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.sbml.jsbml.SBMLDocument;

class BrowserHyperlinkListenerTest {

    @TempDir
    static File appDirectory;

    /** The location of a file relative to the app directory, as {@link File#toURI()} gives it ({@code file:/}). */
    private static String fileUri(String path) {
        return new File(appDirectory, path).toURI().toString();
    }

    /** The location in the form WebKit reports ({@code file:///}). */
    private static String webKitUri(String path) {
        return fileUri(path).replaceFirst("^file:/+", "file:///");
    }

    /** The panel shows its HTML text and the bundled pages in the app directory. */
    @Test
    void panelLocations() {
        for (String location : List.of(
                "", "about:blank", fileUri("gui/help.html"), webKitUri("gui/help.html"), webKitUri("gui/x.html"))) {
            assertTrue(BrowserHyperlinkListener.isPanelLocation(location, appDirectory), location);
        }
    }

    /** Any other page, e.g. of a meta refresh in the notes of a model, is not shown. */
    @Test
    void otherLocations() {
        String sibling = new File(appDirectory.getParentFile(), appDirectory.getName() + "-other/x.html")
                .toURI()
                .toString();
        for (String location : List.of(
                "https://cy3sbml-help/",
                "https://example.org/",
                "data:text/html,x",
                webKitUri("../outside.html"),
                webKitUri("gui/../../outside.html"),
                sibling,
                "file://server/share/x.html",
                "not a uri")) {
            assertFalse(BrowserHyperlinkListener.isPanelLocation(location, appDirectory), location);
        }
    }

    @Test
    void linkActionsRunOnTheDispatchExecutorNotOnTheCallingThread() throws Exception {
        CyApplicationManager applicationManager = mock(CyApplicationManager.class);
        ServiceAdapter adapter = new ServiceAdapter(
                null, applicationManager, null, null, null, null, null, null, null, null, null, null, null, null, null);
        List<Runnable> dispatched = new ArrayList<>();
        BrowserHyperlinkListener listener =
                new BrowserHyperlinkListener(adapter, null, null, null, null, dispatched::add, url -> {});

        listener.linkActivated(
                URI.create(BrowserHyperlinkListener.URL_SELECT_ID + "glc").toURL());
        // the selection (a Cytoscape model edit) waits for the dispatch executor
        verifyNoInteractions(applicationManager);
        assertEquals(1, dispatched.size());

        dispatched.get(0).run();
        verify(applicationManager).getCurrentNetwork();
    }

    @Test
    void resolvesAbsoluteLinks() {
        assertEquals(
                BrowserHyperlinkListener.URL_HELP,
                BrowserHyperlinkListener.resolve("file:/tmp/cy3sbml/gui/info.html", BrowserHyperlinkListener.URL_HELP)
                        .toString());
    }

    @Test
    void resolvesRelativeLinksAgainstTheBaseUri() {
        assertEquals(
                "file:/tmp/cy3sbml/gui/help.html",
                BrowserHyperlinkListener.resolve("file:/tmp/cy3sbml/gui/info.html", "help.html")
                        .toString());
    }

    /**
     * Web and mailto links open in the system browser; links with other schemes, e.g. a
     * file link in the notes of a model, which could open or run a local file, do not.
     */
    @Test
    void onlyWebAndMailtoLinksOpenInTheSystemBrowser() throws Exception {
        List<String> opened = new ArrayList<>();
        BrowserHyperlinkListener listener =
                new BrowserHyperlinkListener(null, null, null, null, null, Runnable::run, opened::add);
        for (String link : List.of(
                "https://identifiers.org/chebi/CHEBI:17234",
                "http://bigg.ucsd.edu/",
                "HTTPS://example.org/",
                "ftp://ftp.ebi.ac.uk/",
                "mailto:someone@example.org",
                "file:///usr/bin/xterm",
                "file:/C:/Windows/System32/calc.exe",
                "jar:file:/tmp/model.jar!/x.html")) {
            listener.linkActivated(URI.create(link).toURL());
        }

        assertEquals(
                List.of(
                        "https://identifiers.org/chebi/CHEBI:17234",
                        "http://bigg.ucsd.edu/",
                        "https://example.org/",
                        "ftp://ftp.ebi.ac.uk/",
                        "mailto:someone@example.org"),
                opened);
    }

    @Test
    void linkWithoutUrlIsNotResolved() {
        // a relative link without a base URI, an unknown scheme,
        // an invalid href
        assertNull(BrowserHyperlinkListener.resolve(null, "relative"));
        assertNull(BrowserHyperlinkListener.resolve("about:blank", "relative"));
        assertNull(BrowserHyperlinkListener.resolve(null, "javascript:void(0)"));
        assertNull(BrowserHyperlinkListener.resolve(null, "http://a b"));
    }

    /** A comp target link makes the network of the target model current and selects the node. */
    @Test
    void targetLinkSelectsTheNodeInTheNetworkOfItsModel() throws Exception {
        CyNetwork[] networks = TestUtils.readNetwork("/models/comp/koenig-toymodel/toy_top_level.xml");
        CyNetwork fba = Arrays.stream(networks)
                .filter(n -> "toy_fba".equals(n.getRow(n).get(CyNetwork.NAME, String.class)))
                .findFirst()
                .orElseThrow();
        CyNode node = fba.getNodeList().get(0);
        String cyId = fba.getRow(node).get(SBML.ATTR_CYID, String.class);

        CyApplicationManager applicationManager = mock(CyApplicationManager.class);
        CyNetworkManager networkManager = mock(CyNetworkManager.class);
        when(networkManager.getNetworkSet()).thenReturn(new LinkedHashSet<>(Arrays.asList(networks)));
        CyNetworkViewManager viewManager = mock(CyNetworkViewManager.class);
        ServiceAdapter adapter = new ServiceAdapter(
                null,
                applicationManager,
                networkManager,
                viewManager,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        BrowserHyperlinkListener listener =
                new BrowserHyperlinkListener(adapter, null, null, null, null, Runnable::run, url -> {});

        listener.linkActivated(URI.create(
                        BrowserHyperlinkListener.URL_SELECT_TARGET + NetworkUtil.getRootNetworkSUID(fba) + "/" + cyId)
                .toURL());

        verify(applicationManager).setCurrentNetwork(fba);
        assertTrue(fba.getRow(node).get(CyNetwork.SELECTED, Boolean.class));
    }

    /** A listener for the current network with an SBML document, and its task manager. */
    private static final class Sbml4HumansSetup {
        final DialogTaskManager taskManager = mock(DialogTaskManager.class);
        final Properties properties = new Properties();
        final BrowserHyperlinkListener listener;
        int asked;

        @SuppressWarnings("unchecked")
        Sbml4HumansSetup(boolean answer) {
            CyNetwork network = new NetworkTestSupport().getNetwork();
            CyApplicationManager applicationManager = mock(CyApplicationManager.class);
            when(applicationManager.getCurrentNetwork()).thenReturn(network);
            CyProperty<Properties> property = mock(CyProperty.class);
            when(property.getProperties()).thenReturn(properties);
            ServiceAdapter adapter = new ServiceAdapter(
                    null,
                    applicationManager,
                    null,
                    null,
                    null,
                    null,
                    taskManager,
                    null,
                    null,
                    null,
                    property,
                    null,
                    null,
                    null,
                    null);
            SBMLManager sbmlManager = new SBMLManager(applicationManager);
            sbmlManager.addSBMLForNetwork(new SBMLDocument(3, 1), network, new One2ManyMapping<>());
            listener = new BrowserHyperlinkListener(
                    adapter, null, sbmlManager, null, null, Runnable::run, url -> {}, consent -> {
                        asked++;
                        return answer;
                    });
        }

        void click() throws Exception {
            listener.linkActivated(
                    URI.create(BrowserHyperlinkListener.URL_SBML4HUMANS).toURL());
        }
    }

    @Test
    void sbml4humansLinkAsksAndRunsTheTask() throws Exception {
        Sbml4HumansSetup setup = new Sbml4HumansSetup(true);

        setup.click();

        assertEquals(1, setup.asked);
        ArgumentCaptor<TaskIterator> tasks = ArgumentCaptor.forClass(TaskIterator.class);
        verify(setup.taskManager).execute(tasks.capture());
        assertTrue(tasks.getValue().next() instanceof Sbml4HumansTask);
    }

    @Test
    void sbml4humansLinkUploadsNothingWhenCancelled() throws Exception {
        Sbml4HumansSetup setup = new Sbml4HumansSetup(false);

        setup.click();

        assertEquals(1, setup.asked);
        verifyNoInteractions(setup.taskManager);
    }

    @Test
    void sbml4humansLinkDoesNotAskAfterConsent() throws Exception {
        Sbml4HumansSetup setup = new Sbml4HumansSetup(false);
        setup.properties.setProperty(Sbml4HumansConsent.PROPERTY_CONFIRMED, "true");

        setup.click();

        assertEquals(0, setup.asked);
        verify(setup.taskManager).execute(org.mockito.ArgumentMatchers.any(TaskIterator.class));
    }

    /** A malformed address of sbml4humans is the error of the task, not a click that does nothing. */
    @Test
    void sbml4humansLinkWithAMalformedAddressRunsTheTask() throws Exception {
        Sbml4HumansSetup setup = new Sbml4HumansSetup(true);
        setup.properties.setProperty(Sbml4HumansConsent.PROPERTY_CONFIRMED, "true");
        setup.properties.setProperty(Sbml4HumansClient.PROPERTY_URL, "http://exa mple.org");

        setup.click();

        verify(setup.taskManager).execute(org.mockito.ArgumentMatchers.any(TaskIterator.class));
    }

    @Test
    void consentMessageNamesWhatIsUploaded() {
        String message = BrowserHyperlinkListener.consentMessage("https://sbml4humans.de/");

        assertTrue(message.contains("external model"), message);
        assertTrue(message.contains("24 hours"), message);
        assertTrue(message.contains("https://sbml4humans.de/"), message);
    }
}
