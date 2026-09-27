package org.cy3sbml.biomodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parsing of the form fields into usable SearchContent instance.
 */
public final class SearchContent {

    public static final String CONTENT_NAME = "NAME";

    public static final String CONTENT_MODE = "MODE";
    public static final String CONNECT_AND = "AND";
    public static final String CONNECT_OR = "OR";
    public static final String PARSED_IDS = "PARSED IDS";

    private List<String> names = new ArrayList<String>();

    private String searchMode;

    public SearchContent(Map<String, String> map) {
        if (map.containsKey(CONTENT_NAME)) {
            names = getTokensFromSearchText(map.get(CONTENT_NAME));
        }
        if (map.containsKey(CONTENT_MODE)) {
            searchMode = map.get(CONTENT_MODE);
        }
    }

    public List<String> getNames() {
        return names;
    }

    public String namesToString(String separator) {
        return getListToString(names, separator);
    }

    public boolean hasNames() {
        return (names.size() > 0);
    }

    // Search Mode
    public String getSearchMode() {
        return searchMode;
    }

    // Parsing form fields
    public List<String> getTokensFromSearchText(String text) {
        String separator = " ";
        String pattern = "([\\s\\.,;:])+";
        text = text.replaceAll(pattern, separator);
        List<String> tokens = new ArrayList<String>();
        String[] tokenArray = text.split(separator);
        if (tokenArray != null) {
            for (String token : tokenArray) {
                token = token.replaceAll("[\\s]", "");
                if (token.length() > 0) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    // Printing
    public String toHTML() {
        return String.format(
                "<table bgcolor=\"#C0C0C0\">" + createHTMLTableRow("Name") + createHTMLTableRow("Search Mode")
                        + "</table>",
                namesToString(" "),
                searchMode);
    }

    private static String createHTMLTableRow(String att) {
        return "<tr><td><font size=\"-1\"><b>" + att + "</b></font></td><td><font size=\"-1\">%s</font></td></tr>";
    }

    @Override
    public String toString() {
        return String.format("""
                Name : %s
                Mode : %s
                """, namesToString(" "), searchMode);
    }

    private static String getListToString(List<String> tokens, String separator) {
        return String.join(separator, tokens);
    }
}
