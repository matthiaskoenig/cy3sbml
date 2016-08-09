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
 * Test cases for biomodels.
 * 
 * 30th BioModels Release, 2016-05-10
 * http://www.ebi.ac.uk/biomodels-main/static-pages.do?page=release_20160510
 * Retrieved on 2016-06-22, 611 curated models
 */
@RunWith(value = Parameterized.class)
public class BioModelsTest {
	private String resource;

	@Mock
	TaskMonitor taskMonitor;

	@Before
	public void setUp() {
		MockitoAnnotations.initMocks(this);
	}

	public BioModelsTest(String resource) {
		this.resource = resource;
	}
	
	@Parameters(name= "{index}: {0}")
	public static Iterable<Object[]> data(){
		HashSet<String> skip = null;
		String filter = null;
		return TestUtils.findResources(TestUtils.BIOMODELS_RESOURCE_PATH, ".xml", filter, skip);
	}
	
	@Test
	public void testSingle() throws Exception {
		TestUtils.testNetwork(taskMonitor, getClass().getName(), resource);
	}
}
