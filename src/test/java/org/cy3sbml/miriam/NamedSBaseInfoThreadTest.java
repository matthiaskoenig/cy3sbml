package org.cy3sbml.miriam;

import static org.junit.Assert.*;

import org.junit.Test;

public class NamedSBaseInfoThreadTest {

	@Test
	public void testStreamToString() {
	   assertNotNull("Test file missing", 
	               getClass().getResource("/sbml/Koenig_demo_v02.xml"));
	
	}
	
	
	@Test
	public void testPreloadAnnotationsForSBMLDocument() {
		// Load an SBML
		
		
		// preload all the Miriam information
	}

}
