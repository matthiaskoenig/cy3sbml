package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.sbml.jsbml.Species;

class HtmlSanitizerTest {

    /** The notes of a species with the given XHTML body content, as shown in the info panel. */
    private static String notes(String body) throws Exception {
        Species species = new Species("s1", 3, 1);
        species.setNotes("<notes><body xmlns=\"http://www.w3.org/1999/xhtml\">" + body + "</body></notes>");
        return SBMLUtil.parseNotes(species).replaceAll("\\s+", " ").strip();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "<meta http-equiv=\"refresh\" content=\"0;url=https://example.org\"/>",
                "<form action=\"https://example.org\"><input type=\"submit\" value=\"x\"/></form>",
                "<iframe src=\"https://example.org\">x</iframe>",
                "<map name=\"m\"><area href=\"file:///etc/passwd\" shape=\"rect\" coords=\"0,0,9,9\"/></map>",
                "<object data=\"https://example.org\">x</object>",
                "<embed src=\"https://example.org\"/>",
                "<base href=\"https://example.org/\"/>",
                "<link rel=\"stylesheet\" href=\"https://example.org/a.css\"/>",
                "<script>x</script>",
                "<style>p { color: red; }</style>",
                "<svg xmlns=\"http://www.w3.org/2000/svg\"><a href=\"https://example.org\">x</a></svg>"
            })
    void removesMarkupThatLoadsOrNavigates(String markup) throws Exception {
        assertEquals("<p>text</p>", notes("<p>text</p>" + markup));
    }

    @Test
    void keepsFormattingMarkup() throws Exception {
        String body = "<p align=\"center\">a <b>b</b> <em>c</em> <sub>2</sub> <font color=\"red\">d</font></p>"
                + "<table border=\"1\"><tr><td colspan=\"2\">e</td></tr></table>"
                + "<a href=\"https://identifiers.org/pubmed/123\" title=\"t\">f</a>";

        String html = notes(body);

        assertTrue(html.contains("<p align=\"center\"> a <b>b</b> <em>c</em> <sub>2</sub>"), html);
        assertTrue(html.contains("<font color=\"red\">d</font>"), html);
        assertTrue(html.contains("<td colspan=\"2\">e</td>"), html);
        assertTrue(html.contains("href=\"https://identifiers.org/pubmed/123\""), html);
        assertTrue(html.contains("title=\"t\""), html);
    }

    @Test
    void removesEventHandlersAndTargets() throws Exception {
        String html =
                notes("<p onclick=\"x()\" onmouseover=\"y()\"><a href=\"https://a.org\" target=\"_blank\">a</a></p>");

        assertFalse(html.contains("onclick") || html.contains("onmouseover") || html.contains("target"), html);
    }

    @ParameterizedTest
    @ValueSource(strings = {"file:///etc/passwd", "data:text/html,x", "javascript:x()", "relative.html", " "})
    void removesLinksWithOtherSchemes(String href) throws Exception {
        String html = notes("<p><a href=\"" + href + "\">a</a></p>");

        assertFalse(html.contains("href"), html);
        assertTrue(html.contains(">a</a>"), html);
    }

    @Test
    void removesImagesWithOtherSchemesAndStylesLoadingResources() throws Exception {
        String html = notes("<p><img src=\"file:///x.png\" alt=\"a\"/>"
                + "<span style=\"background: URL (https://example.org/x.png)\">b</span>"
                + "<span style=\"color: red\">c</span></p>");

        assertFalse(html.contains("src="), html);
        assertTrue(html.contains("alt=\"a\""), html);
        assertTrue(html.contains("<span>b</span>"), html);
        assertTrue(html.contains("<span style=\"color: red\">c</span>"), html);
    }

    /** Elements outside the allowlist, e.g. an html wrapper or RDF, are replaced by their content. */
    @Test
    void unwrapsUnknownElements() throws Exception {
        String html = notes("<html><head><title>t</title></head><body><p>a</p></body></html>"
                + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">b</rdf:RDF>");

        assertEquals("<p>a</p> b", html);
    }
}
