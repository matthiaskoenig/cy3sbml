package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Map;
import org.cy3sbml.SBML;
import org.cy3sbml.util.SBMLUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCModelPlugin;
import org.sbml.jsbml.ext.fbc.UserDefinedConstraint;

/** The info panel HTML of the elements of fbc version 3. */
class FbcV3HtmlTest {
    private Model model;
    private FBCModelPlugin fbcModel;

    @BeforeEach
    void readModel() throws Exception {
        try (InputStream stream =
                FbcV3HtmlTest.class.getResourceAsStream("/models/fbc/fbc_v3_example_L3V1_fbcV3.xml")) {
            SBMLDocument document = new SBMLReader().readSBMLFromStream(stream);
            model = document.getModel();
        }
        fbcModel = (FBCModelPlugin) model.getExtension(FBCConstants.shortLabel);
    }

    @Test
    void keyValuePairsAreATable() {
        String html = KeyValuePairHtml.create(model.getReaction("RGLX"));

        assertTrue(html.contains("key-value pairs"), html);
        assertTrue(html.contains("<b>confidence</b>"), html);
        assertTrue(
                html.contains("<td>4<br><small><a href=\"https://github.com/matthiaskoenig/cy3sbml/kvp\">"
                        + "https://github.com/matthiaskoenig/cy3sbml/kvp</a></small></td>"),
                html);
        // a pair without value and uri
        assertTrue(html.contains("<tr><td><b>curated</b></td><td></td></tr>"), html);
    }

    @Test
    void keyValuePairsShowIdAndName() {
        assertTrue(KeyValuePairHtml.create(model).contains("<b>created</b> <small>(kvp_created)</small>"));
        assertTrue(KeyValuePairHtml.create(model.getSpecies("D"))
                .contains("<b>compartment_label</b> <small>(compartment label)</small>"));
    }

    @Test
    void noKeyValuePairsNoHtml() {
        assertEquals("", KeyValuePairHtml.create(model.getSpecies("A")));
        assertEquals("", KeyValuePairHtml.create(model.getParameter("one")));
    }

    @Test
    void nonRdfAnnotationLeavesOutKeyValuePairs() {
        String html = SBaseHTMLFactory.createNonRDFAnnotation(model.getReaction("RGLX"));

        assertFalse(html.contains("confidence"), html);
    }

    @Test
    void userDefinedConstraintMapShowsTheConstraint() {
        UserDefinedConstraint uc1 = fbcModel.getUserDefinedConstraint("uc1");

        Map<String, String> map = SBMLUtil.createUserDefinedConstraintMap(uc1);

        assertTrue(map.get(SBML.ATTR_FBC_LOWER_BOUND).startsWith("five = 5 <a"), map.toString());
        assertTrue(map.get(SBML.ATTR_FBC_UPPER_BOUND).startsWith("five = 5 <a"), map.toString());
        assertEquals("five &le; one &middot; RGLX + negone &middot; RBTK &le; five", map.get("constraint"));
        assertTrue(map.get("component uc1_c1").startsWith("one &middot; RGLX (linear) <a"), map.toString());
    }

    @Test
    void userDefinedConstraintMapShowsQuadraticComponents() {
        Map<String, String> map = SBMLUtil.createUserDefinedConstraintMap(fbcModel.getUserDefinedConstraint("uc3"));

        assertEquals("zero &le; one &middot; RGLX &middot; RBTK &le; ub", map.get("constraint"));
        assertTrue(
                map.get("component uc3_c1").startsWith("one &middot; RGLX &middot; RBTK (quadratic) <a"),
                map.toString());
    }

    @Test
    void userDefinedConstraintComponentMap() {
        Map<String, String> map = SBMLUtil.createUserDefinedConstraintComponentMap(
                fbcModel.getUserDefinedConstraint("uc3").getUserDefinedConstraintComponent(0));

        assertTrue(map.get("coefficient").startsWith("one = 1 <a"), map.toString());
        assertTrue(map.get("variable").startsWith("RGLX <a"), map.toString());
        assertTrue(map.get("variable2").startsWith("RBTK <a"), map.toString());
        assertEquals("quadratic", map.get(SBML.ATTR_FBC_VARIABLE_TYPE));
    }

    @Test
    void reactionMapShowsVariableTypeOfFluxObjective() {
        Map<String, String> map = SBMLUtil.createReactionMap(model.getReaction("RGLX"));

        assertEquals("1.0", map.get("fbc_objective-obj_quadratic"));
        assertEquals("quadratic", map.get("fbc_objective-obj_quadratic_variableType"));
        assertTrue(map.get(SBML.ATTR_FBC_LOWER_FLUX_BOUND).startsWith("zero = 0 <a"), map.toString());
        assertTrue(map.get(SBML.ATTR_FBC_UPPER_FLUX_BOUND).startsWith("ub = 1000 <a"), map.toString());
    }
}
