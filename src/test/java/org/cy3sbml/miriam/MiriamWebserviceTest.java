package org.cy3sbml.miriam;

import static org.junit.Assert.*;

import org.junit.Test;

import uk.ac.ebi.miriam.lib.MiriamLink;

public class MiriamWebserviceTest {

	@Test
	public void testGetMiriamLink() {
		MiriamLink link = MiriamWebservice.getMiriamLink();
		assertNotNull(link);
	}
	
	@Test
	public void testGetChEBI() {
		MiriamLink link = MiriamWebservice.getMiriamLink();
		String dataType = link.getDataTypeURI("ChEBI");
		System.out.println(dataType);
		assertNotNull(dataType);
	}
}
