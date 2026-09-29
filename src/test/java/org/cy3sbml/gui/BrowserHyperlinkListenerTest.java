package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.cy3sbml.ServiceAdapter;
import org.cytoscape.application.CyApplicationManager;
import org.junit.jupiter.api.Test;

class BrowserHyperlinkListenerTest {

    @Test
    void linkActionsRunOnTheDispatchExecutorNotOnTheCallingThread() throws Exception {
        CyApplicationManager applicationManager = mock(CyApplicationManager.class);
        ServiceAdapter adapter = new ServiceAdapter(
                null,
                applicationManager,
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
                null,
                null,
                null,
                null,
                null,
                null);
        List<Runnable> dispatched = new ArrayList<>();
        BrowserHyperlinkListener listener =
                new BrowserHyperlinkListener(adapter, null, null, null, null, dispatched::add);

        // the WebView does not load the link itself
        assertTrue(listener.linkActivated(
                URI.create(BrowserHyperlinkListener.URL_SELECT_ID + "glc").toURL()));
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

    @Test
    void linkWithoutUrlIsLoadedByTheWebView() {
        // a relative link without a base URI, an unknown scheme,
        // an invalid href
        assertNull(BrowserHyperlinkListener.resolve(null, "relative"));
        assertNull(BrowserHyperlinkListener.resolve("about:blank", "relative"));
        assertNull(BrowserHyperlinkListener.resolve(null, "javascript:void(0)"));
        assertNull(BrowserHyperlinkListener.resolve(null, "http://a b"));
    }
}
