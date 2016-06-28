package org.cy3sbml;

import java.util.HashSet;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test cases for the BIGG models.
 * bigg_models v1.2 (https://github.com/SBRG/bigg_models/releases)
 * 
 * Models were retrieved on 2016-06-22 from the available database dumps on
 * https://www.dropbox.com/sh/yayfmcrsrtrcypw/AACDoew92pCYlSJa8vCs5rSMa?dl=0
 */
@RunWith(value = Parameterized.class)
public class BiGGTest {
	private String resource;
	
	public BiGGTest(String resource) {
		this.resource = resource;
	}
	
	@Parameters(name= "{index}: {0}")
	public static Iterable<Object[]> data(){
		HashSet<String> skip = null;
		String filter = null;
		return TestUtils.findResources(TestUtils.BIGGMODELS_RESOURCE_PATH, ".xml", filter, skip);
	}
	
	@Test
	public void testSingle() throws Exception {
		TestUtils.testNetwork(getClass().getName(), resource);
	}
}