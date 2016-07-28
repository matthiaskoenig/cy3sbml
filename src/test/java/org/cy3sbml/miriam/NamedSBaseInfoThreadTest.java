package org.cy3sbml.miriam;

import java.io.InputStream;

import javax.xml.stream.XMLStreamException;

import org.cy3sbml.gui.SBaseInfoThread;
import org.junit.Test;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

import org.cy3sbml.TestUtils;

public class NamedSBaseInfoThreadTest {
	public static final String TEST_MODEL = TestUtils.UNITTESTS_RESOURCE_PATH + "/" + "core_01.xml";
	
	@Test
	public void testPreloadAnnotationsForSBMLDocument() throws XMLStreamException {
		// Load an SBML
		InputStream is = getClass().getResourceAsStream(TEST_MODEL);
		SBMLDocument document = SBMLReader.read(is);
		
		// TODO: this is not testing properly due to the multiple started threads
		// TODO: fix the tests
		
		// preload all the Miriam information
		SBaseInfoThread.preload(document);
		
		// Second time should just lookup in cache
		SBaseInfoThread.preload(document);
	}

}
