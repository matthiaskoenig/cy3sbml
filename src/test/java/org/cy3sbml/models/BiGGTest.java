package org.cy3sbml.models;

import java.util.HashSet;

import org.cy3sbml.TestUtils;
import org.cytoscape.work.TaskMonitor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test cases for the BIGG models.
 * bigg_models v1.5 (https://github.com/SBRG/bigg_models/releases)
 * 
 * Models were retrieved on 2019-04-17 from the available database dumps on
 * dropbox: https://www.dropbox.com/sh/yayfmcrsrtrcypw/AACDoew92pCYlSJa8vCs5rSMa?dl=0
 */
@RunWith(value = Parameterized.class)
public class BiGGTest {
	private String resource;

	@Mock
	TaskMonitor taskMonitor;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}
	
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
		TestUtils.testNetwork(taskMonitor, getClass().getName(), resource);
	}

	@Test
    public void testSerialization() throws Exception {
        TestUtils.testNetworkSerialization(getClass().getName(), resource);
    }

}