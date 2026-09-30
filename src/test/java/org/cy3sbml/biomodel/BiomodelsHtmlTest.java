package org.cy3sbml.biomodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BiomodelsHtmlTest {
    private static final BiomodelSummary SUMMARY =
            new BiomodelSummary("BIOMD0000000012", "Elowitz2000 <Repressilator>", "2005-06-11T00:00:00Z", "");
    private static final Biomodel DETAILS = new Biomodel(
            "BIOMD0000000012",
            "MODEL6615119181",
            "10659856",
            "Elowitz2000 - Repressilator",
            "<p>A repressilator.</p>",
            "Elowitz MB, Leibler S");

    private static SearchBioModel.Result searchResult(int matches) {
        return new SearchBioModel.Result(
                new SearchContent(Map.of(
                        SearchContent.CONTENT_NAME,
                        "repressilator",
                        SearchContent.CONTENT_MODE,
                        SearchContent.CONNECT_AND)),
                matches,
                List.of(SUMMARY.id()),
                Map.of(SUMMARY.id(), SUMMARY));
    }

    @Test
    void searchResultShowsTheSummaryOfEveryModel() {
        String html = BiomodelsHtml.searchResult(searchResult(1), Map.of(), Set.of(), List.of());

        assertTrue(html.contains("1 BioModel found"), html);
        assertTrue(html.contains("<a name=\"BIOMD0000000012\"></a>"), html);
        assertTrue(html.contains("Elowitz2000 &lt;Repressilator&gt;"), html);
        assertTrue(html.contains("2005-06-11"), html);
        assertFalse(html.contains("refine the search"), html);
        assertFalse(html.contains("Description"), html);
        // no row for the unknown date of the last modification
        assertFalse(html.contains("Last modified"), html);
        assertTrue(html.contains("Select models in the list"), html);
    }

    @Test
    void searchResultSaysThatNotAllModelsAreListed() {
        String html = BiomodelsHtml.searchResult(searchResult(1234), Map.of(), Set.of(), List.of());

        assertTrue(html.contains("1234 BioModels found"), html);
        assertTrue(html.contains("The first 1 models are listed, refine the search"), html);
    }

    @Test
    void searchResultShowsTheDetailsOfLookedUpModels() {
        String html = BiomodelsHtml.searchResult(
                searchResult(1), Map.of(SUMMARY.id(), DETAILS), Set.of(), List.of(SUMMARY.id()));

        assertTrue(html.contains("Elowitz2000 - Repressilator"), html);
        assertTrue(html.contains("<p>A repressilator.</p>"), html);
        assertTrue(html.contains("Elowitz MB, Leibler S"), html);
        assertTrue(html.contains("MODEL6615119181"), html);
        assertTrue(html.contains("https://pubmed.ncbi.nlm.nih.gov/10659856/"), html);
        // selected
        assertTrue(html.contains("bgcolor=\"#339933\""), html);
    }

    @Test
    void modelShowsTheStateOfTheDetails() {
        assertTrue(
                BiomodelsHtml.model("BIOMD1", SUMMARY, null, true, false, true).contains("Loading the details"));
        assertTrue(
                BiomodelsHtml.model("BIOMD1", SUMMARY, null, false, false, true).contains("could not be loaded"));
        assertTrue(BiomodelsHtml.model("BIOMD1", null, null, false, true, false).contains("could not be loaded"));
        String unselected = BiomodelsHtml.model("BIOMD1", SUMMARY, null, false, false, false);
        assertFalse(unselected.contains("could not be loaded"), unselected);
        assertFalse(unselected.contains("bgcolor"), unselected);
    }

    @Test
    void parsedIdsHaveNoSearchTerms() {
        String html = BiomodelsHtml.searchResult(
                SearchBioModel.fromIds(List.of("BIOMD1", "BIOMD2")), Map.of(), Set.of("BIOMD1", "BIOMD2"), List.of());

        assertTrue(html.contains("2 BioModels</h2>"), html);
        assertFalse(html.contains("Search Mode"), html);
        // the details of parsed ids are looked up without a selection
        assertFalse(html.contains("Select models in the list"), html);
        assertTrue(html.contains("<a name=\"BIOMD2\"></a>"), html);
    }

    @Test
    void date() {
        assertEquals("2012-04-27", BiomodelsHtml.date("2012-04-27T00:00:00Z"));
        assertEquals("", BiomodelsHtml.date(""));
        assertEquals("unknown", BiomodelsHtml.date("unknown"));
    }

    @Test
    void publication() {
        assertEquals(
                "<a href=\"https://pubmed.ncbi.nlm.nih.gov/10659856/\">10659856</a>",
                BiomodelsHtml.publication("10659856"));
        assertEquals(
                "<a href=\"https://doi.org/10.1089/ind.2013.0003\">10.1089/ind.2013.0003</a>",
                BiomodelsHtml.publication("10.1089/ind.2013.0003"));
        assertEquals("arXiv &lt;1&gt;", BiomodelsHtml.publication("arXiv <1>"));
        assertEquals("", BiomodelsHtml.publication(""));
    }

    private static String description(String description) {
        Biomodel details = new Biomodel("BIOMD1", "MODEL1", "", "name", description, "");
        return BiomodelsHtml.model("BIOMD1", SUMMARY, details, false, false, true);
    }

    /** The description, the notes of the model, is reduced to formatting markup. */
    @Test
    void descriptionIsSanitized() {
        String html = description("<notes xmlns=\"http://www.sbml.org/sbml/level2/version3\">"
                + "<body xmlns=\"http://www.w3.org/1999/xhtml\"><p>A <b>model</b>"
                + "<a href=\"file:///etc/passwd\">file</a><a href=\"https://example.org/\">web</a>"
                + "<img src=\"jar:file:///x.jar!/a.png\"/><object data=\"https://example.org/x\">o</object></p>"
                + "</body></notes>");

        assertTrue(html.contains("<b>model</b>"), html);
        assertTrue(html.contains("href=\"https://example.org/\""), html);
        assertFalse(html.contains("file:///etc/passwd") || html.contains("jar:") || html.contains("<object"), html);
        assertFalse(html.contains("<notes") || html.contains("<body"), html);
    }

    /** A description that is text, and no well-formed XML, is escaped. */
    @Test
    void textDescriptionIsEscaped() {
        String html = description("It's a model of x < y & <img src=file:///x.png>");

        assertTrue(html.contains("It's a model of x &lt; y &amp; &lt;img"), html);
    }
}
