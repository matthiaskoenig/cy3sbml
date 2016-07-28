package org.cy3sbml.gui;

import org.cy3sbml.SBMLCoreTest;
import org.cy3sbml.TestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import java.util.Collection;
import java.util.HashSet;

import static junit.framework.TestCase.assertNotNull;

/**
 * Testing the HTML information generation.
 * TODO: create a test mock for the panel
 */
public class SBaseInfoThreadTest {
    private SBMLDocument doc;

    @Before public void setUp() { doc = TestUtils.readSBMLDocument(SBMLCoreTest.TEST_MODEL_CORE_01);}

    @After public void tearDown() { doc = null; }

    @Test
    public void run() throws Exception {
        Model model = doc.getModel();

        Collection<Object> objSet = new HashSet<Object>();
        objSet.add(model);
        // starting threads for webservice calls
        SBaseHTMLThread thread = new SBaseHTMLThread(objSet, null);
        thread.start();
        thread.join();
        String html = thread.getInfo();
        assertNotNull(html);
    }

    @Test
    public void preload() throws Exception {
        // preload all the Miriam information
        SBaseHTMLThread.preload(doc);

        // Second time should just lookup in cache
        SBaseHTMLThread.preload(doc);
    }
}