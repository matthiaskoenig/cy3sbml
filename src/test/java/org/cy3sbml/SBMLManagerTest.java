package org.cy3sbml;

import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBase;

import static org.junit.Assert.*;

/**
 * Testing the SBMLManager.
 */
public class SBMLManagerTest {

    private SBMLManager manager;

    private static final Long SUID = new Long(123);
    private static final SBMLDocument DOC = new SBMLDocument();
    private static final One2ManyMapping<String, Long> MAPPING = new One2ManyMapping<>();

    @Before
    public void setUp(){
        manager = SBMLManager.getInstance(null);
    }

    @After
    public void tearDown(){
        manager = null;
    }

    @Test
    public void getInstance() throws Exception {
        SBMLManager test = SBMLManager.getInstance();
        assertNotNull(test);
    }

    @Test
    public void getNetwork2SBMLMapper() throws Exception {
        Network2SBMLMapper mapper = manager.getNetwork2SBMLMapper();
        assertNotNull(mapper);
    }

    @Test
    public void addSBMLForNetwork() throws Exception {
        manager.addSBMLForNetwork(DOC, SUID, MAPPING);
        SBMLDocument doc = manager.getSBMLDocument(SUID);
        assertNotNull(doc);
        assertEquals(DOC, doc);
    }

    @Test
    public void removeSBMLForNetwork() throws Exception {
        final CyNetworkFactory networkFactory = new NetworkTestSupport().getNetworkFactory();
        CyNetwork network = networkFactory.createNetwork();
        Long rootSUID = NetworkUtil.getRootNetworkSUID(network);

        manager.addSBMLForNetwork(DOC, rootSUID, MAPPING);
        SBMLDocument doc = manager.getSBMLDocument(rootSUID);
        assertNotNull(doc);
        assertEquals(DOC, doc);

        doc = manager.getSBMLDocument(network);
        assertNotNull(doc);
        assertEquals(DOC, doc);

        manager.removeSBMLForNetwork(network);
        doc = manager.getSBMLDocument(rootSUID);
        assertNull(doc);
    }


    @Test
    public void getMapping() throws Exception {
        manager.addSBMLForNetwork(DOC, SUID, MAPPING);
        One2ManyMapping<String, Long> mapping = manager.getMapping(SUID);
        assertNotNull(mapping);
        assertEquals(MAPPING, mapping);
    }

    @Test
    public void updateCurrent() throws Exception {
        manager.addSBMLForNetwork(DOC, SUID, MAPPING);
        manager.updateCurrent(SUID);
        assertEquals(SUID, manager.getCurrentSUID());
        assertEquals(DOC, manager.getCurrentSBMLDocument());
        assertEquals(MAPPING, manager.getCurrentSBase2CyNodeMapping());
    }

    @Test
    public void getCurrentSUID() throws Exception {
        manager.addSBMLForNetwork(DOC, SUID, MAPPING);
        manager.updateCurrent(SUID);
        assertEquals(SUID, manager.getCurrentSUID());
    }

    @Test
    public void getCurrentSBMLDocument() throws Exception {
        manager.addSBMLForNetwork(DOC, SUID, MAPPING);
        manager.updateCurrent(SUID);
        assertEquals(DOC, manager.getCurrentSBMLDocument());
    }

    @Test
    public void getSBMLDocument() throws Exception {
        manager.addSBMLForNetwork(DOC, SUID, MAPPING);
        SBMLDocument doc = manager.getSBMLDocument(SUID);
        assertNotNull(doc);
        assertEquals(DOC, doc);
    }

    @Test
    public void getCurrentCyNode2SBaseMapping() throws Exception {
        manager.addSBMLForNetwork(DOC, SUID, MAPPING);
        manager.updateCurrent(SUID);
        assertNotNull(manager.getCurrentCyNode2SBaseMapping());
    }

    @Test
    public void getCurrentSBase2CyNodeMapping() throws Exception {
        manager.addSBMLForNetwork(DOC, SUID, MAPPING);
        manager.updateCurrent(SUID);
        assertNotNull(manager.getCurrentSBase2CyNodeMapping());
    }

    @Test
    public void getSBaseByCyId() throws Exception {
        SBMLDocument doc = new SBMLDocument();
        Model model = doc.createModel();
        Compartment c = model.createCompartment();
        c.setId("c1");
        c.setMetaId("c1_meta");
        manager.addSBMLForNetwork(doc, SUID, MAPPING);
        SBase c2 = manager.getSBaseByCyId("c1_meta", SUID);
        assertNotNull(c2);
        assertEquals(c, c2);
    }

}