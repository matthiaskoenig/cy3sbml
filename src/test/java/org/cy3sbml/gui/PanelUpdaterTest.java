package org.cy3sbml.gui;

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
 * Tests the render coalescing in {@link PanelUpdater#run()}: a request that resolves to
 * the same target as the last one actually rendered must be skipped, without needing
 * JavaFX (the panel is a plain {@link InfoPanel} mock).
 */
class PanelUpdaterTest {
    private CyNetwork network;
    private SBMLManager sbmlManager;
    private SBaseHTMLFactory htmlFactory;
    private InfoPanel panel;
    private RenderCoalescer coalescer;

    @BeforeEach
    void setUp() {
        network = new NetworkTestSupport().getNetworkFactory().createNetwork();
        sbmlManager = mock(SBMLManager.class);
        htmlFactory = mock(SBaseHTMLFactory.class);
        when(htmlFactory.createHTMLText(anyString())).thenAnswer(inv -> inv.getArgument(0));
        panel = mock(InfoPanel.class);
        coalescer = new RenderCoalescer();
    }

    private PanelUpdater updater() {
        return new PanelUpdater(panel, network, sbmlManager, htmlFactory, coalescer);
    }

    @Test
    void rendersOnceForRepeatedDocumentWithNoSelection() {
        SBMLDocument document = new SBMLDocument();
        when(sbmlManager.getCurrentSBMLDocument()).thenReturn(document);
        when(sbmlManager.getCyIdsFromSUIDs(List.of())).thenReturn(List.of());

        updater().run();
        updater().run();
        updater().run();

        verify(panel, times(1)).showSBaseInfo(document);
    }

    @Test
    void rendersOnceForRepeatedSbase() {
        SBMLDocument document = new SBMLDocument();
        Model model = document.createModel("m");
        Compartment sbase = model.createCompartment("c");
        CyNode node = network.addNode();
        network.getRow(node).set(CyNetwork.SELECTED, true);
        when(sbmlManager.getCurrentSBMLDocument()).thenReturn(document);
        when(sbmlManager.getCyIdsFromSUIDs(List.of(node.getSUID()))).thenReturn(List.of("c"));
        when(sbmlManager.getSBaseByCyId("c")).thenReturn(sbase);

        updater().run();
        updater().run();

        verify(panel, times(1)).showSBaseInfo(sbase);
        verify(panel, times(1)).setText(anyString());
    }

    @Test
    void rendersAgainWhenSelectionResolvesToADifferentSbase() {
        SBMLDocument document = new SBMLDocument();
        Model model = document.createModel("m");
        Compartment sbaseA = model.createCompartment("a");
        Compartment sbaseB = model.createCompartment("b");
        CyNode node = network.addNode();
        network.getRow(node).set(CyNetwork.SELECTED, true);
        when(sbmlManager.getCurrentSBMLDocument()).thenReturn(document);
        when(sbmlManager.getCyIdsFromSUIDs(List.of(node.getSUID()))).thenReturn(List.of("a"), List.of("b"));
        when(sbmlManager.getSBaseByCyId("a")).thenReturn(sbaseA);
        when(sbmlManager.getSBaseByCyId("b")).thenReturn(sbaseB);

        updater().run();
        updater().run();

        verify(panel).showSBaseInfo(sbaseA);
        verify(panel).showSBaseInfo(sbaseB);
    }

    @Test
    void rendersAgainAfterResetEvenForTheSameDocument() {
        SBMLDocument document = new SBMLDocument();
        when(sbmlManager.getCurrentSBMLDocument()).thenReturn(document);
        when(sbmlManager.getCyIdsFromSUIDs(List.of())).thenReturn(List.of());

        updater().run();
        coalescer.reset();
        updater().run();

        verify(panel, times(2)).showSBaseInfo(document);
    }

    @Test
    void rendersNoSbmlMessageOnceForRepeatedMissingDocument() {
        when(sbmlManager.getCurrentSBMLDocument()).thenReturn(null);

        updater().run();
        updater().run();

        verify(panel, times(1)).setText(anyString());
        verify(panel, never()).showSBaseInfo(any());
    }

    @Test
    void rendersNoSbaseMessageOnceWhenSelectedNodeHasNoSbase() {
        SBMLDocument document = new SBMLDocument();
        CyNode node = network.addNode();
        network.getRow(node).set(CyNetwork.SELECTED, true);
        when(sbmlManager.getCurrentSBMLDocument()).thenReturn(document);
        when(sbmlManager.getCyIdsFromSUIDs(List.of(node.getSUID()))).thenReturn(List.of("missing"));
        when(sbmlManager.getSBaseByCyId("missing")).thenReturn(null);

        updater().run();
        updater().run();

        verify(panel, times(1)).setText(anyString());
        verify(panel, never()).showSBaseInfo(any());
    }
}
