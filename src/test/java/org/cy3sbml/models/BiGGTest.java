package org.cy3sbml.models;

import java.util.HashSet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.cy3sbml.TestUtils;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Test cases for the BIGG models.
 * bigg_models v1.5 (https://github.com/SBRG/bigg_models/releases)
 *
 * Models were retrieved on 2019-04-17 from the available database dumps on
 * dropbox: https://www.dropbox.com/sh/yayfmcrsrtrcypw/AACDoew92pCYlSJa8vCs5rSMa?dl=0
 */
public class BiGGTest {

//	@Mock
//	TaskMonitor taskMonitor;
//
//	@BeforeEach
//	public void setUp() {
//		MockitoAnnotations.openMocks(this);
//	}
//
//	static Stream<String> biggModelResources() {
//		HashSet<String> skip = null;
//		String filter = null;
//		return StreamSupport.stream(
//				TestUtils.findResources(TestUtils.BIGGMODELS_RESOURCE_PATH, ".xml", filter, skip).spliterator(),
//				false
//		).map(arr -> arr[0].toString());
//	}
//
//	@ParameterizedTest(name = "{index}: {0}")
//	@MethodSource("biggModelResources")
//	void testSingle(String resource) throws Exception {
//		TestUtils.testNetwork(taskMonitor, getClass().getName(), resource);
//	}
//
//	@ParameterizedTest(name = "{index}: {0}")
//	@MethodSource("biggModelResources")
//	void testSerialization(String resource) throws Exception {
//		TestUtils.testNetworkSerialization(getClass().getName(), resource);
//	}
}