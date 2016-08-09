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
 * Test all SBML files of the SBML TestCases.
 * 
 * sbml-test-suite-v3.2 (stochastic and syntactic branch)
 * https://sourceforge.net/projects/sbml/files/test-suite/
 * Retrieved on 2016-08-09.
 */
@RunWith(value = Parameterized.class)
public class SBMLTestCaseTest{
	private String resource;

    @Mock
    TaskMonitor taskMonitor;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

	public SBMLTestCaseTest(String resource) {
		this.resource = resource;
	}
	
	@Parameters(name= "{index}: {0}")
	public static Iterable<Object[]> data(){
	    /*
		HashSet<String> skip = new HashSet<>(Arrays.asList(new String[]{
				"99220-pass-00-01-sev1-l2v1.xml",
				"99220-pass-00-02-sev1-l2v2.xml",
				"99220-pass-00-03-sev1-l2v3.xml",
		}));
		*/

        HashSet<String> skip = new HashSet<>();
		String filter = "-sbml-l\\dv\\d.xml";
		return TestUtils.findResources(TestUtils.SBMLTESTCASES_RESOURCE_PATH, ".xml", filter, skip);
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
