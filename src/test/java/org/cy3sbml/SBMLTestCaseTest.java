package org.cy3sbml;

import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test all SBML files of the SBML TestCases.
 * 
 * TODO: from where the files
 * TODO: update
 * TOCO: check the skipped
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
				"10208-pass-00-08-l2v3.xml",
				"99220-pass-00-02-sev1-l2v2.xml",
				"99220-pass-00-01-sev1-l2v1.xml",
				"99220-pass-00-03-sev1-l2v3.xml",
				"20305-pass-00-11-l2v3.xml",
				"20305-pass-00-12-l2v3.xml",
				"20305-pass-00-10-l2v3.xml",
				"20301-pass-00-04-l2v3.xml",
				"20302-pass-00-04-l2v3.xml",
				"20302-pass-00-06-l2v3.xml",
				"20302-pass-00-05-l2v3.xml",
				"20303-pass-00-04-l2v3.xml",
				"20304-pass-00-06-l2v3.xml",
				"20304-pass-00-05-l2v3.xml",
				"20304-pass-00-04-l2v3.xml",
				"layout-20404-pass-00-01-sev1-l3v1.xml",
				"layout-20402-pass-00-01-sev1-l3v1.xml",
				"layout-20813-pass-00-01-sev1-l3v1.xml",
				"10204-pass-00-04-l2v3.xml",
				"10204-pass-00-13-sev1-l2v5.xml",
				"10204-pass-00-08-sev1-l3v1.xml"
		}));
		String filter = "pass";
		return TestUtils.findResources(TestUtils.SBMLTESTCASES_RESOURCE_PATH, ".xml", filter, skip);
	}
	
	@Test
	public void testSingle() throws Exception {
		TestUtils.testNetwork(getClass().getName(), resource);
	}
}
