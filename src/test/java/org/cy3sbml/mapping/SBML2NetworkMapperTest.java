package org.cy3sbml.mapping;

import static org.junit.Assert.*;

import java.io.FileReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

public class SBML2NetworkMapperTest {

	SBML2NetworkMapper mapper;
	
	@Before
	public void setUp() {
		mapper = new SBML2NetworkMapper();
		
		SBMLDocument doc = SBMLReader.read("./src/test/resources/sbml/small.xml");
		CyNetwork network = null;
		OneToManyMapping mapping = new NamedSBaseToNodeMapping(network);
				
	}
	
	@After
	public void tearDown() {
		mapper = null;
	}
	
	
	@Test
	public void testSetCurrent() {
		fail("Not yet implemented");
	}

	@Test
	public void testKeySet() {
		fail("Not yet implemented");
	}

	@Test
	public void testPutDocument() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveDocument() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCurrentDocument() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCurrentNodeToNSBMapping() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCurrentNSBToNodeMapping() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetDocumentForSUID() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetDocumentMap() {
		fail("Not yet implemented");
	}

}
