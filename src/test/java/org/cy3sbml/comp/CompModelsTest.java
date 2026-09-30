package org.cy3sbml.comp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.io.File;
import java.net.URI;
import java.util.List;
import java.util.Map;
import org.cy3sbml.comp.ModelResolution.Failed;
import org.cy3sbml.comp.ModelResolution.Resolved;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.comp.CompSBMLDocumentPlugin;
import org.sbml.jsbml.ext.comp.ExternalModelDefinition;
import org.sbml.jsbml.ext.comp.Submodel;
import org.slf4j.LoggerFactory;

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

    /** The md5 warnings that the action logs on the calling thread (test classes run in parallel). */
    private static List<String> md5Warnings(Executable action) throws Throwable {
        Logger logger = (Logger) LoggerFactory.getLogger(CompModels.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        String thread = Thread.currentThread().getName();
        try {
            action.execute();
        } finally {
            logger.detachAppender(appender);
        }
        return appender.list.stream()
                .filter(e -> e.getThreadName().equals(thread) && e.getLevel().isGreaterOrEqual(Level.WARN))
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.contains("md5"))
                .toList();
    }

    /** The md5 is checked once per external model definition, not for every reference. */
    @Test
    void logsDifferentMd5Once() throws Throwable {
        SBMLDocument document = read("/models/comp/unit/top.xml");
        CompModels models = new CompModels(document);

        List<String> warnings = md5Warnings(() -> {
            resolved(models.resolve(document, "md5"));
            resolved(models.resolve(document, "md5"));
        });

        assertEquals(1, warnings.size(), warnings::toString);
    }

    /**
     * A source that is an open document (of a restored session) was not read from its file,
     * so there is no checksum to compare with the md5 of the definition.
     */
    @Test
    void doesNotCheckMd5OfOpenDocument() throws Throwable {
        SBMLDocument document = read("/models/comp/unit/top.xml");
        SBMLDocument ext = read("/models/comp/unit/ext.xml");
        CompModels models = new CompModels(document, Map.of(new URI(ext.getLocationURI()), ext));

        List<String> warnings = md5Warnings(() -> {
            Resolved resolved = resolved(models.resolve(document, "md5"));
            assertSame(ext, resolved.document());
        });

        assertEquals(List.of(), warnings);
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

    /** An external file with a long comment before the root element is SBML. */
    @Test
    void resolvesSourceWithLongHeader() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");
        CompSBMLDocumentPlugin plugin = (CompSBMLDocumentPlugin) document.getExtension(CompConstants.shortLabel);
        ExternalModelDefinition definition = plugin.createExternalModelDefinition("long_header");
        definition.setSource("ext_long_header.xml");

        Resolved resolved = resolved(new CompModels(document).resolve(document, "long_header"));

        assertEquals("ext_main", resolved.model().getId());
    }

    /** An external file that is no SBML, e.g. the HTML page of a proxy, is a failure with a reason. */
    @Test
    void failsForSourceThatIsNoSbml() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");
        CompSBMLDocumentPlugin plugin = (CompSBMLDocumentPlugin) document.getExtension(CompConstants.shortLabel);
        ExternalModelDefinition definition = plugin.createExternalModelDefinition("html");
        definition.setSource("not_sbml.xml");

        String reason = failure(new CompModels(document).resolve(document, "html"));

        assertTrue(reason.contains("not_sbml.xml") && reason.contains("SBML"), reason);
    }
}
