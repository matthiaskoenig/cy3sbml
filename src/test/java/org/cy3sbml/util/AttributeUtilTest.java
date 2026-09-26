package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.NetworkTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AttributeUtilTest {

    private CyNetwork network;

    @BeforeEach
    void setUp() {
        network = new NetworkTestSupport().getNetworkFactory().createNetwork();
    }

    @Test
    void setCreatesColumnAndStoresValue() {
        CyNode node = network.addNode();
        AttributeUtil.set(network, node, "myAttr", "value1", String.class);
        assertEquals("value1", AttributeUtil.get(network, node, "myAttr", String.class));
    }

    @Test
    void setWithNullValueDoesNotCreateColumn() {
        CyNode node = network.addNode();
        AttributeUtil.set(network, node, "myAttr", null, String.class);
        assertNull(network.getDefaultNodeTable().getColumn("myAttr"));
    }

    @Test
    void setListCreatesListColumnAndStoresValues() {
        CyNode node = network.addNode();
        List<String> values = List.of("a", "b");
        AttributeUtil.setList(network, node, "myList", values, String.class);
        assertEquals(values, AttributeUtil.get(network, node, "myList", List.class));
    }

    @Test
    void getNodeByAttributeFindsFirstMatchingRow() {
        CyNode node = network.addNode();
        AttributeUtil.set(network, node, "sbmlId", "s1", String.class);

        CyNode found = AttributeUtil.getNodeByAttribute(network, "sbmlId", "s1");

        assertEquals(node, found);
    }

    @Test
    void getNodeByAttributeReturnsNullWhenNoMatch() {
        CyNode node = network.addNode();
        AttributeUtil.set(network, node, "sbmlId", "s1", String.class);

        assertNull(AttributeUtil.getNodeByAttribute(network, "sbmlId", "missing"));
    }

    @Test
    void copyNodeAttributesCopiesUserColumnValues() {
        CyNode source = network.addNode();
        CyNode target = network.addNode();
        AttributeUtil.set(network, source, "type", "species", String.class);

        AttributeUtil.copyNodeAttributes(network, source, target);

        assertEquals("species", AttributeUtil.get(network, target, "type", String.class));
    }

    @Test
    void copyEdgeAttributesCopiesUserColumnValues() {
        CyNode n1 = network.addNode();
        CyNode n2 = network.addNode();
        CyNode n3 = network.addNode();
        CyEdge source = network.addEdge(n1, n2, true);
        CyEdge target = network.addEdge(n1, n3, true);
        AttributeUtil.set(network, source, "interactionType", "reaction", String.class);

        AttributeUtil.copyEdgeAttributes(network, source, target);

        assertEquals("reaction", AttributeUtil.get(network, target, "interactionType", String.class));
    }
}
