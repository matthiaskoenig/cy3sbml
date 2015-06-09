package org.cy3sbml.mapping;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class OneToManyMapping {
	private HashMap<String, List<String>> map;
	
	public OneToManyMapping(){
		map = new HashMap<String, List<String>> ();
	}
	
	public boolean containsKey(String key){
		return map.containsKey(key);
	}
	
	public Set<String> keySet(){
		return map.keySet();
	}
	
	public boolean put(String key, String newValue){
		boolean valueAdded = true;
		List<String> values = getOrCreateValues(key);
		if (! values.contains(newValue)){
			values.add(newValue);
			valueAdded = true;
		}
		map.put(key, values);
		return valueAdded;
	}
	
	public List<String> getValues(String key){
		List<String> values; 
		if (containsKey(key)){
			values = map.get(key);
		} else {
			values = new LinkedList<String>();
		}
		return values;
	}
	
	public List<String> getValues(List<String> keys){
		List<String> values = new LinkedList<String>();
		for (String key: keys){
			if (containsKey(key)){
				values.addAll(map.get(key));
			}	
		}
		return values;
	}
	
	private List<String> getOrCreateValues(String key){
		List<String> values;
		if (containsKey(key)){
			values = map.get(key);
		} else {
			values = new LinkedList<String>();
		}
		return values;
	}

	public static OneToManyMapping createReverseMapping(OneToManyMapping mapping) {
		OneToManyMapping reverseMapping = new OneToManyMapping();
		for (String key: mapping.keySet()){
			for (String value : mapping.getValues(key)){
				reverseMapping.put(value, key);
			}
		}
		return reverseMapping;
	}
	
	public String toString(){
		String info = "*** OneToManyMapping ***\n";
		for (String key: keySet()){
			info += String.format("%s -> %s\n", key, map.get(key));
		}
		info += "************************";
		return info;
	}
}
