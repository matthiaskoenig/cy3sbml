package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
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
 * Tests {@link PanelUpdater#resolveTarget} (pure) and {@link PanelUpdater#run()}'s
 * completed-marking, plus, with a real {@link RenderCoalescer} and {@link
 * LatestTaskExecutor}, the coalescing decision that {@code WebViewPanel.
 * updateInformation} makes before submitting - none of it needs JavaFX (the panel is a
 * plain {@link InfoPanel} implementation).
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

    /** Mirrors {@code WebViewPanel.updateInformation}'s coalescing decision. */
    private static void requestRender(
            Object target, RenderCoalescer coalescer, LatestTaskExecutor executor, PanelUpdater updater) {
        if (coalescer.isRedundant(target)) {
            return;
        }
        coalescer.markPending(target);
        executor.submit(updater);
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

    // ---- run(): marks completed only on an actually-finished render -----------------

    @Test
    void runMarksTheSbaseTargetCompletedWhenShowSBaseInfoSucceeds() {
        Compartment sbase = new SBMLDocument().createModel("m").createCompartment("c");
        when(panel.showSBaseInfo(sbase)).thenReturn(true);

        new PanelUpdater(panel, sbase, htmlFactory, coalescer).run();

        assertTrue(coalescer.isRedundant(sbase));
        verify(panel).setText(anyString());
        verify(panel).showSBaseInfo(sbase);
    }

    @Test
    void runDoesNotMarkTheSbaseTargetCompletedWhenShowSBaseInfoIsCancelled() {
        Compartment sbase = new SBMLDocument().createModel("m").createCompartment("c");
        when(panel.showSBaseInfo(sbase)).thenReturn(false);

        new PanelUpdater(panel, sbase, htmlFactory, coalescer).run();

        assertFalse(coalescer.isRedundant(sbase));
    }

    @Test
    void runMarksTheDocumentTargetCompletedWhenShowSBaseInfoSucceeds() {
        SBMLDocument document = new SBMLDocument();
        when(panel.showSBaseInfo(document)).thenReturn(true);

        new PanelUpdater(panel, document, htmlFactory, coalescer).run();

        assertTrue(coalescer.isRedundant(document));
    }

    @Test
    void runDoesNotMarkTheDocumentTargetCompletedWhenShowSBaseInfoIsCancelled() {
        SBMLDocument document = new SBMLDocument();
        when(panel.showSBaseInfo(document)).thenReturn(false);

        new PanelUpdater(panel, document, htmlFactory, coalescer).run();

        assertFalse(coalescer.isRedundant(document));
    }

    @Test
    void runAlwaysMarksAFixedMessageTargetCompleted() {
        new PanelUpdater(panel, PanelUpdater.TEXT_NO_SBML, htmlFactory, coalescer).run();

        assertTrue(coalescer.isRedundant(PanelUpdater.TEXT_NO_SBML));
        verify(panel).setText(anyString());
        verify(panel, never()).showSBaseInfo(any());
    }

    // ---- coalescing at submit time, with a real LatestTaskExecutor ------------------

    /**
     * A fake panel whose {@code showSBaseInfo} blocks until released, simulating a slow
     * OLS/UniProt/ChEBI lookup, and returns false (as {@code SBaseHTMLThread} does) if
     * interrupted while blocked. {@code renderStarted} counts each time a render for a
     * target actually begins, so a test can tell a render was skipped from one that ran.
     */
    private static final class BlockingPanel implements InfoPanel {
        final CountDownLatch release;
        final AtomicInteger renderStarted = new AtomicInteger();

        BlockingPanel(CountDownLatch release) {
            this.release = release;
        }

        @Override
        public void setText(String text) {}

        @Override
        public boolean showSBaseInfo(Object obj) {
            return showSBaseInfo(Set.of(obj));
        }

        @Override
        public boolean showSBaseInfo(Set<Object> objSet) {
            renderStarted.incrementAndGet();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) {
                    fail("release latch was never counted down");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
            return !Thread.currentThread().isInterrupted();
        }
    }

    /** Waits (bounded) until {@code condition} holds, polling rather than sleeping fixed time. */
    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() > deadline) {
                fail("condition was never true within the timeout");
            }
            Thread.sleep(5);
        }
    }

    @Test
    void aSlowRenderInterruptedByARequestForTheSameTargetStillCompletes() throws Exception {
        SBMLDocument document = new SBMLDocument();
        Compartment target = document.createModel("m").createCompartment("c");
        CountDownLatch release = new CountDownLatch(1);
        BlockingPanel panel = new BlockingPanel(release);
        LatestTaskExecutor executor = new LatestTaskExecutor();
        try {
            // U1: submitted, its render is now blocked mid-lookup
            requestRender(target, coalescer, executor, new PanelUpdater(panel, target, htmlFactory, coalescer));
            awaitTrue(() -> panel.renderStarted.get() == 1);

            // U2: another request for the SAME target while U1 is still in flight - must
            // be coalesced away (isRedundant sees the pending target), NOT submitted, so
            // it must not cancel U1
            requestRender(target, coalescer, executor, new PanelUpdater(panel, target, htmlFactory, coalescer));

            release.countDown();
            awaitTrue(() -> coalescer.isRedundant(target));

            // exactly one render actually ran (U2 never started a second one)
            assertEquals(1, panel.renderStarted.get());
        } finally {
            executor.close();
        }
    }

    @Test
    void aCancelledRenderDoesNotBlockALaterRequestForTheSameTarget() throws Exception {
        SBMLDocument document = new SBMLDocument();
        Model model = document.createModel("m");
        Compartment targetA = model.createCompartment("a");
        Compartment targetB = model.createCompartment("b");
        CountDownLatch releaseA = new CountDownLatch(1); // never counted down: A is cancelled, not finished
        CountDownLatch releaseB = new CountDownLatch(0); // already open: B's render returns immediately
        BlockingPanel panelA = new BlockingPanel(releaseA);
        BlockingPanel panelB = new BlockingPanel(releaseB);
        LatestTaskExecutor executor = new LatestTaskExecutor();
        try {
            // U1: target A, blocks forever (until cancelled)
            requestRender(targetA, coalescer, executor, new PanelUpdater(panelA, targetA, htmlFactory, coalescer));
            awaitTrue(() -> panelA.renderStarted.get() == 1);

            // U2: a genuinely different target B - submitting cancels (interrupts) U1
            requestRender(targetB, coalescer, executor, new PanelUpdater(panelB, targetB, htmlFactory, coalescer));
            awaitTrue(() -> coalescer.isRedundant(targetB));

            // A was cancelled, never completed: a later request for it must still render,
            // not be coalesced away as if it were already shown ("stuck loading" bug)
            assertFalse(coalescer.isRedundant(targetA));
            CountDownLatch releaseA2 = new CountDownLatch(0);
            BlockingPanel panelA2 = new BlockingPanel(releaseA2);
            requestRender(targetA, coalescer, executor, new PanelUpdater(panelA2, targetA, htmlFactory, coalescer));
            // isRedundant(targetA) is already true synchronously (markPending runs before
            // submit), so wait for the render itself to actually run instead
            awaitTrue(() -> panelA2.renderStarted.get() == 1);

            assertEquals(1, panelA2.renderStarted.get());
        } finally {
            executor.close();
        }
    }
}
