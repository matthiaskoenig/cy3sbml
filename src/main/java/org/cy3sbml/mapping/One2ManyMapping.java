package org.cy3sbml.mapping;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/** 
 * One to many mapping class.
 */
public class One2ManyMapping<T1, T2> implements Serializable{
	private static final long serialVersionUID = 1L;
	private HashMap<T1, HashSet<T2>> map;
	
	public One2ManyMapping(){
		map = new HashMap<> ();
	}
	
	public boolean containsKey(T1 key){
		return map.containsKey(key);
	}
	
	public Set<T1> keySet(){
		return map.keySet();
	}
	
	public boolean put(T1 key, T2 newValue){
		boolean valueAdded = false;
		HashSet<T2> values = getOrCreateValues(key);
		if (! values.contains(newValue)){
			values.add(newValue);
			valueAdded = true;
		}
		map.put(key, values);
		return valueAdded;
	}
	
	public void remove(T1 key){
		map.remove(key);
	}
	
	
	public HashSet<T2> getValues(T1 key){
		HashSet<T2> values; 
		if (containsKey(key)){
			values = map.get(key);
		} else {
			values = new HashSet<>();
		}
		return values;
	}
	
	public HashSet<T2> getValues(Collection<T1> keys){
		HashSet<T2> values = new HashSet<>();
		for (T1 key: keys){
			if (containsKey(key)){
				values.addAll(map.get(key));
			}
		}
		return values;
	}
	
	private HashSet<T2> getOrCreateValues(T1 key){
		HashSet<T2> values;
		if (containsKey(key)){
			values = map.get(key);
		} else {
			values = new HashSet<>();
		}
		return values;
	}

	public One2ManyMapping<T2, T1> createReverseMapping(){
		One2ManyMapping<T2, T1> reverseMapping = new One2ManyMapping<>();
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
