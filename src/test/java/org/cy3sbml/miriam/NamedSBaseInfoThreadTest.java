package org.cy3sbml.miriam;

import static org.junit.Assert.*;

import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

public class NamedSBaseInfoThreadTest {

	@Test
	public void testStreamToString() {
	   assertNotNull("Test file missing", 
	               getClass().getResource("/models/BIOMD0000000001.xml"));
	}
	
	@Test
	public void testPreloadAnnotationsForSBMLDocument() throws XMLStreamException {
		// Load an SBML
		InputStream is = getClass().getResourceAsStream("/models/BIOMD0000000001.xml");
		SBMLDocument document = SBMLReader.read(is);
		
		// TODO: this is not testing properly due to the multiple started threads
		// TODO: fix the tests
		
		// preload all the Miriam information
		NamedSBaseInfoThread.preloadAnnotationsForSBMLDocument(document);
		
		// Second time should just lookup in cache
		NamedSBaseInfoThread.preloadAnnotationsForSBMLDocument(document);
		
	}

}
