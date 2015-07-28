package org.cy3sbml.miriam;

import static org.junit.Assert.*;

import org.cy3sbml.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import uk.ac.ebi.miriam.lib.MiriamLink;


public class MiriamWebserviceTest {
	MiriamLink link;
	
	@BeforeClass 
	public static void onlyOnce() {
	       TestUtils.setSystemProxyForTests();
	}
	
	@Before
	public void setUp() {
		link = MiriamWebservice.getMiriamLink();		
	}
	
	@After
	public void tearDown() {
		link = null;
	}
	
	@Test
	public void testGetMiriamLink() {	
		assertNotNull(link);
	}
	
	@Test
	public void testGetChEBI() {
		String dataType = link.getDataTypeURI("ChEBI");
		assertNotNull(dataType);
	}
}
