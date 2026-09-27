package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Species;
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
}
