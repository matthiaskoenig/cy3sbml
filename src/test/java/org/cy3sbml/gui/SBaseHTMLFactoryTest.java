package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.xml.XMLAttributes;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.jsbml.xml.XMLTriple;

class SBaseHTMLFactoryTest {

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
}
