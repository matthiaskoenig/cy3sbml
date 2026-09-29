package org.cy3sbml.gui;

import java.util.List;
import org.cy3sbml.util.HtmlUtil;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.fbc.KeyValuePair;
import org.sbml.jsbml.ext.fbc.KeyValuePairs;

/**
 * The HTML of the key-value pairs of an SBase (fbc v3, a {@code listOfKeyValuePairs} in the
 * annotation): a table with a row per pair, the key (with id and name, if set), the value
 * and the uri defining the key, as a link if it is a URL.
 */
public final class KeyValuePairHtml {
    private static final String TABLE_START = "<table class=\"table table-striped table-condensed table-hover\">\n";

    private KeyValuePairHtml() {}

    /** The HTML of the key-value pairs of the SBase, empty if it has none. */
    public static String create(SBase sbase) {
        List<KeyValuePair> pairs = KeyValuePairs.get(sbase);
        if (pairs.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder();
        html.append("<p class=\"cvterm\"><span class=\"qualifier\">key-value pairs</span></p>\n");
        html.append(TABLE_START);
        for (KeyValuePair pair : pairs) {
            html.append("<tr><td><b>")
                    .append(HtmlUtil.escape(pair.getKey()))
                    .append("</b>")
                    .append(idAndName(pair))
                    .append("</td><td>")
                    .append(pair.getValue() != null ? HtmlUtil.escape(pair.getValue()) : "")
                    .append("</td><td>")
                    .append(uri(pair.getUri()))
                    .append("</td></tr>\n");
        }
        html.append("</table>\n");
        return html.toString();
    }

    /** The id and name of the pair after its key, e.g. {@code (kvp1, created)}. */
    private static String idAndName(KeyValuePair pair) {
        String id = pair.getId() != null ? pair.getId() : "";
        String name = pair.getName() != null ? pair.getName() : "";
        String text = String.join(", ", id, name).replaceAll("^, |, $", "");
        return text.isEmpty() ? "" : " <small>(" + HtmlUtil.escape(text) + ")</small>";
    }

    /** The uri, a link if it is a URL (a URN is not). */
    private static String uri(String uri) {
        if (uri == null) {
            return "";
        }
        String escaped = HtmlUtil.escape(uri);
        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            return "<a href=\"" + escaped + "\">" + escaped + "</a>";
        }
        return escaped;
    }
}
