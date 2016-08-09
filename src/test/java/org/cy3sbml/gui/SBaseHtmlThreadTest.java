package org.cy3sbml.gui;

import java.util.Collection;
import java.util.HashSet;

import org.cy3sbml.mapping.IdObjectMap;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

import org.cy3sbml.SBMLCoreTest;
import org.cy3sbml.SBMLQualTest;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.cy3sbml.util.SBMLUtil;
import org.sbml.jsbml.SBase;

/**
 * Testing the HTML information generation.
 *
 * A mock for the panel is created to simplify testing.
 * http://www.vogella.com/tutorials/Mockito/article.html
 */
public class SBaseHtmlThreadTest {
    @Mock
    SBMLPanel panel;

    @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Test
    public void run() throws Exception {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLCoreTest.TEST_MODEL_CORE_01);
        Model model = doc.getModel();

        Collection<Object> objSet = new HashSet<>();
        objSet.add(model);
        // starting threads for webservice calls
        SBaseHTMLThread thread = new SBaseHTMLThread(objSet, panel);
        thread.start();
        thread.join();
        String html = thread.getInfo();
        assertNotNull(html);
    }

    @Test
    public void runCoreTest1() throws Exception{
        String resource = SBMLCoreTest.TEST_MODEL_CORE_01;
        SBMLDocument doc = SBMLUtil.readSBMLDocument(resource);
        Model model = doc.getModel();

        // objects from model
        IdObjectMap map = new IdObjectMap(doc);
        Collection<SBase> objects = map.getObjects();
        for (SBase sbase : objects){
            Collection<Object> objCollection = new HashSet<>();
            objCollection.add(sbase);
            SBaseHTMLThread t1 = new SBaseHTMLThread(objCollection, panel);
            t1.start();
            t1.join();
            String html = t1.getInfo();
            assertNotNull(html);
        }
    }


    private String createHTMLOutput(String resource) throws Exception{
        SBMLDocument doc = SBMLUtil.readSBMLDocument(resource);
        Model model = doc.getModel();

        Collection<Object> objSet = new HashSet<>();
        objSet.add(model);

        // running in caching mode, no html generated
        SBaseHTMLThread t1 = new SBaseHTMLThread(objSet, panel);
        t1.start();
        t1.join();
        String html = t1.getInfo();
        return html;
    }




    /////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Writing HTML information to file for development.
     * This allows faster development cycle of the information HTML than
     * packing it in the Cytoscape app.
     */
    public static void main(String[] args) throws Exception{
        String resource = SBMLCoreTest.TEST_MODEL_CORE_01;
        SBaseHtmlThreadTest test = new SBaseHtmlThreadTest();
        String html = test.createHTMLOutput(resource);
        System.out.println(html);
    }
}