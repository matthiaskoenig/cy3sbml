package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.cy3sbml.chebi.ChebiAccess;
import org.cy3sbml.miriam.MiriamRegistry;
import org.cy3sbml.ols.OlsClient;
import org.cy3sbml.uniprot.UniprotAccess;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.util.filters.Filter;
import org.sbml.jsbml.xml.XMLAttributes;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.jsbml.xml.XMLTriple;

class SBaseHTMLFactoryTest {

    @Test
    void htmlTextHasBaseDirAndTitle() {
        SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory("file:///app/gui/", null, null, null, null);

        String html = htmlFactory.createHTMLText("<p>text</p>", "Title");

        assertTrue(html.contains("<base href=\"file:///app/gui/\" />"), html);
        assertTrue(html.contains("<title>Title</title>"), html);
        assertTrue(html.contains("<p>text</p>"), html);
    }

    @Test
    void renderingShowsTheSboTermWithoutChangingTheDocument() throws Exception {
        SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory(
                "file:///app/gui/",
                MiriamRegistry.bundled(),
                mock(OlsClient.class),
                mock(UniprotAccess.class),
                mock(ChebiAccess.class));
        Species species = new Species("s1", 3, 1);
        species.setSBOTerm(247);

        String html = htmlFactory.createInfo(species);

        // the SBO term is shown as an annotation, but not added to the model
        assertEquals(0, species.getCVTermCount());
        assertTrue(html.contains("SBO:0000247"), html);
    }

    /**
     * The compact identifiers of the example model (e.g. "BAO:0000362", "DOI:10.1016/...",
     * "GO:0007049") resolve to their data collections and match their patterns (#394).
     */
    @Test
    void compactIdentifiersResolveToTheirDataCollections() throws Exception {
        SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory(
                "file:///app/gui/",
                MiriamRegistry.bundled(),
                mock(OlsClient.class),
                mock(UniprotAccess.class),
                mock(ChebiAccess.class));
        SBMLDocument document;
        try (InputStream in = getClass().getResourceAsStream("/models/Faure2006_MammalianCellCycle.sbml")) {
            document = SBMLReader.read(in);
        }
        List<SBase> annotated = new ArrayList<>();
        Filter hasCVTerms = o -> o instanceof SBase sbase && sbase.getCVTermCount() > 0;
        for (Object o : document.filter(hasCVTerms)) {
            annotated.add((SBase) o);
        }
        assertFalse(annotated.isEmpty());

        StringBuilder html = new StringBuilder();
        for (SBase sbase : annotated) {
            html.append(htmlFactory.createInfo(sbase));
        }

        assertFalse(html.toString().contains("does not match pattern"), html.toString());
        assertFalse(html.toString().contains("Unknown data collection"), html.toString());
        assertTrue(html.toString().contains("https://doi.org/10.1016/j.jtbi.2004.04.039"), html.toString());
    }

    @Test
    void nonRdfAnnotationShowsNonRdfElements() throws Exception {
        Species species = new Species("s1", 3, 1);
        species.getAnnotation()
                .appendNonRDFAnnotation("<sabio xmlns=\"http://sabiork.h-its.org\">kinetic data</sabio>");

        String html = SBaseHTMLFactory.createNonRDFAnnotation(species);

        assertTrue(html.contains("kinetic data"), html);
    }

    @Test
    void nonRdfAnnotationSkipsRdfElement() throws Exception {
        // element names are compared by value, not by reference: the name is a new String
        XMLNode rdf = new XMLNode(
                new XMLTriple(new String("RDF"), "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf"),
                new XMLAttributes());
        rdf.addChild(new XMLNode("rdf content"));
        XMLNode annotation = new XMLNode(new XMLTriple("annotation", "", ""), new XMLAttributes());
        annotation.addChild(rdf);
        Species species = new Species("s1", 3, 1);
        species.getAnnotation().setNonRDFAnnotation(annotation);

        String html = SBaseHTMLFactory.createNonRDFAnnotation(species);

        assertFalse(html.contains("rdf content"), html);
    }

    @Test
    void ontologyTextKeepsInlineFormatting() {
        assertEquals(
                "<small>D</small>-galactose, H<sub>2</sub>O",
                SBaseHTMLFactory.ontologyTextHTML("<small>D</small>-galactose, H<sub>2</sub>O"));
    }

    @Test
    void ontologyTextEscapesOtherMarkup() {
        assertEquals(
                "&lt;script&gt;x&lt;/script&gt; a &amp; b &lt;sub onclick=&quot;x&quot;&gt;",
                SBaseHTMLFactory.ontologyTextHTML("<script>x</script> a & b <sub onclick=\"x\">"));
    }

    @Test
    void ontologyTextKeepsNestedPairs() {
        assertEquals("<b><i>x</i></b> y", SBaseHTMLFactory.ontologyTextHTML("<b><i>x</i></b> y"));
    }

    @Test
    void ontologyTextEscapesUnbalancedTags() {
        assertEquals("H&lt;sub&gt;2 O", SBaseHTMLFactory.ontologyTextHTML("H<sub>2 O"));
        assertEquals("x&lt;/i&gt; y", SBaseHTMLFactory.ontologyTextHTML("x</i> y"));
        assertEquals("&lt;b&gt;&lt;i&gt;x&lt;/b&gt;&lt;/i&gt;", SBaseHTMLFactory.ontologyTextHTML("<b><i>x</b></i>"));
    }
}
