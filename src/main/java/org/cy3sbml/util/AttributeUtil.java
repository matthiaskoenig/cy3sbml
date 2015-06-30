package org.cy3sbml.util;


import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;

public class AttributeUtil {
	
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
	
	
	// TODO: ? figure out how to implement the generic get function
	
	public static Class<?> get(CyNetwork network, CyIdentifiable entry, String name, Class<?> type) {
		return get(network, entry, CyNetwork.DEFAULT_ATTRS, name, type);
	}

	private static Class<?> get(CyNetwork network, CyIdentifiable entry, String tableName, String name, Class<?> type) {
		CyRow row = network.getRow(entry, tableName);
		// CyTable table = row.getTable();
		// CyColumn column = table.getColumn(name);
		return (Class<?>) row.get(name, type);
	}
	
}
