package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Optional;
import org.cy3sbml.comp.CompTargets;
import org.cy3sbml.comp.SBaseRefResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.distrib.DistribConstants;
import org.sbml.jsbml.ext.distrib.DistribSBasePlugin;
import org.sbml.jsbml.ext.distrib.UncertParameter;
import org.sbml.jsbml.ext.distrib.Uncertainty;

/** The info panel HTML of the uncertainties of the distrib package. */
class UncertaintyHtmlTest {
    /** The root network of the model, for the links to the nodes. */
    private static final long ROOT = 7L;

    private static final CompTargets TARGETS = new CompTargets() {
        @Override
        public SBaseRefResolver resolver(SBase sbase) {
            return null;
        }

        @Override
        public Optional<Long> rootNetwork(Model model) {
            return Optional.of(ROOT);
        }
    };

    private Model model;

    @BeforeEach
    void readModel() throws Exception {
        try (InputStream stream =
                UncertaintyHtmlTest.class.getResourceAsStream("/models/distrib/distrib_uncertainties.xml")) {
            SBMLDocument document = new SBMLReader().readSBMLFromStream(stream);
            model = document.getModel();
        }
        // the import sets the metaIds of the elements with nodes
        model.getParameter("sd_k1").setMetaId("meta_sd_k1");
    }

    @Test
    void parameterShowsAllUncertaintyParametersWithLinksToTheVars() {
        String html = UncertaintyHtml.create(model.getParameter("k1"), TARGETS);

        assertTrue(html.contains("<span class=\"qualifier\">uncertainty</span>"), html);
        assertTrue(html.contains("<td>standardDeviation</td>"), html);
        // two columns like the attribute table, no header row
        assertFalse(html.contains("<th>"), html);
        assertTrue(
                html.contains(
                        "sd_k1 <a href=\"" + BrowserHyperlinkListener.URL_SELECT_TARGET + ROOT + "/meta_sd_k1\">"),
                html);
        assertTrue(html.contains("[sd_k1 <a href="), html);
        assertTrue(html.contains(", 10.0]"), html);
        assertTrue(html.contains("normal(1, sd_k1)"), html);
        assertTrue(
                html.contains("<a href=\"http://www.sbml.org/sbml/symbols/distrib/normal\""
                        + " title=\"http://www.sbml.org/sbml/symbols/distrib/normal\">normal</a>"),
                html);
        assertTrue(html.contains(">PROB_k0000225</a>"), html);
        // the nested external parameter follows its parent
        assertTrue(html.indexOf("skew") > html.indexOf("normal(1, sd_k1)"), html);
        assertTrue(html.contains("class=\"distrib-nested\""), html);
    }

    @Test
    void uncertaintyShowsIdNameAndUnits() {
        String html = UncertaintyHtml.create(model.getSpecies("S1"), TARGETS);

        assertTrue(html.contains("<span class=\"qualifier\">uncertainty</span> <b>u_S1_a</b> measured"), html);
        assertTrue(html.contains("<b>u_S1_b</b>"), html);
        assertTrue(html.contains("[3.5, 4.9]"), html);
        String compartment = UncertaintyHtml.create(model.getCompartment("C"), TARGETS);
        assertTrue(compartment.contains("<td>0.1 litre</td>"), compartment);
    }

    @Test
    void varOfAMissingElementHasNoLink() {
        Parameter p = model.createParameter("p_missing");
        Uncertainty u = ((DistribSBasePlugin) p.getPlugin(DistribConstants.shortLabel)).createUncertainty();
        UncertParameter up = u.createUncertParameter();
        up.setType(UncertParameter.Type.mean);
        up.setVar("does_not_exist");

        String html = UncertaintyHtml.create(p, TARGETS);

        assertTrue(html.contains("<td>does_not_exist</td>"), html);
        assertFalse(html.contains(BrowserHyperlinkListener.URL_SELECT_TARGET), html);
    }

    @Test
    void textIsEscaped() {
        Parameter p = model.createParameter("p_escaped");
        Uncertainty u = ((DistribSBasePlugin) p.getPlugin(DistribConstants.shortLabel)).createUncertainty();
        u.setName("a <b> & c");
        UncertParameter up = u.createUncertParameter();
        up.setType(UncertParameter.Type.externalParameter);
        up.setName("<i>");

        String html = UncertaintyHtml.create(p, TARGETS);

        assertTrue(html.contains("a &lt;b&gt; &amp; c"), html);
        assertTrue(html.contains("<td>&lt;i&gt;</td>"), html);
    }

    @Test
    void noUncertaintiesNoHtml() {
        assertEquals("", UncertaintyHtml.create(model.getSpecies("S2"), TARGETS));
        assertEquals("", UncertaintyHtml.create(model.getParameter("sd_k1"), null));
    }

    @Test
    void infoOfTheElementContainsTheUncertainties() throws Exception {
        String html = new SBaseHTMLFactory("file:///app/gui/", null, null, null, null, TARGETS)
                .createInfo(model.getParameter("k1"));

        assertTrue(html.contains("<span class=\"qualifier\">uncertainty</span>"), html);
        assertTrue(html.contains("normal(1, sd_k1)"), html);
    }
}
