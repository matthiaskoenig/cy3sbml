package org.cy3sbml.models;

import java.util.Arrays;
import java.util.HashSet;

import org.cy3sbml.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test all SBML files of the SBML TestCases.
 * 
 * sbml-test-cases v3.1.1
 * https://sourceforge.net/projects/sbml/files/test-suite/
 */
@RunWith(value = Parameterized.class)
public class SBMLTestCaseTest{
	private String resource;
	
	public SBMLTestCaseTest(String resource) {
		this.resource = resource;
	}
	
	@Parameters(name= "{index}: {0}")
	public static Iterable<Object[]> data(){
		HashSet<String> skip = new HashSet<String>(Arrays.asList(new String[]{
				"99220-pass-00-01-sev1-l2v1.xml",
				"99220-pass-00-02-sev1-l2v2.xml",
				"99220-pass-00-03-sev1-l2v3.xml",
		}));
		String filter = "pass";
		return TestUtils.findResources(TestUtils.SBMLTESTCASES_RESOURCE_PATH, ".xml", filter, skip);
	}
	
	@Test
	public void testSingle() throws Exception {
		TestUtils.testNetwork(getClass().getName(), resource);
	}
}
