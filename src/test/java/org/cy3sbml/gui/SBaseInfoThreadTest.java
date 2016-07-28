package org.cy3sbml.gui;

import org.cy3sbml.SBMLCoreTest;
import org.cy3sbml.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import static org.junit.Assert.*;

/**
 * Testing the HTML information generation.
 */
public class SBaseInfoThreadTest {
    private SBMLDocument doc;

    @Before public void setUp() { doc = TestUtils.readSBMLDocument(SBMLCoreTest.TEST_MODEL_CORE_01);}

    @After public void tearDown() { doc = null; }

    @Test
    public void run() throws Exception {
        Model model = doc.getModel();

        // starting threads for webservice calls
        SBaseInfoThread thread = new SBaseInfoThread(objSet, this);
        thread.start();

    }

    @Test
    public void preload() throws Exception {

    }

}