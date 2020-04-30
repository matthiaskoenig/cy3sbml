package org.cy3sbml.mapping;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Testing One2ManyMapping.
 */
public class One2ManyMappingTest {

	One2ManyMapping<String, Long> map;
	
	@Before
	public void setUp(){
		map = new One2ManyMapping<String, Long>();
	}
	
	@After
	public void tearDown(){
		map = null;
	}
	
	@Test
	public void testContainsKey() {
		map.put("id1", Long.valueOf(10));
		assertTrue(map.containsKey("id1"));
		assertFalse(map.containsKey("id2"));
	}

	@Test
	public void testKeySet() {
		map.put("id1", Long.valueOf(10));
		map.put("id2", Long.valueOf(20));
		Set<String> keys = map.keySet();
		assertEquals(keys.size(), 2);
		assertTrue(keys.contains("id1"));
		assertTrue(keys.contains("id2"));
	}

	@Test
	public void testPut() {
		map.put("id1", Long.valueOf(10));
		assertTrue(map.containsKey("id1"));
		assertEquals(map.keySet().size(), 1);
	}

	@Test
	public void testRemove() {
		map.put("id1", Long.valueOf(10));
		assertTrue(map.containsKey("id1"));
		map.remove("id1");
		assertFalse(map.containsKey("id1"));
		assertEquals(map.keySet().size(), 0);
	}

	@Test
	public void testGetValues() {
		map.put("id1", Long.valueOf(10));
		map.put("id1", Long.valueOf(20));
		map.put("id1", Long.valueOf(30));
		HashSet<Long> values = map.getValues("id1");
		assertEquals(values.size(), 3);
		values = map.getValues("id2");
		assertEquals(values.size(), 0);
	}

	@Test
	public void testGetValuesListOf() {
		map.put("id1", Long.valueOf(10));
		map.put("id1", Long.valueOf(20));
		map.put("id1", Long.valueOf(30));
		map.put("id2", Long.valueOf(-10));
		map.put("id2", Long.valueOf(-20));
		map.put("id2", Long.valueOf(-30));
		
		List<String> keys = new LinkedList<String>();
		keys.add("id1");
		keys.add("id2");
		
		HashSet<Long> values = map.getValues(keys);
		assertEquals(values.size(), 6);
	}

	@Test
	public void testCreateReverseMapping() {
		map.put("id1", Long.valueOf(10));
		map.put("id1", Long.valueOf(20));
		One2ManyMapping<Long, String> revMap = map.createReverseMapping();
		assertTrue(revMap.containsKey(Long.valueOf(10)));
		assertTrue(revMap.containsKey(Long.valueOf(20)));
	}

}
