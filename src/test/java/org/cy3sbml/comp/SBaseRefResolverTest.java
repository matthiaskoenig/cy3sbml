package org.cy3sbml.comp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.cy3sbml.comp.SBaseRefResolution.Resolved;
import org.cy3sbml.comp.SBaseRefResolution.Unresolved;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.comp.CompSBMLDocumentPlugin;
import org.sbml.jsbml.ext.comp.CompSBasePlugin;
import org.sbml.jsbml.ext.comp.Deletion;
import org.sbml.jsbml.ext.comp.Submodel;

class SBaseRefResolverTest {

    /**
     * The main model instantiates mdA as submodel A, mdA instantiates mdB as submodel B.
     */
    private static final String SBML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core"
                  xmlns:comp="http://www.sbml.org/sbml/level3/version1/comp/version1"
                  level="3" version="1" comp:required="true">
              <model id="main">
                <listOfCompartments>
                  <compartment id="c" constant="true"/>
                </listOfCompartments>
                <listOfSpecies>
                  <species id="S" compartment="c" hasOnlySubstanceUnits="false" boundaryCondition="false"
                           constant="false">
                    <comp:listOfReplacedElements>
                      <comp:replacedElement comp:submodelRef="A" comp:portRef="pa_x"/>
                    </comp:listOfReplacedElements>
                  </species>
                </listOfSpecies>
                <listOfParameters>
                  <parameter id="q" constant="true">
                    <comp:replacedBy comp:submodelRef="A" comp:idRef="B">
                      <comp:sBaseRef comp:idRef="y"/>
                    </comp:replacedBy>
                  </parameter>
                </listOfParameters>
                <comp:listOfSubmodels>
                  <comp:submodel comp:id="A" comp:modelRef="mdA">
                    <comp:listOfDeletions>
                      <comp:deletion comp:id="del_meta" comp:metaIdRef="meta_x"/>
                      <comp:deletion comp:id="del_unit" comp:unitRef="u"/>
                      <comp:deletion comp:id="del_nested" comp:idRef="B">
                        <comp:sBaseRef comp:idRef="y"/>
                      </comp:deletion>
                      <comp:deletion comp:id="del_missing" comp:idRef="missing"/>
                      <comp:deletion comp:id="del_no_submodel" comp:idRef="x">
                        <comp:sBaseRef comp:idRef="y"/>
                      </comp:deletion>
                      <comp:deletion comp:id="del_base_unit" comp:unitRef="second"/>
                      <comp:deletion comp:id="del_unit_id" comp:idRef="u"/>
                    </comp:listOfDeletions>
                  </comp:submodel>
                  <comp:submodel comp:id="Z" comp:modelRef="no_such_model"/>
                </comp:listOfSubmodels>
                <comp:listOfPorts>
                  <comp:port comp:id="p_S" comp:idRef="S"/>
                </comp:listOfPorts>
              </model>
              <comp:listOfModelDefinitions>
                <comp:modelDefinition id="mdA">
                  <listOfUnitDefinitions>
                    <unitDefinition id="u">
                      <listOfUnits>
                        <unit kind="second" exponent="1" scale="0" multiplier="1"/>
                      </listOfUnits>
                    </unitDefinition>
                  </listOfUnitDefinitions>
                  <listOfParameters>
                    <parameter metaid="meta_x" id="x" constant="true"/>
                  </listOfParameters>
                  <comp:listOfSubmodels>
                    <comp:submodel comp:id="B" comp:modelRef="mdB"/>
                  </comp:listOfSubmodels>
                  <comp:listOfPorts>
                    <comp:port comp:id="pa_x" comp:idRef="x"/>
                  </comp:listOfPorts>
                </comp:modelDefinition>
                <comp:modelDefinition id="mdB">
                  <listOfParameters>
                    <parameter id="y" constant="true"/>
                  </listOfParameters>
                </comp:modelDefinition>
              </comp:listOfModelDefinitions>
            </sbml>
            """;

    private SBMLDocument document;
    private SBaseRefResolver resolver;

    @BeforeEach
    void setUp() throws Exception {
        document = new SBMLReader().readSBMLFromString(SBML.strip());
        resolver = new SBaseRefResolver(new CompModels(document));
    }

    private Model modelDefinition(String id) {
        return ((CompSBMLDocumentPlugin) document.getExtension(CompConstants.shortLabel)).getModelDefinition(id);
    }

    private static CompModelPlugin compModel(Model model) {
        return (CompModelPlugin) model.getExtension(CompConstants.shortLabel);
    }

    private Deletion deletion(String id) {
        return compModel(document.getModel())
                .getSubmodel("A")
                .getListOfDeletions()
                .get(id);
    }

    private static Resolved resolved(SBaseRefResolution resolution) {
        return assertInstanceOf(Resolved.class, resolution, resolution::toString);
    }

    private static String unresolved(SBaseRefResolution resolution) {
        return assertInstanceOf(Unresolved.class, resolution, resolution::toString)
                .reason();
    }

    @Test
    void portResolvesInItsOwnModel() {
        Resolved resolved =
                resolved(resolver.resolve(compModel(document.getModel()).getPort("p_S")));

        assertSame(document.getModel(), resolved.model());
        assertSame(document.getModel().getSpecies("S"), resolved.target());
        assertEquals(List.of(), resolved.path());
    }

    @Test
    void replacedElementResolvesThroughPortOfSubmodel() {
        CompSBasePlugin plugin =
                (CompSBasePlugin) document.getModel().getSpecies("S").getExtension(CompConstants.shortLabel);

        Resolved resolved = resolved(resolver.resolve(plugin.getReplacedElement(0)));

        assertSame(modelDefinition("mdA"), resolved.model());
        assertSame(modelDefinition("mdA").getParameter("x"), resolved.target());
    }

    @Test
    void replacedByResolvesNestedReference() {
        CompSBasePlugin plugin =
                (CompSBasePlugin) document.getModel().getParameter("q").getExtension(CompConstants.shortLabel);

        Resolved resolved = resolved(resolver.resolve(plugin.getReplacedBy()));

        assertSame(modelDefinition("mdB"), resolved.model());
        assertSame(modelDefinition("mdB").getParameter("y"), resolved.target());
        Submodel b = compModel(modelDefinition("mdA")).getSubmodel("B");
        assertEquals(List.of(b), resolved.path());
    }

    @Test
    void deletionResolvesMetaIdRef() {
        Resolved resolved = resolved(resolver.resolve(deletion("del_meta")));

        assertSame(modelDefinition("mdA").getParameter("x"), resolved.target());
    }

    @Test
    void deletionResolvesUnitRef() {
        Resolved resolved = resolved(resolver.resolve(deletion("del_unit")));

        assertSame(modelDefinition("mdA").getUnitDefinition("u"), resolved.target());
    }

    @Test
    void deletionResolvesNestedReference() {
        Resolved resolved = resolved(resolver.resolve(deletion("del_nested")));

        assertSame(modelDefinition("mdB").getParameter("y"), resolved.target());
    }

    @Test
    void missingTargetIsUnresolved() {
        String reason = unresolved(resolver.resolve(deletion("del_missing")));

        assertTrue(reason.contains("missing"), reason);
    }

    @Test
    void nestedReferenceIntoElementThatIsNoSubmodelIsUnresolved() {
        String reason = unresolved(resolver.resolve(deletion("del_no_submodel")));

        assertTrue(reason.contains("submodel"), reason);
    }

    @Test
    void baseUnitIsUnresolved() {
        String reason = unresolved(resolver.resolve(deletion("del_base_unit")));

        assertTrue(reason.contains("second"), reason);
    }

    /** Unit definitions are in the UnitSId namespace, an idRef does not find them. */
    @Test
    void idRefDoesNotFindUnitDefinition() {
        String reason = unresolved(resolver.resolve(deletion("del_unit_id")));

        assertTrue(reason.contains("'u'"), reason);
    }

    @Test
    void unknownModelOfSubmodelIsUnresolved() {
        Deletion deletion = compModel(document.getModel()).getSubmodel("Z").createDeletion("del_z");
        deletion.setIdRef("x");

        String reason = unresolved(resolver.resolve(deletion));

        assertTrue(reason.contains("no_such_model"), reason);
    }

    @Test
    void resolvedTargetGetsMetaId() {
        assertFalse(modelDefinition("mdB").getParameter("y").isSetMetaId());

        resolved(resolver.resolve(deletion("del_nested")));

        assertTrue(modelDefinition("mdB").getParameter("y").isSetMetaId());
    }

    @Test
    void describesTheChainOfReferences() {
        CompSBasePlugin plugin =
                (CompSBasePlugin) document.getModel().getParameter("q").getExtension(CompConstants.shortLabel);

        assertEquals("submodelRef=A > idRef=B > idRef=y", SBaseRefResolver.describe(plugin.getReplacedBy()));
        assertEquals("metaIdRef=meta_x", SBaseRefResolver.describe(deletion("del_meta")));
    }
}
