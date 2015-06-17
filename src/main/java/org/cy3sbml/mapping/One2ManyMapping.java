package org.cy3sbml.mapping;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/** 
 * Simple one 2 many mapping class.
 */
public class One2ManyMapping<T1, T2> {	
	private HashMap<T1, List<T2>> map;
	
	public One2ManyMapping(){
		map = new HashMap<T1, List<T2>> ();
	}
	
	public boolean containsKey(T1 key){
		return map.containsKey(key);
	}
	
	public Set<T1> keySet(){
		return map.keySet();
	}
	
	public boolean put(T1 key, T2 newValue){
		boolean valueAdded = false;
		List<T2> values = getOrCreateValues(key);
		if (! values.contains(newValue)){
			values.add(newValue);
			valueAdded = true;
		}
		map.put(key, values);
		return valueAdded;
	}
	
	public List<T2> getValues(T1 key){
		List<T2> values; 
		if (containsKey(key)){
			values = map.get(key);
		} else {
			values = new LinkedList<T2>();
		}
		return values;
	}
	
	public List<T2> getValues(List<T1> keys){
		List<T2> values = new LinkedList<T2>();
		for (T1 key: keys){
			if (containsKey(key)){
				values.addAll(map.get(key));
			}
		}
		return values;
	}
	
	private List<T2> getOrCreateValues(T1 key){
		List<T2> values;
		if (containsKey(key)){
			values = map.get(key);
		} else {
			values = new LinkedList<T2>();
		}
		return values;
	}

	public One2ManyMapping<T2, T1> createReverseMapping(){
		One2ManyMapping<T2, T1> reverseMapping = new One2ManyMapping<T2,T1>();
		for (T1 key: this.keySet()){
			for (T2 value : this.getValues(key)){
				reverseMapping.put(value, key);
			}
		}
		return reverseMapping;
	}

	public String toString(){
		String info = "*** OneToManyMapping ***\n";
		for (T1 key: keySet()){
			info += String.format("%s -> %s\n", key.toString(), map.get(key).toString());
		}
		info += "************************";
		return info;
	}
}
