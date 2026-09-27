package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import javax.swing.tree.TreeNode;
import org.cy3sbml.*;
import org.cy3sbml.chebi.ChebiAccess;
import org.cy3sbml.miriam.MiriamRegistry;
import org.cy3sbml.ols.OlsClient;
import org.cy3sbml.uniprot.UniprotAccess;
import org.cy3sbml.util.HttpJson;
import org.cy3sbml.util.SBMLUtil;
import org.cy3sbml.util.filter.SBaseFilter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;

/**
 * Testing the HTML information generation.
 * <p>
 * A mock for the panel is created to simplify testing.
 * http://www.vogella.com/tutorials/Mockito/article.html
 */
@Tag("network")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SBaseHtmlThreadTest {
    @Mock
    InfoPanel panel;

    private static SBaseHTMLFactory htmlFactory;

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        HttpJson httpJson = HttpJson.createDefault();
        htmlFactory = new SBaseHTMLFactory(
                "file:///cy3sbml/gui/",
                MiriamRegistry.bundled(),
                new OlsClient(httpJson),
                new UniprotAccess(httpJson),
                new ChebiAccess(httpJson));
    }

    @Test
    public void run() {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(SBMLCoreTest.TEST_MODEL_CORE_01);
        Model model = doc.getModel();

        Collection<Object> objSet = new HashSet<>();
        objSet.add(model);
        SBaseHTMLThread task = new SBaseHTMLThread(objSet, panel, htmlFactory);

        task.run();
        String html = task.getInfo();
        assertNotNull(html);
    }

    @Test
    public void runCore1() throws Exception {
        runModelTest(SBMLCoreTest.TEST_MODEL_CORE_01);
    }

    @Test
    public void runCore2() throws Exception {
        runModelTest(SBMLCoreTest.TEST_MODEL_CORE_02);
    }

    @Test
    public void runCore3() throws Exception {
        runModelTest(SBMLCoreTest.TEST_MODEL_CORE_03);
    }

    @Test
    public void runComp1() throws Exception {
        runModelTest(SBMLCompTest.TEST_MODEL_COMP_01);
    }

    @Test
    public void runComp2() throws Exception {
        runModelTest(SBMLCompTest.TEST_MODEL_COMP_02);
    }

    @Test
    public void runFbc1() throws Exception {
        runModelTest(SBMLFbcTest.TEST_MODEL_FBC);
    }

    @Test
    public void runGroups1() throws Exception {
        runModelTest(SBMLGroupsTest.TEST_MODEL_GROUPS);
    }

    @Test
    public void runLayouts1() throws Exception {
        runModelTest(SBMLLayoutTest.TEST_MODEL_LAYOUT);
    }

    @Test
    public void runQual1() throws Exception {
        runModelTest(SBMLQualTest.TEST_MODEL_QUAL);
    }

    /**
     * Creates info for all objects in the model.
     */
    private void runModelTest(String resource) {
        SBMLDocument doc = SBMLUtil.readSBMLDocument(resource);

        // all SBases of the model
        List<? extends TreeNode> objects = doc.getModel().filter(new SBaseFilter());

        for (TreeNode sbase : objects) {
            Collection<Object> objCollection = new HashSet<>();
            objCollection.add(sbase);
            SBaseHTMLThread t1 = new SBaseHTMLThread(objCollection, panel, htmlFactory);
            t1.run();
            String html = t1.getInfo();
            assertNotNull(html);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////

    /*
     * Writing HTML information to file for development.
     * This allows faster development cycle of the information HTML than
     * packing it in the Cytoscape app.
     */
}
