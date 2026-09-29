package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.distrib.DistribConstants;
import org.sbml.jsbml.ext.distrib.DistribSBasePlugin;
import org.sbml.jsbml.ext.distrib.UncertParameter;

class DistribUtilTest {
    private static Model model;

    @BeforeAll
    static void readModel() throws Exception {
        try (InputStream stream =
                DistribUtilTest.class.getResourceAsStream("/models/distrib/distrib_uncertainties.xml")) {
            model = new SBMLReader().readSBMLFromStream(stream).getModel();
        }
    }

    private static String summary(SBase sbase) {
        return DistribUtil.summary(DistribUtil.uncertainties(sbase));
    }

    @Test
    void valueWithUnits() {
        assertEquals("standardDeviation=0.1 litre", summary(model.getCompartment("C")));
    }

    @Test
    void uncertaintiesWithIdsAndSpan() {
        assertEquals(
                "u_S1_a: mean=4.2; variance=0.3 | u_S1_b: confidenceInterval=[3.5, 4.9]",
                summary(model.getSpecies("S1")));
    }

    @Test
    void varSpanWithVarAndDistributionWithNestedParameter() {
        assertEquals(
                "standardDeviation=sd_k1; range=[sd_k1, 10.0]; distribution=normal(1, sd_k1) (skew=0.1)",
                summary(model.getParameter("k1")));
    }

    @Test
    void coefficientOfVariationWrittenByLibSBML() {
        assertEquals("coefficientOfVariation=0.05", summary(model.getInitialAssignment(0)));
        assertEquals("coefficientOfVariation=0.05", summary(model.getRule(0)));
    }

    @Test
    void reactionAndSpeciesReferences() {
        Reaction reaction = model.getReaction("J0");
        assertEquals("median=1.5", summary(reaction));
        assertEquals("standardDeviation=0.5", summary(reaction.getReactant(0)));
        assertEquals("mean=1.0", summary(reaction.getProduct(0)));
    }

    @Test
    void externalParameterWithOnlyDefinitionURL() {
        assertEquals(
                "externalParameter=http://www.uncertml.org/distributions/normal",
                summary(model.getReaction("J0").getKineticLaw()));
    }

    @Test
    void spanWithOneBound() {
        assertEquals(
                "interquartileRange=[0.1, ?]",
                summary(model.getReaction("J0").getKineticLaw().getLocalParameter("kl")));
    }

    @Test
    void parameterWithOnlyType() {
        Parameter p = new SBMLDocument(3, 2).createModel("m").createParameter("p");
        DistribSBasePlugin plugin = (DistribSBasePlugin) p.getPlugin(DistribConstants.shortLabel);
        plugin.createUncertainty().createUncertParameter().setType(UncertParameter.Type.mode);

        assertEquals("mode", summary(p));
    }

    @Test
    void elementWithoutUncertaintiesIsNotChanged() {
        Parameter p = model.getParameter("sd_k1");

        assertTrue(DistribUtil.uncertainties(p).isEmpty());
        assertEquals("", DistribUtil.summary(DistribUtil.uncertainties(p)));
        assertNull(p.getExtension(DistribConstants.shortLabel));
    }
}
