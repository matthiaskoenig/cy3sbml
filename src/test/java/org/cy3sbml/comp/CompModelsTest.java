package org.cy3sbml.comp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.util.List;
import org.cy3sbml.comp.ModelResolution.Failed;
import org.cy3sbml.comp.ModelResolution.Resolved;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.comp.CompSBMLDocumentPlugin;
import org.sbml.jsbml.ext.comp.ExternalModelDefinition;
import org.sbml.jsbml.ext.comp.Submodel;

class CompModelsTest {

    /** Reads the test model with its location set. */
    static SBMLDocument read(String resource) throws Exception {
        File file = new File(CompModelsTest.class.getResource(resource).toURI());
        SBMLDocument document = SBMLReader.read(file);
        document.setLocationURI(file.toURI().toString());
        return document;
    }

    static Submodel submodel(SBMLDocument document, String id) {
        return ((CompModelPlugin) document.getModel().getExtension(CompConstants.shortLabel)).getSubmodel(id);
    }

    private static Resolved resolved(ModelResolution resolution) {
        return assertInstanceOf(Resolved.class, resolution, () -> resolution.toString());
    }

    private static String failure(ModelResolution resolution) {
        return assertInstanceOf(Failed.class, resolution, () -> resolution.toString())
                .reason();
    }

    @Test
    void resolvesModelDefinition() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");
        CompModels models = new CompModels(document);

        Resolved resolved = resolved(models.resolve(submodel(document, "sub_local")));

        assertEquals("local", resolved.model().getId());
        assertSame(document, resolved.document());
    }

    @Test
    void resolvesMainModel() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");

        Resolved resolved = resolved(new CompModels(document).resolve(document, "top"));

        assertSame(document.getModel(), resolved.model());
    }

    /** 'file:ext.xml' is a relative source; without modelRef the main model is used. */
    @Test
    void resolvesRelativeFileUriToMainModel() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");

        Resolved resolved = resolved(new CompModels(document).resolve(submodel(document, "sub_ext")));

        assertEquals("ext_main", resolved.model().getId());
        assertTrue(resolved.document().getLocationURI().endsWith("/models/comp/unit/ext.xml"));
    }

    @Test
    void resolvesModelDefinitionOfExternalDocument() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");

        Resolved resolved = resolved(new CompModels(document).resolve(submodel(document, "sub_inner")));

        assertEquals("inner", resolved.model().getId());
        assertTrue(resolved.document().getLocationURI().endsWith("/models/comp/unit/sub/ext2.xml"));
    }

    /** inner2 refers to an external model definition of sub/ext2.xml, which refers to ../ext.xml. */
    @Test
    void resolvesChainOfExternalModelDefinitions() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");
        CompModels models = new CompModels(document);

        Resolved chained = resolved(models.resolve(submodel(document, "sub_inner2")));
        Resolved direct = resolved(models.resolve(submodel(document, "sub_ext")));

        assertEquals("ext_main", chained.model().getId());
        // every file is read once
        assertSame(direct.document(), chained.document());
    }

    @Test
    void resolvesTheToyModelWithRelativeSources() throws Exception {
        SBMLDocument document = read("/models/comp/koenig-toymodel/toy_top_level.xml");

        Resolved resolved = resolved(new CompModels(document).resolve(submodel(document, "fba")));

        assertEquals("toy_fba", resolved.model().getId());
    }

    @Test
    void failsForRelativeSourceWithoutLocation() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");
        document.setLocationURI(null);

        String reason = failure(new CompModels(document).resolve(submodel(document, "sub_ext")));

        assertTrue(reason.contains("location"), reason);
    }

    @Test
    void failsForUnknownModelRef() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");

        String reason = failure(new CompModels(document).resolve(document, "no_such_model"));

        assertTrue(reason.contains("no_such_model"), reason);
    }

    @Test
    void failsForUnknownModelInExternalDocument() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");

        String reason = failure(new CompModels(document).resolve(document, "unknown"));

        assertTrue(reason.contains("unknown"), reason);
    }

    @Test
    void failsForMissingFile() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");

        String reason = failure(new CompModels(document).resolve(document, "missing"));

        assertTrue(reason.contains("missing.xml"), reason);
    }

    @Test
    void failsForCycle() throws Exception {
        SBMLDocument document = read("/models/comp/unit/cycle_a.xml");

        String reason = failure(new CompModels(document).resolve(submodel(document, "sub")));

        assertTrue(reason.contains("cycle"), reason);
    }

    /** A different md5 is logged, the model is used anyway. */
    @Test
    void resolvesModelWithDifferentMd5() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");

        Resolved resolved = resolved(new CompModels(document).resolve(document, "md5"));

        assertEquals("ext_main", resolved.model().getId());
    }

    @Test
    void listsEveryExternalModelDefinitionOnce() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");

        List<CompModels.External> externals = new CompModels(document).externalModels();

        // the six of top.xml and the one of sub/ext2.xml
        assertEquals(
                List.of("ext", "inner", "inner2", "missing", "unknown", "md5", "inner2"),
                externals.stream().map(e -> e.definition().getId()).toList());
        assertInstanceOf(Failed.class, externals.get(3).resolution());
    }

    @Test
    void resolvesSourceRelativeToHttpLocation() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");
        document.setLocationURI("https://example.org/models/top.xml");
        ExternalModelDefinition definition = ((CompSBMLDocumentPlugin) document.getExtension(CompConstants.shortLabel))
                .getExternalModelDefinition("inner");

        assertEquals(URI.create("https://example.org/models/sub/ext2.xml"), CompModels.sourceUri(definition));
    }
}
