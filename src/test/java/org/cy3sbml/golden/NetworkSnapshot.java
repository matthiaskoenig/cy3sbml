package org.cy3sbml.golden;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.cy3sbml.SBML;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CySubNetwork;

/**
 * Canonical JSON snapshot of the networks created by the SBML reader.
 * <p>
 * The snapshot contains every column of the default network, node and edge tables,
 * except the columns in {@link #EXCLUDED_COLUMNS}. These hold SUIDs or selection
 * state, which differ between runs and carry no information about the SBML import:
 * <ul>
 * <li>{@code SUID}: the Cytoscape session unique id of the row.</li>
 * <li>{@code selected}: the selection state.</li>
 * </ul>
 * <p>
 * The snapshot is independent of SUIDs, hash ordering and the platform:
 * <ul>
 * <li>networks are sorted by their {@code name} column,</li>
 * <li>nodes are sorted by their key ({@link SBML#ATTR_ID}, falling back to
 * {@link SBML#ATTR_METAID}, then {@code shared name}, then the attribute JSON) and
 * then by their attribute JSON,</li>
 * <li>edges reference their source and target by the node key and are sorted by
 * their JSON,</li>
 * <li>{@code Double} values are rounded to 12 significant digits, non-finite
 * values are written as strings.</li>
 * </ul>
 * List values keep their order: the only list column,
 * {@link SBML#ATTR_QUAL_RESULT_LEVELS}, holds the result levels in the document
 * order of the function terms, which is deterministic.
 */
public final class NetworkSnapshot {

    /** Columns that hold SUIDs or selection state and are left out of the snapshot. */
    public static final Set<String> EXCLUDED_COLUMNS = Set.of(CyIdentifiable.SUID, CyNetwork.SELECTED);

    private static final JsonNodeFactory FACTORY = JsonNodeFactory.instance;
    private static final MathContext PRECISION = new MathContext(12);

    private NetworkSnapshot() {}

    /**
     * Create the canonical snapshot of the given networks.
     */
    public static ObjectNode of(CyNetwork[] networks) {
        List<CyNetwork> sorted = new ArrayList<>(Arrays.asList(networks));
        sorted.sort(Comparator.comparing(NetworkSnapshot::networkName));

        Map<CyNode, String> nodeKeys = new HashMap<>();
        ArrayNode networkArray = FACTORY.arrayNode();
        for (CyNetwork network : sorted) {
            networkArray.add(networkSnapshot(network, nodeKeys));
        }
        ObjectNode snapshot = FACTORY.objectNode();
        snapshot.set("networks", networkArray);
        snapshot.set("groups", groupsSnapshot(sorted, nodeKeys));
        return snapshot;
    }

    /**
     * Group nodes of the root networks with their attributes and member keys.
     * <p>
     * Group nodes live in the root network only, their members are the nodes of
     * the network the group node points to.
     */
    private static ArrayNode groupsSnapshot(List<CyNetwork> networks, Map<CyNode, String> nodeKeys) {
        Set<CyRootNetwork> roots = new LinkedHashSet<>();
        for (CyNetwork network : networks) {
            roots.add(((CySubNetwork) network).getRootNetwork());
        }
        List<ObjectNode> groups = new ArrayList<>();
        for (CyRootNetwork root : roots) {
            for (CyNode node : root.getNodeList()) {
                CyNetwork pointer = node.getNetworkPointer();
                if (pointer == null) {
                    continue;
                }
                List<String> members = new ArrayList<>();
                for (CyNode member : pointer.getNodeList()) {
                    members.add(nodeKeys.getOrDefault(member, "<not in networks>"));
                }
                Collections.sort(members);
                ObjectNode group = FACTORY.objectNode();
                group.put("root", networkName(root));
                group.set("attributes", attributes(root.getDefaultNodeTable(), root.getRow(node)));
                ArrayNode memberArray = group.putArray("members");
                members.forEach(memberArray::add);
                groups.add(group);
            }
        }
        groups.sort(Comparator.comparing(NetworkSnapshot::json));
        return FACTORY.arrayNode().addAll(groups);
    }

    private static String networkName(CyNetwork network) {
        String name = network.getRow(network).get(CyNetwork.NAME, String.class);
        return name == null ? "" : name;
    }

    private static ObjectNode networkSnapshot(CyNetwork network, Map<CyNode, String> nodeKeys) {
        ObjectNode result = FACTORY.objectNode();
        result.put("name", networkName(network));
        result.put("nodeCount", network.getNodeCount());
        result.put("edgeCount", network.getEdgeCount());
        result.set("attributes", attributes(network.getDefaultNetworkTable(), network.getRow(network)));

        List<ObjectNode> nodes = new ArrayList<>();
        for (CyNode node : network.getNodeList()) {
            ObjectNode attributes = attributes(network.getDefaultNodeTable(), network.getRow(node));
            nodeKeys.put(node, nodeKey(attributes));
            nodes.add(attributes);
        }
        nodes.sort(Comparator.comparing(NetworkSnapshot::nodeKey).thenComparing(NetworkSnapshot::json));
        result.set("nodes", FACTORY.arrayNode().addAll(nodes));

        List<ObjectNode> edges = new ArrayList<>();
        for (CyEdge edge : network.getEdgeList()) {
            ObjectNode attributes = attributes(network.getDefaultEdgeTable(), network.getRow(edge));
            ObjectNode edgeNode = FACTORY.objectNode();
            edgeNode.put("source", nodeKeys.get(edge.getSource()));
            edgeNode.put("target", nodeKeys.get(edge.getTarget()));
            edgeNode.set("interaction", attributes.get(SBML.INTERACTION_ATTR));
            edgeNode.set("attributes", attributes);
            edges.add(edgeNode);
        }
        edges.sort(Comparator.comparing(NetworkSnapshot::json));
        result.set("edges", FACTORY.arrayNode().addAll(edges));
        return result;
    }

    /**
     * Key of a node: its SBML id, else its metaId, else its shared name, else its attribute JSON.
     */
    private static String nodeKey(ObjectNode attributes) {
        for (String column : List.of(SBML.ATTR_ID, SBML.ATTR_METAID, CyRootNetwork.SHARED_NAME)) {
            JsonNode value = attributes.get(column);
            if (value != null && value.isTextual()) {
                return value.asText();
            }
        }
        return json(attributes);
    }

    /**
     * All non-null values of the row, keyed by column name.
     */
    private static ObjectNode attributes(CyTable table, CyRow row) {
        Map<String, JsonNode> values = new TreeMap<>();
        for (CyColumn column : table.getColumns()) {
            String name = column.getName();
            Object value = row.getRaw(name);
            if (value != null && !EXCLUDED_COLUMNS.contains(name)) {
                values.put(name, toJson(value));
            }
        }
        ObjectNode result = FACTORY.objectNode();
        result.setAll(values);
        return result;
    }

    private static JsonNode toJson(Object value) {
        if (value instanceof List<?> list) {
            ArrayNode array = FACTORY.arrayNode();
            for (Object item : list) {
                array.add(toJson(item));
            }
            return array;
        }
        if (value instanceof Double d) {
            if (!Double.isFinite(d)) {
                return FACTORY.textNode(d.toString());
            }
            return FACTORY.numberNode(new BigDecimal(d).round(PRECISION).doubleValue());
        }
        if (value instanceof Integer i) {
            return FACTORY.numberNode(i);
        }
        if (value instanceof Long l) {
            return FACTORY.numberNode(l);
        }
        if (value instanceof Boolean b) {
            return FACTORY.booleanNode(b);
        }
        if (value instanceof String s) {
            return FACTORY.textNode(s);
        }
        throw new IllegalArgumentException("Unsupported column value type: " + value.getClass());
    }

    /**
     * Compact JSON, used as a stable sort key. Attribute keys are inserted in sorted order.
     */
    private static String json(JsonNode node) {
        return node.toString();
    }
}
