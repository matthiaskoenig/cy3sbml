package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import org.cy3sbml.SBMLManager;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

/**
 * Tests {@link PanelUpdater#resolveTarget} (pure) and {@link PanelUpdater#run()}, without
 * needing JavaFX (the panel is a plain {@link InfoPanel} mock). The coalescing itself -
 * whether a render for a given target is worth submitting at all - now lives entirely in
 * {@code LatestTaskExecutor.submit(Object key, Runnable task)}; see
 * {@code LatestTaskExecutorTest} for that.
 */
class PanelUpdaterTest {
    private CyNetwork network;
    private SBMLManager sbmlManager;
    private SBaseHTMLFactory htmlFactory;
    private InfoPanel panel;

    @BeforeEach
    void setUp() {
        network = new NetworkTestSupport().getNetworkFactory().createNetwork();
        sbmlManager = mock(SBMLManager.class);
        htmlFactory = mock(SBaseHTMLFactory.class);
        when(htmlFactory.createHTMLText(anyString())).thenAnswer(inv -> inv.getArgument(0));
        panel = mock(InfoPanel.class);
    }

    // ---- resolveTarget -------------------------------------------------------------

    @Test
    void resolveTargetReturnsTheDocumentWhenNothingIsSelected() {
        SBMLDocument document = new SBMLDocument();
        when(sbmlManager.getCurrentSBMLDocument()).thenReturn(document);
        when(sbmlManager.getCyIdsFromSUIDs(List.of())).thenReturn(List.of());

        assertSame(document, PanelUpdater.resolveTarget(network, sbmlManager));
    }

    @Test
    void resolveTargetReturnsTheSelectedNodesSbase() {
        SBMLDocument document = new SBMLDocument();
        Model model = document.createModel("m");
        Compartment sbase = model.createCompartment("c");
        CyNode node = network.addNode();
        network.getRow(node).set(CyNetwork.SELECTED, true);
        when(sbmlManager.getCurrentSBMLDocument()).thenReturn(document);
        when(sbmlManager.getCyIdsFromSUIDs(List.of(node.getSUID()))).thenReturn(List.of("c"));
        when(sbmlManager.getSBaseByCyId("c")).thenReturn(sbase);

        assertSame(sbase, PanelUpdater.resolveTarget(network, sbmlManager));
    }

    @Test
    void resolveTargetReturnsTheNoSbmlMessageWhenThereIsNoDocument() {
        when(sbmlManager.getCurrentSBMLDocument()).thenReturn(null);

        assertEquals(PanelUpdater.TEXT_NO_SBML, PanelUpdater.resolveTarget(network, sbmlManager));
    }

    @Test
    void resolveTargetReturnsTheNoSbaseMessageWhenTheSelectedNodeHasNoSbase() {
        SBMLDocument document = new SBMLDocument();
        CyNode node = network.addNode();
        network.getRow(node).set(CyNetwork.SELECTED, true);
        when(sbmlManager.getCurrentSBMLDocument()).thenReturn(document);
        when(sbmlManager.getCyIdsFromSUIDs(List.of(node.getSUID()))).thenReturn(List.of("missing"));
        when(sbmlManager.getSBaseByCyId("missing")).thenReturn(null);

        assertEquals(PanelUpdater.TEXT_NO_SBML_NODE, PanelUpdater.resolveTarget(network, sbmlManager));
    }

    // ---- run() ------------------------------------------------------------------

    @Test
    void runShowsTheSbaseTargetAfterAWebServiceLoadingPlaceholder() {
        Compartment sbase = new SBMLDocument().createModel("m").createCompartment("c");

        new PanelUpdater(panel, sbase, htmlFactory).run();

        verify(panel).setText(anyString());
        verify(panel).showSBaseInfo(sbase);
    }

    @Test
    void runShowsTheDocumentTargetDirectly() {
        SBMLDocument document = new SBMLDocument();

        new PanelUpdater(panel, document, htmlFactory).run();

        verify(panel, never()).setText(anyString());
        verify(panel).showSBaseInfo(document);
    }

    @Test
    void runShowsAFixedMessageTargetAsText() {
        new PanelUpdater(panel, PanelUpdater.TEXT_NO_SBML, htmlFactory).run();

        verify(panel).setText(anyString());
        verify(panel, never()).showSBaseInfo(any());
    }
}
