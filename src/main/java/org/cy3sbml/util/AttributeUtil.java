package org.cy3sbml.util;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;

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
	public static void set(CyNetwork network, CyIdentifiable entry, String name, Object value, Class<?> type) {
		set(network, entry, CyNetwork.DEFAULT_ATTRS, name, value, type);
	}
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
	public static <T> T get(CyNetwork network, CyIdentifiable entry, String name, Class<?extends T> type) {
		return get(network, entry, CyNetwork.DEFAULT_ATTRS, name, type);
	}

	private static <T> T get(CyNetwork network, CyIdentifiable entry, String tableName, String name, Class<?extends T>  type) {
		CyRow row = network.getRow(entry, tableName);
		// CyTable table = row.getTable();
		// CyColumn column = table.getColumn(name);
		return row.get(name, type);
	}
	
}
