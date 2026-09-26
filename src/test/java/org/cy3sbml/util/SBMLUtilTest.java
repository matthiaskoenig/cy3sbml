package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.cy3sbml.SBML;
import org.cy3sbml.gui.SBaseHTMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.fbc.FBCConstants;
import org.sbml.jsbml.ext.fbc.FBCReactionPlugin;
import org.sbml.jsbml.ext.fbc.FBCSpeciesPlugin;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.groups.Member;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;

class SBMLUtilTest {

    private Model model;
    private Compartment compartment;
    private Species species;
    private Parameter parameter;
    private Reaction reaction;

    @BeforeEach
    void setUp() {
        SBMLDocument doc = new SBMLDocument(3, 1);
        model = doc.createModel("m1");
        model.setSubstanceUnits("mole");
        model.setTimeUnits("second");
        model.setVolumeUnits("litre");
        model.setAreaUnits("metre2");
        model.setLengthUnits("metre");
        model.setExtentUnits("mole");
        model.setConversionFactor("p1");

        compartment = model.createCompartment("c1");
        compartment.setName("Comp1");
        compartment.setSpatialDimensions(3d);
        compartment.setSize(1.0);
        compartment.setConstant(true);

        species = model.createSpecies("s1", compartment);
        species.setName("S1");
        species.setInitialConcentration(2.0);
        species.setBoundaryCondition(false);
        species.setHasOnlySubstanceUnits(false);
        species.setConstant(false);

        parameter = model.createParameter("p1");
        parameter.setName("P1");
        parameter.setValue(5.0);
        parameter.setConstant(false);

        reaction = model.createReaction("r1");
        reaction.setName("R1");
        reaction.setReversible(false);
        reaction.setCompartment("c1");
        KineticLaw law = reaction.createKineticLaw();
        law.setMath(new ASTNode(1.0));
        LocalParameter lp = law.createLocalParameter("k1");
        lp.setValue(1.0);
    }

    @Test
    void qualitativeSpeciesMapShowsInitialAndMaxLevel() {
        QualitativeSpecies qs = new QualitativeSpecies("s1", 3, 1);
        qs.setInitialLevel(1);
        qs.setMaxLevel(2);

        Map<String, String> map = SBMLUtil.createQualitativeSpeciesMap(qs);

        assertEquals("1/2", map.get(SBML.ATTR_QUAL_INITIAL_LEVEL + "/" + SBML.ATTR_QUAL_MAX_LEVEL));
    }

    @Test
    void getUnqualifiedClassNameStripsPackageAndReplacesDollar() {
        assertEquals("Compartment", SBMLUtil.getUnqualifiedClassName(compartment));
        assertEquals("QualitativeSpecies", SBMLUtil.getUnqualifiedClassName(new QualitativeSpecies("s2", 3, 1)));
    }

    @Test
    void getVariableFromRuleReturnsNullForAlgebraicRule() {
        assertNull(SBMLUtil.getVariableFromRule(model.createAlgebraicRule()));
    }

    @Test
    void getVariableFromRuleReturnsVariableForAssignmentRule() {
        AssignmentRule rule = model.createAssignmentRule();
        rule.setVariable("p1");
        rule.setMath(new ASTNode(3.0));

        Variable variable = SBMLUtil.getVariableFromRule(rule);

        assertNotNull(variable);
        assertEquals("p1", variable.getId());
    }

    @Test
    void getVariableFromRuleReturnsVariableForRateRule() {
        RateRule rule = model.createRateRule();
        rule.setVariable("p1");
        rule.setMath(new ASTNode(3.0));

        Variable variable = SBMLUtil.getVariableFromRule(rule);

        assertNotNull(variable);
        assertEquals("p1", variable.getId());
    }

    @Test
    void parseNotesStripsBodyWrapper() throws Exception {
        species.setNotes("<notes><body xmlns=\"http://www.w3.org/1999/xhtml\"><p>hello</p></body></notes>");

        String text = SBMLUtil.parseNotes(species);

        assertTrue(text.contains("hello"));
        assertTrue(text.contains("<p>"));
    }

    @Test
    void createSBaseMapUsesMetaId() {
        compartment.setMetaId("meta1");
        Map<String, String> map = SBMLUtil.createSBaseMap(compartment);
        assertEquals("meta1", map.get(SBML.ATTR_METAID));
    }

    @Test
    void createNamedSBaseMapUsesIdAndName() {
        Map<String, String> map = SBMLUtil.createNamedSBaseMap(compartment);
        assertEquals("c1", map.get("id"));
        assertEquals("Comp1", map.get("name"));
    }

    @Test
    void createSBMLDocumentMapIsEmpty() {
        Map<String, String> map = SBMLUtil.createSBMLDocumentMap(model.getSBMLDocument());
        assertTrue(map.isEmpty());
    }

    @Test
    void createModelMapIncludesUnits() {
        Map<String, String> map = SBMLUtil.createModelMap(model);
        assertTrue(map.get(SBML.ATTR_SUBSTANCE_UNITS).contains("mole"));
        assertTrue(map.get(SBML.ATTR_TIME_UNITS).contains("second"));
        assertTrue(map.get(SBML.ATTR_VOLUME_UNITS).contains("litre"));
        assertEquals("p1", map.get(SBML.ATTR_CONVERSION_FACTOR));
    }

    @Test
    void createCompartmentMapIncludesSizeAndDimensions() {
        Map<String, String> map = SBMLUtil.createCompartmentMap(compartment);
        assertEquals("3.0", map.get(SBML.ATTR_SPATIAL_DIMENSIONS));
        assertEquals("1.0", map.get(SBML.ATTR_SIZE));
    }

    @Test
    void createParameterMapIncludesValue() {
        Map<String, String> map = SBMLUtil.createParameterMap(parameter);
        assertTrue(map.get(SBML.ATTR_VALUE).contains("5.0"));
    }

    @Test
    void createSpeciesMapIncludesCompartmentLinkAndAmounts() {
        Map<String, String> map = SBMLUtil.createSpeciesMap(species);
        assertTrue(map.get("compartment").contains("c1"));
        assertEquals("2.0", map.get("initialConcentration"));
        assertEquals(SBaseHTMLFactory.booleanHTML(false), map.get(SBML.ATTR_BOUNDARY_CONDITION));
    }

    @Test
    void createSpeciesMapIncludesDeprecatedCharge() {
        // the deprecated core "charge" attribute only exists on SBML Level 2 species
        Species s2 = new Species("s2", 2, 4);
        s2.setCompartment("c1");
        s2.setCharge(2);

        Map<String, String> map = SBMLUtil.createSpeciesMap(s2);

        assertEquals("2", map.get("charge"));
    }

    @Test
    void createSpeciesMapIncludesFbcChargeAndFormula() {
        FBCSpeciesPlugin fbcSpecies = new FBCSpeciesPlugin(species);
        species.addExtension(FBCConstants.namespaceURI, fbcSpecies);
        fbcSpecies.setCharge(-1);
        fbcSpecies.setChemicalFormula("H2O");

        Map<String, String> map = SBMLUtil.createSpeciesMap(species);

        assertEquals("-1", map.get(SBML.ATTR_FBC_CHARGE));
        assertEquals("H2O", map.get(SBML.ATTR_FBC_CHEMICAL_FORMULA));
    }

    @Test
    void createReactionMapIncludesKineticLawAndCompartment() {
        Map<String, String> map = SBMLUtil.createReactionMap(reaction);
        assertTrue(map.get(SBML.ATTR_KINETIC_LAW).contains("1"));
        assertTrue(map.get("compartment").contains("c1"));
        assertEquals(SBaseHTMLFactory.booleanHTML(false), map.get(SBML.ATTR_REVERSIBLE));
    }

    @Test
    void createReactionMapIncludesFbcFluxBounds() {
        FBCReactionPlugin fbcReaction = new FBCReactionPlugin(reaction);
        reaction.addExtension(FBCConstants.namespaceURI, fbcReaction);
        fbcReaction.setLowerFluxBound("lb");
        fbcReaction.setUpperFluxBound("ub");

        Map<String, String> map = SBMLUtil.createReactionMap(reaction);

        assertEquals("lb", map.get(SBML.ATTR_FBC_LOWER_FLUX_BOUND));
        assertEquals("ub", map.get(SBML.ATTR_FBC_UPPER_FLUX_BOUND));
    }

    @Test
    void createInitialAssignmentMapIncludesVariable() {
        InitialAssignment ia = model.createInitialAssignment();
        ia.setSymbol("p1");
        ia.setMath(new ASTNode(5.0));

        Map<String, String> map = SBMLUtil.createInitialAssignmentMap(ia);

        assertTrue(map.get(SBML.ATTR_VARIABLE).contains("p1"));
        assertTrue(map.get(SBML.ATTR_MATH).contains("p1"));
    }

    @Test
    void createUnitDefinitionMapListsUnits() {
        UnitDefinition ud = model.createUnitDefinition("ud1");
        Unit u = ud.createUnit();
        u.setKind(Unit.Kind.MOLE);
        u.setExponent(1.0);
        u.setScale(0);
        u.setMultiplier(1.0);

        Map<String, String> map = SBMLUtil.createUnitDefinitionMap(ud);

        assertTrue(map.get("units").contains(u.printUnit()));
    }

    @Test
    void createUnitMapIncludesKindExponentScaleMultiplier() {
        Unit u = new Unit(3, 1);
        u.setKind(Unit.Kind.MOLE);
        u.setExponent(2.0);
        u.setScale(1);
        u.setMultiplier(1.5);

        Map<String, String> map = SBMLUtil.createUnitMap(u);

        assertEquals("MOLE", map.get(SBML.ATTR_UNIT_KIND));
        assertEquals("2.0", map.get(SBML.ATTR_UNIT_EXPONENT));
        assertEquals("1.5", map.get(SBML.ATTR_UNIT_MULTIPLIER));
        assertEquals("1", map.get(SBML.ATTR_UNIT_SCALE));
    }

    @Test
    void createConstraintMapIncludesMessage() throws Exception {
        Constraint constraint = model.createConstraint();
        constraint.setMath(new ASTNode(ASTNode.Type.CONSTANT_TRUE));
        constraint.setMessage("must be non-negative");

        Map<String, String> map = SBMLUtil.createConstraintMap(constraint);

        assertTrue(map.get(SBML.ATTR_MESSAGE).contains("must be non-negative"));
    }

    @Test
    void createEventMapIncludesTriggerPriorityDelay() {
        Event event = model.createEvent("e1");
        Trigger trigger = event.createTrigger();
        trigger.setMath(new ASTNode(ASTNode.Type.CONSTANT_TRUE));
        trigger.setInitialValue(true);
        trigger.setPersistent(true);
        Priority priority = event.createPriority();
        priority.setMath(new ASTNode(1.0));
        Delay delay = event.createDelay();
        delay.setMath(new ASTNode(0.0));

        Map<String, String> map = SBMLUtil.createEventMap(event);

        assertTrue(map.get("trigger").contains("true"));
        assertTrue(map.get("priority").contains("1"));
        assertTrue(map.get("delay").contains("0"));
    }

    @Test
    void createEventAssignmentMapIncludesVariable() {
        Event event = model.createEvent("e1");
        EventAssignment ea = event.createEventAssignment();
        ea.setVariable("p1");
        ea.setMath(new ASTNode(10.0));

        Map<String, String> map = SBMLUtil.createEventAssignmentMap(ea);

        assertTrue(map.get(SBML.ATTR_VARIABLE).contains("p1"));
    }

    @Test
    void createRuleMapIncludesVariableForAssignmentRule() {
        AssignmentRule rule = model.createAssignmentRule();
        rule.setVariable("p1");
        rule.setMath(new ASTNode(3.0));

        Map<String, String> map = SBMLUtil.createRuleMap(rule);

        assertTrue(map.get(SBML.ATTR_VARIABLE).contains("p1"));
    }

    @Test
    void createLocalParameterMapIncludesReactionLink() {
        LocalParameter lp = reaction.getKineticLaw().getLocalParameter("k1");

        Map<String, String> map = SBMLUtil.createLocalParameterMap(lp);

        assertTrue(map.get("reaction").contains("r1"));
    }

    @Test
    void createKineticLawMapIncludesReactionLink() {
        Map<String, String> map = SBMLUtil.createKineticLawMap(reaction.getKineticLaw());
        assertTrue(map.get("reaction").contains("r1"));
    }

    @Test
    void createFunctionDefinitionMapIncludesMath() throws Exception {
        FunctionDefinition fd = model.createFunctionDefinition("f1");
        fd.setMath(ASTNode.parseFormula("lambda(x, x*2)"));

        Map<String, String> map = SBMLUtil.createFunctionDefinitionMap(fd);

        assertTrue(map.get(SBML.ATTR_MATH).contains("lambda"));
    }

    @Test
    void createTransitionMapUsesNamedSBase() {
        Transition transition = new Transition("t1", 3, 1);
        transition.setName("T1");

        Map<String, String> map = SBMLUtil.createTransitionMap(transition);

        assertEquals("t1", map.get("id"));
        assertEquals("T1", map.get("name"));
    }

    @Test
    void createGeneProductMapUsesNamedSBase() {
        GeneProduct gp = new GeneProduct("gp1", 3, 1);
        gp.setName("GP1");

        Map<String, String> map = SBMLUtil.createGeneProductMap(gp);

        assertEquals("gp1", map.get("id"));
        assertEquals("GP1", map.get("name"));
    }

    @Test
    void createPortMapIncludesRefs() {
        Port port = new Port("port1", 3, 1);
        port.setPortRef("portRef1");
        port.setIdRef("idRef1");
        port.setUnitRef("unitRef1");
        port.setMetaIdRef("metaIdRef1");

        Map<String, String> map = SBMLUtil.createPortMap(port);

        assertEquals("portRef1", map.get(SBML.ATTR_COMP_PORTREF));
        assertEquals("idRef1", map.get(SBML.ATTR_COMP_IDREF));
        assertEquals("unitRef1", map.get(SBML.ATTR_COMP_UNITREF));
        assertEquals("metaIdRef1", map.get(SBML.ATTR_COMP_METAIDREF));
    }

    @Test
    void createGroupMapIncludesKindAndMembers() {
        Group group = new Group(3, 1);
        group.setId("g1");
        group.setName("G1");
        group.setKind(Group.Kind.partonomy);
        Member member = group.createMember();
        member.setIdRef("r1");

        Map<String, String> map = SBMLUtil.createGroupMap(group);

        assertEquals("partonomy", map.get("kind"));
        assertTrue(map.get("members").contains("<ul>"));
    }
}
