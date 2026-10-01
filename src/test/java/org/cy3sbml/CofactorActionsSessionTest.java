package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.cy3sbml.actions.MergeCofactorsAction;
import org.cy3sbml.actions.SplitCofactorsAction;
import org.cy3sbml.cofactors.CofactorManager;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.events.SetCurrentNetworkEvent;
import org.cytoscape.event.DummyCyEventHelper;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.SBMLDocument;

/**
 * The cofactor actions are enabled for the current network of a loaded session: Cytoscape
 * sets it before the SBML mapping of the session is restored.
 */
class CofactorActionsSessionTest {

    @Test
    void actionsAreEnabledWhenTheSessionIsRestored() {
        CyNetwork network = new NetworkTestSupport().getNetwork();
        CyApplicationManager applicationManager = mock(CyApplicationManager.class);
        when(applicationManager.getCurrentNetwork()).thenReturn(network);
        ServiceAdapter adapter = new ServiceAdapter(
                null,
                applicationManager,
                null,
                mock(CyNetworkViewManager.class),
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
        SBMLManager sbmlManager = new SBMLManager(applicationManager);
        CofactorManager cofactorManager = new CofactorManager(sbmlManager, new DummyCyEventHelper());
        SplitCofactorsAction split = new SplitCofactorsAction(adapter, sbmlManager, cofactorManager);
        MergeCofactorsAction merge = new MergeCofactorsAction(adapter, sbmlManager, cofactorManager);

        // the current network of the session, before its mapping is restored
        split.handleEvent(new SetCurrentNetworkEvent(applicationManager, network));
        merge.handleEvent(new SetCurrentNetworkEvent(applicationManager, network));
        assertFalse(split.isEnabled());
        assertFalse(merge.isEnabled());

        sbmlManager.addSBMLForNetwork(new SBMLDocument(3, 1), network, new One2ManyMapping<>());
        sbmlManager.sessionRestored();

        assertTrue(split.isEnabled());
        assertTrue(merge.isEnabled());
    }
}
