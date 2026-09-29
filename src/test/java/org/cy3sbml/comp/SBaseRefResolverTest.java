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
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.comp.CompSBMLDocumentPlugin;
import org.sbml.jsbml.ext.comp.CompSBasePlugin;
import org.sbml.jsbml.ext.comp.Deletion;
import org.sbml.jsbml.ext.comp.Submodel;

class SBaseRefResolverTest {

    /** The main model instantiates mdA as submodel A, mdA instantiates mdB as submodel B. */
    static final String RESOURCE = "/models/comp/unit/references.xml";

    private SBMLDocument document;
    private SBaseRefResolver resolver;

    @BeforeEach
    void setUp() throws Exception {
        document = CompModelsTest.read(RESOURCE);
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
