package org.cy3sbml.biomodel;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.Test;

public class SearchContentTest {
	
	@Test
	public void test() {
		
		// TODO: some real world test searches and test the content
		String name = "Test";
		String person = "König, Bölling;; ,";
		String publication = "PMID:12345";
		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(SearchContent.CONTENT_NAME, name);
		map.put(SearchContent.CONTENT_PERSON, person);
		map.put(SearchContent.CONTENT_PUBLICATION, publication);
		map.put(SearchContent.CONTENT_MODE, SearchContent.CONNECT_AND);
		
		SearchContent content = new SearchContent(map);
		String info = content.toString();
		System.out.println(info);
		
	}
	
}
