package org.cy3sbml.util;

import java.util.Collection;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Getting and setting the attributes (columns) of the nodes, edges and networks in the
 * default tables of a network.
 */
public class AttributeUtil {
    private static final Logger logger = LoggerFactory.getLogger(AttributeUtil.class);

    // ------------------------------------------------------------
    // Set Attributes
    // ------------------------------------------------------------
    /**
     * Sets the attribute of the node, edge or network, creating the column if needed.
     * A null value is not set.
     *
     * @param network the network
     * @param entry the node, edge or network
     * @param name the column name
     * @param value the value, may be null
     * @param type the column type
     */
    public static void set(CyNetwork network, CyIdentifiable entry, String name, Object value, Class<?> type) {
        set(network, entry, CyNetwork.DEFAULT_ATTRS, name, value, type);
    }

    /**
     * Sets the list attribute of the node, edge or network, creating the list column if
     * needed. A null value is not set.
     *
     * @param network the network
     * @param entry the node, edge or network
     * @param name the column name
     * @param value the list, may be null
     * @param type the type of the list elements
     */
    public static void setList(CyNetwork network, CyIdentifiable entry, String name, Object value, Class<?> type) {
        setList(network, entry, CyNetwork.DEFAULT_ATTRS, name, value, type);
    }

    private static void set(
            CyNetwork network, CyIdentifiable entry, String tableName, String name, Object value, Class<?> type) {
        CyRow row = network.getRow(entry, tableName);
        CyTable table = row.getTable();
        CyColumn column = table.getColumn(name);
        if (value != null) {
            if (column == null) {
                table.createColumn(name, type, false);
            }
            row.set(name, value);
        }
    }

    private static void setList(
            CyNetwork network, CyIdentifiable entry, String tableName, String name, Object value, Class<?> type) {
        CyRow row = network.getRow(entry, tableName);
        CyTable table = row.getTable();
        CyColumn column = table.getColumn(name);
        if (value != null) {
            if (column == null) {
                table.createListColumn(name, type, false);
            }
            row.set(name, value);
        }
    }

    // ------------------------------------------------------------
    // Get Attributes
    // ------------------------------------------------------------
    /**
     * The attribute of the node, edge or network.
     *
     * @param network the network
     * @param entry the node, edge or network
     * @param name the column name
     * @param type the column type
     * @param <T> the value type
     * @return the value, null if not set
     */
    public static <T> T get(CyNetwork network, CyIdentifiable entry, String name, Class<? extends T> type) {
        return get(network, entry, CyNetwork.DEFAULT_ATTRS, name, type);
    }

    private static <T> T get(
            CyNetwork network, CyIdentifiable entry, String tableName, String name, Class<? extends T> type) {
        CyRow row = network.getRow(entry, tableName);
        return row.get(name, type);
    }

    // ------------------------------------------------------------
    // Clone Attributes
    // ------------------------------------------------------------

    /**
     * Copies the attributes of the default node table from the source to the target node,
     * except the SUID.
     *
     * @param network the network of the nodes
     * @param source the source node
     * @param target the target node
     */
    public static void copyNodeAttributes(CyNetwork network, CyNode source, CyNode target) {
        CyTable table = network.getDefaultNodeTable();
        Collection<CyColumn> columns = table.getColumns();
        for (CyColumn column : columns) {
            if (column.isPrimaryKey()) {
                continue;
            }
            String columnName = column.getName();

            AttributeUtil.set(
                    network,
                    target,
                    columnName,
                    AttributeUtil.get(network, source, columnName, column.getType()),
                    column.getType());
        }
    }

    /**
     * Copies the attributes of the default edge table from the source to the target edge,
     * except the SUID.
     *
     * @param network the network of the edges
     * @param source the source edge
     * @param target the target edge
     */
    public static void copyEdgeAttributes(CyNetwork network, CyEdge source, CyEdge target) {
        CyTable table = network.getDefaultEdgeTable();
        Collection<CyColumn> columns = table.getColumns();
        for (CyColumn column : columns) {
            if (column.isPrimaryKey()) {
                continue;
            }
            String columnName = column.getName();

            AttributeUtil.set(
                    network,
                    target,
                    columnName,
                    AttributeUtil.get(network, source, columnName, column.getType()),
                    column.getType());
        }
    }

    // ------------------------------------------------------------
    // Find Nodes
    // ------------------------------------------------------------

    /**
     * The first node with the value of the attribute in the default node table.
     *
     * @param network    network in which the node is searched
     * @param attribute  attribute column to search
     * @param identifier identifier to search
     * @return the node, null if no node has the value
     */
    public static CyNode getNodeByAttribute(CyNetwork network, String attribute, String identifier) {
        Collection<CyRow> rows = network.getDefaultNodeTable().getMatchingRows(attribute, identifier);
        CyNode node = null;
        if (rows != null && !rows.isEmpty()) {
            // return first matching one
            CyRow row = rows.iterator().next();
            node = network.getNode(row.get(CyTable.SUID, Long.class));
        } else {
            logger.debug("node not in network: {}:{}", attribute, identifier);
        }
        return node;
    }
}
