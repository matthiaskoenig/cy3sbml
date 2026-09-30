package org.cy3sbml.biomodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class SearchContentTest {

    @Test
    public void testByName() {
        SearchContent content = new SearchContent(
                Map.of(SearchContent.CONTENT_NAME, "Test", SearchContent.CONTENT_MODE, SearchContent.CONNECT_AND));

        assertTrue(content.hasNames());
        assertEquals(List.of("Test"), content.getNames());
        assertEquals(SearchContent.CONNECT_AND, content.getSearchMode());
    }

    @Test
    public void splitsTheSearchTextIntoTerms() {
        SearchContent content = new SearchContent(Map.of(
                SearchContent.CONTENT_NAME,
                "König, Bölling;; , glucose.liver",
                SearchContent.CONTENT_MODE,
                SearchContent.CONNECT_OR));

        assertEquals(List.of("König", "Bölling", "glucose", "liver"), content.getNames());
    }

    @Test
    public void hasNoNamesForParsedIds() {
        SearchContent content = new SearchContent(Map.of(SearchContent.CONTENT_MODE, SearchContent.PARSED_IDS));

        assertFalse(content.hasNames());
        assertTrue(content.toHTML().contains(SearchContent.PARSED_IDS));
    }

    @Test
    public void escapesTheSearchTermsInTheHtml() {
        SearchContent content = new SearchContent(Map.of(
                SearchContent.CONTENT_NAME, "<img src=x> a&b", SearchContent.CONTENT_MODE, SearchContent.CONNECT_AND));

        String html = content.toHTML();
        assertFalse(html.contains("<img"), html);
        assertTrue(html.contains("&lt;img src=x&gt; a&amp;b"), html);
    }
}
