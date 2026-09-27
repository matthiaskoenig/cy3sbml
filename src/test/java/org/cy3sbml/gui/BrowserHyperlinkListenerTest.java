package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.HyperlinkEvent;
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
                null,
                null);
        List<Runnable> dispatched = new ArrayList<>();
        BrowserHyperlinkListener listener =
                new BrowserHyperlinkListener(adapter, null, null, null, null, dispatched::add);

        HyperlinkEvent event = new HyperlinkEvent(
                this,
                HyperlinkEvent.EventType.ACTIVATED,
                URI.create(BrowserHyperlinkListener.URL_SELECT_ID + "glc").toURL());

        // the WebView does not load the link itself
        assertTrue(listener.hyperlinkUpdate(event));
        // the selection (a Cytoscape model edit) waits for the dispatch executor
        verifyNoInteractions(applicationManager);
        assertEquals(1, dispatched.size());

        dispatched.get(0).run();
        verify(applicationManager).getCurrentNetwork();
    }

    @Test
    void linkWithoutUrlIsLoadedByTheWebView() {
        List<Runnable> dispatched = new ArrayList<>();
        BrowserHyperlinkListener listener = new BrowserHyperlinkListener(null, null, null, null, null, dispatched::add);
        HyperlinkEvent event = new HyperlinkEvent(this, HyperlinkEvent.EventType.ACTIVATED, null, "relative");

        assertEquals(false, listener.hyperlinkUpdate(event));
        assertTrue(dispatched.isEmpty());
    }
}
