package org.cy3sbml.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Species;

class CobraNotesParserTest {

    private static Species speciesWithNotes(String paragraphs) throws Exception {
        Species species = new Species(3, 1);
        species.setNotes("<notes><body xmlns=\"http://www.w3.org/1999/xhtml\">" + paragraphs + "</body></notes>");
        return species;
    }

    @Test
    void parsesKeyValueParagraphs() throws Exception {
        Properties props = CobraNotesParser.parse(speciesWithNotes("<p>GENE_ASSOCIATION: b0001</p>"));
        assertEquals(Map.of("GENE_ASSOCIATION", "b0001"), props);
    }

    @Test
    void ignoresMalformedLines() throws Exception {
        Properties props = CobraNotesParser.parse(speciesWithNotes("<p>GENE_ASSOCIATION: b0001</p>"
                + "<p>no colon here</p>"
                + "<p>two: colons: here</p>"
                + "<p>key with spaces: value</p>"));
        assertEquals(Map.of("GENE_ASSOCIATION", "b0001"), props);
    }

    @Test
    void returnsEmptyPropertiesWithoutNotes() {
        assertTrue(CobraNotesParser.parse(new Species(3, 1)).isEmpty());
    }

    private static Species speciesWithRawNotes(String notes) throws Exception {
        Species species = new Species(3, 1);
        species.setNotes(notes);
        return species;
    }

    /** Paragraphs directly in the notes, without a body element. */
    @Test
    void parsesDirectParagraphsWithoutBody() throws Exception {
        Properties props = CobraNotesParser.parse(speciesWithRawNotes("<notes>"
                + "<p xmlns=\"http://www.w3.org/1999/xhtml\">GENE_ASSOCIATION: b0001</p>"
                + "<p xmlns=\"http://www.w3.org/1999/xhtml\">SUBSYSTEM: Glycolysis</p>"
                + "</notes>"));
        assertEquals(Map.of("GENE_ASSOCIATION", "b0001", "SUBSYSTEM", "Glycolysis"), props);
    }

    /** Paragraphs in an html element with a body. */
    @Test
    void parsesParagraphsInHtmlBody() throws Exception {
        Properties props = CobraNotesParser.parse(speciesWithRawNotes("<notes>"
                + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head><title>t</title></head>"
                + "<body><p>GENE_ASSOCIATION: b0001</p></body></html>"
                + "</notes>"));
        assertEquals(Map.of("GENE_ASSOCIATION", "b0001"), props);
    }
}
