package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    /** All edges of a reaction start at the reaction, as in the network of the model. */
    @Test
    void speciesReferenceGlyphsAreEdgesOfTheirModelEdge() throws Exception {
        CyNetwork layout = layout1();

        Set<String> edges = edges(layout, keys(layout));
        assertTrue(edges.contains("rg_R1 -> sg_A " + SBML.INTERACTION_REACTION_REACTANT), edges::toString);
        // without species reference: the edge between the nodes
        assertTrue(edges.contains("rg_R1 -> sg_B " + SBML.INTERACTION_REACTION_PRODUCT), edges::toString);
        // the role inhibitor of the modifier edge
        assertTrue(edges.contains("rg_R1 -> sg_E " + SBML.INTERACTION_REACTION_MODIFIER), edges::toString);
    }

    @Test
    void edgeOfASpeciesReferenceGlyphIsACopyOfTheModelEdge() throws Exception {
        CyNetwork layout = layout1();
        Map<CyNode, String> keys = keys(layout);

        CyEdge edge = layout.getEdgeList().stream()
                .filter(e -> "rg_R1".equals(keys.get(e.getSource())) && "sg_A".equals(keys.get(e.getTarget())))
                .findFirst()
                .orElseThrow();
        assertEquals("sr_A", layout.getRow(edge).get(SBML.ATTR_ID, String.class));
    }

    @Test
    void speciesReferenceGlyphWithoutModelEdgeIsAnEdgeOfTheRole() throws Exception {
        CyNetwork layout = layout1();

        Set<String> edges = edges(layout, keys(layout));
        assertTrue(edges.contains("rg_R1 -> sg_X " + SBML.INTERACTION_REACTION_ACTIVATOR), edges::toString);
    }

    @Test
    void speciesReferenceGlyphOfAMissingSpeciesGlyphIsNoEdge() throws Exception {
        CyNetwork layout = layout1();
        Map<CyNode, String> keys = keys(layout);

        long edgesOfR1 = layout.getEdgeList().stream()
                .filter(e -> "rg_R1".equals(keys.get(e.getSource())) || "rg_R1".equals(keys.get(e.getTarget())))
                .count();
        assertEquals(4, edgesOfR1);
    }

    @Test
    void reactionWithoutGlyphIsAGeneratedNodeBetweenItsParticipants() throws Exception {
        CyNetwork layout = layout1();
        CyNode generated = generatedNodes(layout).get("R2");

        assertNotNull(generated);
        assertEquals(SBML.NODETYPE_REACTION, layout.getRow(generated).get(SBML.NODETYPE_ATTR, String.class));
        assertNull(layout.getRow(generated).get(SBML.ATTR_LAYOUT_GLYPH, String.class));
        GlyphBox box = box(layout, generated);
        assertEquals((50 + 50 + 330) / 3.0, box.x(), 1e-9);
        assertEquals((50 + 250 + 150) / 3.0, box.y(), 1e-9);
        assertEquals(20, box.width());
        assertEquals(20, box.height());
        Set<String> edges = edges(layout, keys(layout));
        assertTrue(edges.contains("R2 -> sg_B " + SBML.INTERACTION_REACTION_REACTANT), edges::toString);
        assertTrue(edges.contains("R2 -> sg_A " + SBML.INTERACTION_REACTION_PRODUCT), edges::toString);
        assertTrue(edges.contains("R2 -> sg_A2 " + SBML.INTERACTION_REACTION_PRODUCT), edges::toString);
    }

    @Test
    void reactionWithAGlyphIsNotGenerated() throws Exception {
        assertEquals(Set.of("R2"), generatedNodes(layout1()).keySet());
    }

    @Test
    void referenceGlyphsOfAGeneralGlyphAreEdges() throws Exception {
        CyNetwork layout = layout1();
        Map<CyNode, String> keys = keys(layout);

        Set<String> edges = edges(layout, keys);
        assertTrue(edges.contains("gg_1 -> sg_B " + SBML.INTERACTION_LAYOUT_REFERENCE), edges::toString);
        CyEdge edge = layout.getEdgeList().stream()
                .filter(e -> "gg_1".equals(keys.get(e.getSource())))
                .findFirst()
                .orElseThrow();
        assertEquals("highlight", layout.getRow(edge).get(SBML.ATTR_LAYOUT_ROLE, String.class));
    }

    @Test
    void reactionGlyphWithoutSpeciesReferenceGlyphsHasTheEdgesOfTheModel() throws Exception {
        Map<String, CyNetwork> networks = byName(read("/models/layout/hsa00450_L3V1_layoutV1.xml"));
        CyNetwork layout = layoutNetworks(networks).get(0);
        CyNetwork all = allNetwork(networks);
        Map<String, Integer> glyphsOfCyId = new HashMap<>();
        for (CyNode node : layout.getNodeList()) {
            glyphsOfCyId.merge(layout.getRow(node).get(SBML.ATTR_CYID, String.class), 1, Integer::sum);
        }

        int reactionGlyphs = 0;
        for (CyNode node : layout.getNodeList()) {
            if (!SBML.NODETYPE_LAYOUT_REACTIONGLYPH.equals(
                    layout.getRow(node).get(SBML.ATTR_LAYOUT_GLYPH_TYPE, String.class))) {
                continue;
            }
            reactionGlyphs++;
            CyNode reaction = nodeByCyId(all, layout.getRow(node).get(SBML.ATTR_CYID, String.class));
            int expected = 0;
            for (CyEdge edge : all.getAdjacentEdgeList(reaction, CyEdge.Type.ANY)) {
                String type = all.getRow(edge).get(SBML.INTERACTION_ATTR, String.class);
                if (LayoutNetworkBuilder.PARTICIPANT_INTERACTIONS.contains(type)) {
                    CyNode participant = edge.getSource().equals(reaction) ? edge.getTarget() : edge.getSource();
                    expected +=
                            glyphsOfCyId.getOrDefault(all.getRow(participant).get(SBML.ATTR_CYID, String.class), 0);
                }
            }
            assertEquals(
                    expected, layout.getAdjacentEdgeList(node, CyEdge.Type.ANY).size());
        }
        assertEquals(13, reactionGlyphs);
        assertFalse(layout.getEdgeList().isEmpty());
    }

    @Test
    void transitionsWithoutGlyphAreGenerated() throws Exception {
        CyNetwork layout = layoutNetworks(byName(read("layout_01.xml"))).get(0);
        Map<String, CyNode> generated = generatedNodes(layout);

        assertFalse(generated.isEmpty());
        Set<String> types = new HashSet<>();
        for (CyNode node : generated.values()) {
            assertEquals(SBML.NODETYPE_QUAL_TRANSITION, layout.getRow(node).get(SBML.NODETYPE_ATTR, String.class));
            for (CyEdge edge : layout.getAdjacentEdgeList(node, CyEdge.Type.ANY)) {
                types.add(layout.getRow(edge).get(SBML.INTERACTION_ATTR, String.class));
            }
        }
        assertEquals(Set.of(SBML.INTERACTION_QUAL_TRANSITION_INPUT, SBML.INTERACTION_QUAL_TRANSITION_OUTPUT), types);
    }

    /** Every model source has the networks of its layouts: model definitions and the flat model. */
    @Test
    void layoutsOfModelDefinitionsAndTheFlatModel() throws Exception {
        String sbml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core" level="3" version="1"
                    xmlns:comp="http://www.sbml.org/sbml/level3/version1/comp/version1" comp:required="true"
                    xmlns:layout="http://www.sbml.org/sbml/level3/version1/layout/version1" layout:required="false">
                  <model id="top">
                    <comp:listOfSubmodels>
                      <comp:submodel comp:id="sub" comp:modelRef="definition"/>
                    </comp:listOfSubmodels>
                    <layout:listOfLayouts>
                      <layout:layout layout:id="top_layout">
                        <layout:dimensions layout:width="100" layout:height="100"/>
                      </layout:layout>
                    </layout:listOfLayouts>
                  </model>
                  <comp:listOfModelDefinitions>
                    <comp:modelDefinition id="definition">
                      <listOfCompartments>
                        <compartment id="c" constant="true"/>
                      </listOfCompartments>
                      <listOfSpecies>
                        <species id="S" compartment="c" hasOnlySubstanceUnits="false" boundaryCondition="false" constant="false"/>
                      </listOfSpecies>
                      <layout:listOfLayouts>
                        <layout:layout layout:id="definition_layout">
                          <layout:dimensions layout:width="100" layout:height="100"/>
                          <layout:listOfSpeciesGlyphs>
                            <layout:speciesGlyph layout:id="sg_S" layout:species="S">
                              <layout:boundingBox>
                                <layout:position layout:x="10" layout:y="10"/>
                                <layout:dimensions layout:width="20" layout:height="20"/>
                              </layout:boundingBox>
                            </layout:speciesGlyph>
                          </layout:listOfSpeciesGlyphs>
                        </layout:layout>
                      </layout:listOfLayouts>
                    </comp:modelDefinition>
                  </comp:listOfModelDefinitions>
                </sbml>
                """;
        SBMLReaderTask task = new SBMLReaderTask(
                new ByteArrayInputStream(sbml.strip().getBytes(StandardCharsets.UTF_8)),
                "definitions.xml",
                new NetworkTestSupport().getNetworkFactory(),
                new GroupTestSupport().getGroupFactory());
        task.run(mock(TaskMonitor.class));
        Map<String, CyNetwork> networks = byName(task.getNetworks());

        assertTrue(networks.containsKey("top__layout_top_layout"), networks.keySet()::toString);
        CyNetwork definitionLayout = networks.get("definition__layout_definition_layout");
        assertNotNull(definitionLayout, networks.keySet()::toString);
        assertEquals(Set.of("sg_S"), glyphNodes(definitionLayout).keySet());
        assertTrue(
                networks.keySet().stream()
                        .anyMatch(name -> name.startsWith(SBML.PREFIX_NETWORK_FLAT + "__top__layout_")),
                networks.keySet()::toString);
    }

    // ------------------------------------------------------------
    // support
    // ------------------------------------------------------------

    /** Reads the unit test model with the file name, or the model resource with an absolute path. */
    static CyNetwork[] read(String model) throws Exception {
        String resource = model.startsWith("/") ? model : "/models/unittests/" + model;
        String fileName = resource.substring(resource.lastIndexOf('/') + 1);
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

    /** The glyph key of every node, the SBML id for the generated nodes. */
    static Map<CyNode, String> keys(CyNetwork layout) {
        Map<CyNode, String> keys = new HashMap<>();
        for (CyNode node : layout.getNodeList()) {
            CyRow row = layout.getRow(node);
            String glyph = row.get(SBML.ATTR_LAYOUT_GLYPH, String.class);
            keys.put(node, glyph != null ? glyph : row.get(SBML.ATTR_ID, String.class));
        }
        return keys;
    }

    static CyNode nodeByCyId(CyNetwork network, String cyId) {
        return network.getNodeList().stream()
                .filter(n -> cyId.equals(network.getRow(n).get(SBML.ATTR_CYID, String.class)))
                .findFirst()
                .orElseThrow();
    }
}
