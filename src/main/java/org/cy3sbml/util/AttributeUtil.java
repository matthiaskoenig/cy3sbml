package org.cy3sbml.util;

import java.util.Collection;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;

/**
 * Helper class for getting and setting attribute.
 * 
 * Use these function to get and set attributes in the network, node and
 * edge tables.
 */

public class AttributeUtil {
	
	//////////////////////////////////////////////////////////////////////////
	// Set Attributes
	//////////////////////////////////////////////////////////////////////////
	/** Set attribute for CyIdentifiable. */
	public static void set(CyNetwork network, CyIdentifiable entry, String name, Object value, Class<?> type) {
		set(network, entry, CyNetwork.DEFAULT_ATTRS, name, value, type);
	}
	/** Set attribute list for CyIdentifiable. */
	public static void setList(CyNetwork network, CyIdentifiable entry, String name, Object value, Class<?> type) {
		setList(network, entry, CyNetwork.DEFAULT_ATTRS, name, value, type);
	}
	
	private static void set(CyNetwork network, CyIdentifiable entry, String tableName, String name, Object value, Class<?> type) {
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
	
	private static void setList(CyNetwork network, CyIdentifiable entry, String tableName, String name, Object value, Class<?> type) {
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

	//////////////////////////////////////////////////////////////////////////
	// Get Attributes
	//////////////////////////////////////////////////////////////////////////
	/** Get attribute for CyIdentifiable. */
	public static <T> T get(CyNetwork network, CyIdentifiable entry, String name, Class<?extends T> type) {
		return get(network, entry, CyNetwork.DEFAULT_ATTRS, name, type);
	}

	private static <T> T get(CyNetwork network, CyIdentifiable entry, String tableName, String name, Class<?extends T>  type) {
		CyRow row = network.getRow(entry, tableName);
		// CyTable table = row.getTable();
		// CyColumn column = table.getColumn(name);
		return row.get(name, type);
	}


	
	//////////////////////////////////////////////////////////////////////////
	// Clone Attributes
	//////////////////////////////////////////////////////////////////////////
	
	/** 
	 * Copy node attributes.
	 * 
	 * Gets all node attributes from the DefaultNodeTable and copies from 
	 * source to target node.
	 */
	public static void copyNodeAttributes(CyNetwork network, CyNode source, CyNode target){
		CyTable table = network.getDefaultNodeTable();
		Collection<CyColumn> columns = table.getColumns();
		for (CyColumn column : columns){
			String columnName = column.getName();
			
		 	AttributeUtil.set(network, target, 
		 			columnName,
		 			AttributeUtil.get(network, source, columnName, column.getType()), 
		 			column.getType());
		}
	}
	
	/** Copy edge attributes.
	 * 
	 * Gets all edge attributes from DefaultEdgeTable and copies from source
	 * to target edge.
	 */
	public static void copyEdgeAttributes(CyNetwork network, CyEdge source, CyEdge target){
		CyTable table = network.getDefaultEdgeTable();
		Collection<CyColumn> columns = table.getColumns();
		for (CyColumn column : columns){
			String columnName = column.getName();
			
		 	AttributeUtil.set(network, target, 
		 			columnName,
		 			AttributeUtil.get(network, source, columnName, column.getType()), 
		 			column.getType());
		}
	}
	
	
}
