package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.cy3sbml.SBML;
import org.cytoscape.group.GroupTestSupport;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.NetworkTestSupport;
import org.cytoscape.model.subnetwork.CySubNetwork;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.Test;

/**
 * The layout networks of the import (#71): one network per layout with a node per glyph.
 * <p>
 * {@code layout_02.xml}: species A (glyphs {@code sg_A}, {@code sg_A2}), B ({@code sg_B}),
 * E ({@code sg_E}, zero size), a glyph of an unknown species ({@code sg_X}), reaction R1
 * with the reaction glyph {@code rg_R1} and reaction R2 without glyph, the compartment glyph
 * {@code cg_c}, the general glyph {@code gg_1} and text glyphs; the second layout has only
 * the glyphs {@code sg2_A} and {@code sg2_B}.
 */
class LayoutNetworkBuilderTest {

    private static final String LAYOUT_02 = "layout_02";

    @Test
    void everyLayoutIsANetworkAfterTheSubnetworks() throws Exception {
        List<String> names = names(read(LAYOUT_02 + ".xml"));

        assertEquals(
                List.of(
                        "layout_02",
                        "layout_02__kinetic",
                        "layout_02__all",
                        "layout_02__layout_layout1",
                        "layout_02__layout_layout2"),
                names);
    }

    @Test
    void layoutNetworkHasTheLayoutType() throws Exception {
        CyNetwork network = layout1();

        assertEquals(SBML.NETWORKTYPE_LAYOUT, network.getRow(network).get(SBML.NETWORKTYPE_ATTR, String.class));
        assertEquals("layout1", network.getRow(network).get(SBML.ATTR_LAYOUT_ID, String.class));
    }

    @Test
    void everyGlyphIsANode() throws Exception {
        Map<String, CyNode> glyphs = glyphNodes(layout1());

        assertEquals(Set.of("cg_c", "sg_A", "sg_A2", "sg_B", "sg_E", "sg_X", "rg_R1", "gg_1"), glyphs.keySet());
    }

    @Test
    void aliasesRepresentTheirElement() throws Exception {
        Map<String, CyNetwork> networks = byName(read(LAYOUT_02 + ".xml"));
        CyNetwork layout = networks.get("layout_02__layout_layout1");
        CyNetwork all = networks.get("layout_02__all");
        Map<String, CyNode> glyphs = glyphNodes(layout);
        CyNode species = nodeBySbmlId(all, "A");

        for (String glyph : List.of("sg_A", "sg_A2")) {
            CyRow row = layout.getRow(glyphs.get(glyph));
            assertEquals(all.getRow(species).get(SBML.ATTR_CYID, String.class), row.get(SBML.ATTR_CYID, String.class));
            assertEquals(SBML.NODETYPE_SPECIES, row.get(SBML.NODETYPE_ATTR, String.class));
            assertEquals("species A", row.get(SBML.ATTR_NAME, String.class));
            assertEquals("species A", row.get(CyNetwork.NAME, String.class));
            assertEquals(SBML.NODETYPE_LAYOUT_SPECIESGLYPH, row.get(SBML.ATTR_LAYOUT_GLYPH_TYPE, String.class));
        }
        assertNotEquals(glyphs.get("sg_A"), glyphs.get("sg_A2"));
        assertFalse(all.containsNode(glyphs.get("sg_A")));
    }

    @Test
    void glyphOfAnUnknownElementIsItsOwnNode() throws Exception {
        CyNetwork layout = layout1();
        CyRow row = layout.getRow(glyphNodes(layout).get("sg_X"));

        assertEquals(SBML.NODETYPE_LAYOUT_SPECIESGLYPH, row.get(SBML.NODETYPE_ATTR, String.class));
        assertEquals("sg_X", row.get(SBML.ATTR_ID, String.class));
        assertEquals("sg_X", row.get(SBML.LABEL, String.class));
        assertNotNull(row.get(SBML.ATTR_CYID, String.class));
    }

    @Test
    void glyphTypes() throws Exception {
        CyNetwork layout = layout1();
        Map<String, CyNode> glyphs = glyphNodes(layout);

        assertEquals(
                SBML.NODETYPE_LAYOUT_COMPARTMENTGLYPH,
                layout.getRow(glyphs.get("cg_c")).get(SBML.ATTR_LAYOUT_GLYPH_TYPE, String.class));
        assertEquals(
                SBML.NODETYPE_COMPARTMENT, layout.getRow(glyphs.get("cg_c")).get(SBML.NODETYPE_ATTR, String.class));
        assertEquals(
                SBML.NODETYPE_LAYOUT_REACTIONGLYPH,
                layout.getRow(glyphs.get("rg_R1")).get(SBML.ATTR_LAYOUT_GLYPH_TYPE, String.class));
        assertEquals(SBML.NODETYPE_REACTION, layout.getRow(glyphs.get("rg_R1")).get(SBML.NODETYPE_ATTR, String.class));
        assertEquals(
                SBML.NODETYPE_LAYOUT_GENERALGLYPH,
                layout.getRow(glyphs.get("gg_1")).get(SBML.ATTR_LAYOUT_GLYPH_TYPE, String.class));
        assertEquals(
                SBML.NODETYPE_LAYOUT_GENERALGLYPH,
                layout.getRow(glyphs.get("gg_1")).get(SBML.NODETYPE_ATTR, String.class));
    }

    @Test
    void geometryIsTheCentreAndSizeOfTheBoundingBox() throws Exception {
        CyNetwork layout = layout1();
        Map<String, CyNode> glyphs = glyphNodes(layout);

        assertEquals(new GlyphBox(50, 50, 60, 20), box(layout, glyphs.get("sg_A")));
        assertEquals(new GlyphBox(50, 250, 60, 20), box(layout, glyphs.get("sg_A2")));
        // zero size is the default size
        assertEquals(new GlyphBox(175, 35, 30, 30), box(layout, glyphs.get("sg_E")));
    }

    @Test
    void textGlyphsAreTheLabels() throws Exception {
        CyNetwork layout = layout1();
        Map<String, CyNode> glyphs = glyphNodes(layout);

        assertEquals("Alpha", layout.getRow(glyphs.get("sg_A")).get(SBML.LABEL, String.class));
        // origin of text: the label of the element
        assertEquals("species B", layout.getRow(glyphs.get("sg_B")).get(SBML.LABEL, String.class));
        // no text glyph: the label of the element
        assertEquals("species A", layout.getRow(glyphs.get("sg_A2")).get(SBML.LABEL, String.class));
    }

    @Test
    void layoutsAreIndependentNetworks() throws Exception {
        Map<String, CyNetwork> networks = byName(read(LAYOUT_02 + ".xml"));
        CyNetwork layout2 = networks.get("layout_02__layout_layout2");
        Map<String, CyNode> glyphs = glyphNodes(layout2);

        assertEquals(Set.of("sg2_A", "sg2_B"), glyphs.keySet());
        assertEquals(new GlyphBox(25, 10, 50, 20), box(layout2, glyphs.get("sg2_A")));
        for (CyNode node : glyphs.values()) {
            assertFalse(networks.get("layout_02__layout_layout1").containsNode(node));
        }
    }

    @Test
    void layoutColumnsAreLocalToTheLayoutNetworks() throws Exception {
        Map<String, CyNetwork> networks = byName(read(LAYOUT_02 + ".xml"));

        for (String name : List.of("layout_02", "layout_02__kinetic", "layout_02__all")) {
            CyNetwork network = networks.get(name);
            assertNull(network.getDefaultNodeTable().getColumn(SBML.ATTR_LAYOUT_X), name);
            assertNull(network.getDefaultNodeTable().getColumn(SBML.ATTR_LAYOUT_GLYPH), name);
        }
    }

    @Test
    void modelWithoutLayoutHasNoLayoutNetwork() throws Exception {
        List<String> names = names(read("core_01.xml"));

        assertTrue(names.stream().noneMatch(name -> name.contains(SBML.SUFFIX_SUBNETWORK_LAYOUT)), names::toString);
    }

    @Test
    void qualLayoutHasANodePerSpeciesGlyph() throws Exception {
        Map<String, CyNetwork> networks = byName(read("layout_01.xml"));
        CyNetwork layout = layoutNetworks(networks).get(0);
        CyNetwork all = allNetwork(networks);

        Map<String, CyNode> glyphs = glyphNodes(layout);
        assertEquals(87, glyphs.size());
        Set<String> cyIds = all.getNodeList().stream()
                .map(n -> all.getRow(n).get(SBML.ATTR_CYID, String.class))
                .collect(Collectors.toSet());
        for (CyNode node : glyphs.values()) {
            assertTrue(cyIds.contains(layout.getRow(node).get(SBML.ATTR_CYID, String.class)));
            assertEquals(SBML.NODETYPE_QUAL_SPECIES, layout.getRow(node).get(SBML.NODETYPE_ATTR, String.class));
        }
    }

    @Test
    void generalGlyphsAndTextGlyphs() throws Exception {
        Map<String, CyNetwork> networks = byName(read("small_population.xml"));
        CyNetwork layout = layoutNetworks(networks).get(0);

        List<String> types = layout.getNodeList().stream()
                .map(n -> layout.getRow(n).get(SBML.ATTR_LAYOUT_GLYPH_TYPE, String.class))
                .toList();
        assertEquals(
                36,
                types.stream().filter(SBML.NODETYPE_LAYOUT_GENERALGLYPH::equals).count());
        assertEquals(
                1,
                types.stream()
                        .filter(SBML.NODETYPE_LAYOUT_COMPARTMENTGLYPH::equals)
                        .count());
        // every general glyph has a text glyph
        for (CyNode node : layout.getNodeList()) {
            CyRow row = layout.getRow(node);
            if (SBML.NODETYPE_LAYOUT_GENERALGLYPH.equals(row.get(SBML.ATTR_LAYOUT_GLYPH_TYPE, String.class))) {
                assertNotEquals(row.get(SBML.ATTR_LAYOUT_GLYPH, String.class), row.get(SBML.LABEL, String.class));
            }
        }
    }

    // ------------------------------------------------------------
    // support
    // ------------------------------------------------------------

    static CyNetwork[] read(String fileName) throws Exception {
        String resource = "/models/unittests/" + fileName;
        try (InputStream stream = LayoutNetworkBuilderTest.class.getResourceAsStream(resource)) {
            assertNotNull(stream, resource);
            SBMLReaderTask task = new SBMLReaderTask(
                    stream,
                    fileName,
                    new NetworkTestSupport().getNetworkFactory(),
                    new GroupTestSupport().getGroupFactory());
            task.run(mock(TaskMonitor.class));
            return task.getNetworks();
        }
    }

    static CyNetwork layout1() throws Exception {
        return byName(read(LAYOUT_02 + ".xml")).get("layout_02__layout_layout1");
    }

    static List<String> names(CyNetwork[] networks) {
        List<String> names = new ArrayList<>();
        for (CyNetwork network : networks) {
            names.add(network.getRow(network).get(CyNetwork.NAME, String.class));
        }
        return names;
    }

    static Map<String, CyNetwork> byName(CyNetwork[] networks) {
        Map<String, CyNetwork> byName = new LinkedHashMap<>();
        for (CyNetwork network : networks) {
            byName.put(network.getRow(network).get(CyNetwork.NAME, String.class), network);
        }
        return byName;
    }

    static List<CyNetwork> layoutNetworks(Map<String, CyNetwork> networks) {
        return networks.entrySet().stream()
                .filter(e -> e.getKey().contains(SBML.SUFFIX_SUBNETWORK_LAYOUT))
                .map(Map.Entry::getValue)
                .toList();
    }

    static CyNetwork allNetwork(Map<String, CyNetwork> networks) {
        return networks.entrySet().stream()
                .filter(e -> e.getKey().endsWith(SBML.SUFFIX_SUBNETWORK_ALL))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElseThrow();
    }

    /** The glyph nodes of the layout network by glyph id (metaId without id), without the generated nodes. */
    static Map<String, CyNode> glyphNodes(CyNetwork layout) {
        Map<String, CyNode> nodes = new HashMap<>();
        for (CyNode node : layout.getNodeList()) {
            String glyph = layout.getRow(node).get(SBML.ATTR_LAYOUT_GLYPH, String.class);
            if (glyph != null) {
                assertNull(nodes.put(glyph, node), "two nodes of glyph " + glyph);
            }
        }
        return nodes;
    }

    /** The generated nodes of the layout network by the SBML id of their element. */
    static Map<String, CyNode> generatedNodes(CyNetwork layout) {
        Map<String, CyNode> nodes = new HashMap<>();
        for (CyNode node : layout.getNodeList()) {
            CyRow row = layout.getRow(node);
            if (SBML.NODETYPE_LAYOUT_GENERATED.equals(row.get(SBML.ATTR_LAYOUT_GLYPH_TYPE, String.class))) {
                nodes.put(row.get(SBML.ATTR_ID, String.class), node);
            }
        }
        return nodes;
    }

    static CyNode nodeBySbmlId(CyNetwork network, String id) {
        return network.getNodeList().stream()
                .filter(n -> id.equals(network.getRow(n).get(SBML.ATTR_ID, String.class)))
                .findFirst()
                .orElseThrow();
    }

    static GlyphBox box(CyNetwork network, CyNode node) {
        CyRow row = network.getRow(node);
        return new GlyphBox(
                row.get(SBML.ATTR_LAYOUT_X, Double.class),
                row.get(SBML.ATTR_LAYOUT_Y, Double.class),
                row.get(SBML.ATTR_LAYOUT_WIDTH, Double.class),
                row.get(SBML.ATTR_LAYOUT_HEIGHT, Double.class));
    }

    /** The edges between the nodes as "source -> target" of the node labels with their interaction type. */
    static Set<String> edges(CyNetwork network, Map<CyNode, String> names) {
        Set<String> edges = new HashSet<>();
        for (CyEdge edge : network.getEdgeList()) {
            String source = names.get(edge.getSource());
            String target = names.get(edge.getTarget());
            if (source != null && target != null) {
                edges.add(
                        source + " -> " + target + " " + network.getRow(edge).get(SBML.INTERACTION_ATTR, String.class));
            }
        }
        return edges;
    }

    static boolean isSubnetworkOfTheModel(CyNetwork layout, CyNetwork all) {
        return ((CySubNetwork) layout).getRootNetwork().equals(((CySubNetwork) all).getRootNetwork());
    }
}
