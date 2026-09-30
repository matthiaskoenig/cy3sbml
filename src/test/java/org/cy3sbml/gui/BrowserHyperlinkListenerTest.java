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
import org.cy3sbml.SBML;
import org.cy3sbml.ServiceAdapter;
import org.cy3sbml.TestUtils;
import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BrowserHyperlinkListenerTest {

    private static final File APP_DIRECTORY = new File("/opt/cy3sbml/app");

    /** The panel shows its HTML text and the bundled pages in the app directory. */
    @ParameterizedTest
    @ValueSource(
            strings = {"", "about:blank", "file:///opt/cy3sbml/app/gui/help.html", "file:/opt/cy3sbml/app/gui/x.html"})
    void panelLocations(String location) {
        assertTrue(BrowserHyperlinkListener.isPanelLocation(location, APP_DIRECTORY), location);
    }

    /** Any other page, e.g. of a meta refresh in the notes of a model, is not shown. */
    @ParameterizedTest
    @ValueSource(
            strings = {
                "https://cy3sbml-help/",
                "https://example.org/",
                "data:text/html,x",
                "file:///etc/passwd",
                "file:///opt/cy3sbml/app/../../../etc/passwd",
                "file:///opt/cy3sbml/application/x.html",
                "file://server/share/x.html",
                "not a uri"
            })
    void otherLocations(String location) {
        assertFalse(BrowserHyperlinkListener.isPanelLocation(location, APP_DIRECTORY), location);
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
}
