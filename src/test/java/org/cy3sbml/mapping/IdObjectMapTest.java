package org.cy3sbml.mapping;

import org.cy3sbml.*;
import org.cy3sbml.util.SBMLUtil;
import org.junit.Test;
import org.sbml.jsbml.SBMLDocument;

import static org.junit.Assert.*;

/**
 * Test the different models.
 */
public class IdObjectMapTest {

    @Test
    public void testCoreNetwork_01() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLCoreTest.TEST_MODEL_CORE_01);
        IdObjectMap map = new IdObjectMap(doc);
        assertNotNull(map);
    }

    @Test
    public void testCoreNetwork_02() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLCoreTest.TEST_MODEL_CORE_02);
        IdObjectMap map = new IdObjectMap(doc);
        assertNotNull(map);
    }

    @Test
    public void testCoreNetwork_03() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLCoreTest.TEST_MODEL_CORE_03);
        IdObjectMap map = new IdObjectMap(doc);
        assertNotNull(map);
    }

    @Test
    public void testCompNetwork_01() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLCompTest.TEST_MODEL_COMP_01);
        IdObjectMap map = new IdObjectMap(doc);
        assertNotNull(map);
    }

    @Test
    public void testCompNetwork_02() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLCompTest.TEST_MODEL_COMP_02);
        IdObjectMap map = new IdObjectMap(doc);
        assertNotNull(map);
    }

    @Test
    public void testFbcNetwork_01() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLFbcTest.TEST_MODEL_FBC);
        IdObjectMap map = new IdObjectMap(doc);
        assertNotNull(map);
    }

    @Test
    public void testGroupsNetwork_01() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLGroupsTest.TEST_MODEL_GROUPS);
        IdObjectMap map = new IdObjectMap(doc);
        assertNotNull(map);
    }

    @Test
    public void testLayoutNetwork_01() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLLayoutTest.TEST_MODEL_LAYOUT);
        IdObjectMap map = new IdObjectMap(doc);
        assertNotNull(map);
    }

    @Test
    public void testQualNetwork_01() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLQualTest.TEST_MODEL_QUAL);
        IdObjectMap map = new IdObjectMap(doc);
        assertNotNull(map);
    }

}