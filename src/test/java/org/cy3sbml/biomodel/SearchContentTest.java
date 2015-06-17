package org.cy3sbml.biomodel;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

public class SearchContentTest {
	
	@Test
	public void testByName() {	
		String name = "Test";
		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(SearchContent.CONTENT_NAME, name);
		map.put(SearchContent.CONTENT_MODE, SearchContent.CONNECT_AND);
		
		SearchContent content = new SearchContent(map);
		assertTrue(content.hasNames());
		assertFalse(content.hasPublications());
	}
	
	@Test
	public void testByPerson() {
		String person = "König, Bölling";
		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(SearchContent.CONTENT_PERSON, person);
		map.put(SearchContent.CONTENT_MODE, SearchContent.CONNECT_AND);
		
		SearchContent content = new SearchContent(map);
		assertTrue(content.hasPersons());
		assertFalse(content.hasPublications());
	}
	
	@Test
	public void testByPublication() {
		// HepatoNet1
		String publication = "PMID:20823849";
		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(SearchContent.CONTENT_PUBLICATION, publication);
		map.put(SearchContent.CONTENT_MODE, SearchContent.CONNECT_AND);
		
		SearchContent content = new SearchContent(map);
		assertTrue(content.hasPublications());
		assertFalse(content.hasChebis());
	}
	
	@Test
	public void testCombination() {
		String name = "Test";
		String person = "König, Bölling;; ,";
		String publication = "PMID:12345";
		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(SearchContent.CONTENT_NAME, name);
		map.put(SearchContent.CONTENT_PERSON, person);
		map.put(SearchContent.CONTENT_PUBLICATION, publication);
		map.put(SearchContent.CONTENT_MODE, SearchContent.CONNECT_AND);
		
		SearchContent content = new SearchContent(map);
		assertTrue(content.hasNames());
		assertTrue(content.hasPersons());
		assertTrue(content.hasPublications());
	}
}
