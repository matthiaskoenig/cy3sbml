package org.cy3sbml.biomodel;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.cy3sbml.util.HtmlUtil;

/**
 * The search of the BioModels dialog: the search terms of the name field and the search
 * mode, which combines them ({@link #CONNECT_AND}, {@link #CONNECT_OR}) or marks ids parsed
 * from text ({@link #PARSED_IDS}).
 */
public final class SearchContent {

    /** The key of the search text in the form fields. */
    public static final String CONTENT_NAME = "NAME";

    /** The key of the search mode in the form fields. */
    public static final String CONTENT_MODE = "MODE";

    /** The search mode in which all terms must match. */
    public static final String CONNECT_AND = "AND";

    /** The search mode in which any term must match. */
    public static final String CONNECT_OR = "OR";

    /** The mode of model ids parsed from text, not searched. */
    public static final String PARSED_IDS = "PARSED IDS";

    /**
     * The separators of the search terms: whitespace, {@code .,;:} and the characters of
     * the search syntax (a term like {@code (} or {@code "} makes the search find nothing),
     * except the wildcards {@code *} and {@code ?}.
     */
    private static final Pattern TERM_SEPARATOR = Pattern.compile("[\\s.,;:()\\[\\]{}\"^~!\\\\/+]+");

    private final List<String> names;
    private final String searchMode;

    /**
     * Creates the search from the form fields.
     *
     * @param fields the search text ({@link #CONTENT_NAME}), split into terms at whitespace
     *     and {@code .,;:} and the search syntax, and the search mode ({@link #CONTENT_MODE}); both are optional
     */
    public SearchContent(Map<String, String> fields) {
        String text = fields.get(CONTENT_NAME);
        names = text != null ? tokens(text) : List.of();
        searchMode = fields.get(CONTENT_MODE);
    }

    /** The search terms, in the order of the search text. */
    public List<String> getNames() {
        return names;
    }

    /** The search terms joined by the separator. */
    public String namesToString(String separator) {
        return String.join(separator, names);
    }

    /** Whether the search has a search term. */
    public boolean hasNames() {
        return !names.isEmpty();
    }

    /** The search mode, null if not set. */
    public String getSearchMode() {
        return searchMode;
    }

    /** The search terms of the text, see {@link #TERM_SEPARATOR}. */
    private static List<String> tokens(String text) {
        return TERM_SEPARATOR
                .splitAsStream(text)
                .filter(token -> !token.isEmpty())
                .toList();
    }

    /** The search terms and the search mode as an HTML table. */
    public String toHTML() {
        return "<table bgcolor=\"#C0C0C0\">" + row("Name", namesToString(" ")) + row("Search Mode", searchMode)
                + "</table>";
    }

    private static String row(String attribute, String value) {
        return "<tr><td><font size=\"-1\"><b>" + attribute + "</b></font></td><td><font size=\"-1\">"
                + HtmlUtil.escape(String.valueOf(value)) + "</font></td></tr>";
    }

    @Override
    public String toString() {
        return String.format("""
                Name : %s
                Mode : %s
                """, namesToString(" "), searchMode);
    }
}
