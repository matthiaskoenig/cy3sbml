package org.cy3sbml;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Optional;
import org.cy3sbml.comp.CompModels;
import org.cy3sbml.comp.ModelResolution;
import org.cy3sbml.comp.SBaseRefResolver;
import org.cy3sbml.mapping.Network2SBMLMapper;
import org.cy3sbml.mapping.One2ManyMapping;
import org.cy3sbml.util.NetworkUtil;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.NetworkTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;

public class SBMLManagerTest {

    private SBMLManager manager;

    private static final Long SUID = Long.valueOf(123);
    private static final SBMLDocument DOC = new SBMLDocument();
    private static final One2ManyMapping<String, Long> MAPPING = new One2ManyMapping<>();

    @BeforeEach
    public void setUp() {
        manager = new SBMLManager(null);
    }

    @AfterEach
    public void tearDown() {
        manager = null;
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

    /** Reads the comp test model with the external models it references. */
    private static SBMLDocument readTop() throws Exception {
        java.net.URL url = SBMLManagerTest.class.getResource("/models/comp/unit/top.xml");
        SBMLDocument document = new SBMLReader().readSBMLFromStream(url.openStream());
        document.setLocationURI(url.toURI().toString());
        return document;
    }

    @Test
    public void resolverOfTheReaderIsUsedForItsDocuments() throws Exception {
        SBMLDocument document = readTop();
        SBaseRefResolver resolver = new SBaseRefResolver(new CompModels(document));
        ModelResolution.Resolved external =
                (ModelResolution.Resolved) resolver.models().resolve(document, "ext");
        CyNetwork network = new NetworkTestSupport().getNetworkFactory().createNetwork();
        manager.addSBMLForNetwork(document, network, MAPPING);
        manager.addSBaseRefResolver(resolver);

        assertSame(resolver, manager.getSBaseRefResolver(document.getModel()));
        // an element of an external document the resolver read
        assertSame(resolver, manager.getSBaseRefResolver(external.model()));
    }

    @Test
    public void elementOfAnotherDocumentGetsANewResolver() throws Exception {
        SBMLDocument document = readTop();
        manager.addSBaseRefResolver(new SBaseRefResolver(new CompModels(readTop())));

        SBaseRefResolver resolver = manager.getSBaseRefResolver(document.getModel());

        assertSame(document, resolver.models().document());
    }

    @Test
    public void resolverIsRemovedWithTheNetworksOfItsDocument() throws Exception {
        SBMLDocument document = readTop();
        SBaseRefResolver resolver = new SBaseRefResolver(new CompModels(document));
        CyNetwork network = new NetworkTestSupport().getNetworkFactory().createNetwork();
        manager.addSBMLForNetwork(document, network, MAPPING);
        manager.addSBaseRefResolver(resolver);

        manager.removeSBMLForNetwork(network);

        assertNotSame(resolver, manager.getSBaseRefResolver(document.getModel()));
    }

    /** The network collection of a model is found by the model object, not its id. */
    @Test
    public void rootNetworkOfAModel() throws Exception {
        SBMLDocument first = readTop();
        SBMLDocument second = readTop();
        CyNetwork firstNetwork = new NetworkTestSupport().getNetworkFactory().createNetwork();
        CyNetwork secondNetwork = new NetworkTestSupport().getNetworkFactory().createNetwork();
        manager.addSBMLForNetwork(first, firstNetwork, MAPPING);
        manager.addModelForNetwork(firstNetwork, first.getModel());
        manager.addSBMLForNetwork(second, secondNetwork, MAPPING);
        manager.addModelForNetwork(secondNetwork, second.getModel());

        assertEquals(
                Optional.of(NetworkUtil.getRootNetworkSUID(secondNetwork)), manager.rootNetwork(second.getModel()));
        assertEquals(Optional.empty(), manager.rootNetwork(readTop().getModel()));
    }

    /**
     * Without the resolver of the reader (a restored session), the resolver of a document is
     * created once and uses the open documents instead of reading their files again.
     */
    @Test
    public void resolverOfARestoredDocumentUsesTheOpenDocuments() throws Exception {
        SBMLDocument document = readTop();
        SBaseRefResolver reader = new SBaseRefResolver(new CompModels(document));
        ModelResolution.Resolved external =
                (ModelResolution.Resolved) reader.models().resolve(document, "ext");
        CyNetwork network = new NetworkTestSupport().getNetworkFactory().createNetwork();
        CyNetwork externalNetwork = new NetworkTestSupport().getNetworkFactory().createNetwork();
        manager.addSBMLForNetwork(document, network, MAPPING);
        manager.addSBMLForNetwork(external.document(), externalNetwork, MAPPING);

        SBaseRefResolver resolver = manager.getSBaseRefResolver(document.getModel());

        assertSame(resolver, manager.getSBaseRefResolver(document.getModel()));
        ModelResolution.Resolved restored =
                (ModelResolution.Resolved) resolver.models().resolve(document, "ext");
        assertSame(external.model(), restored.model());
    }
}
