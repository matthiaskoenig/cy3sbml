package org.cy3sbml.util;

/**
 * Escaping of text for HTML.
 */
public final class HtmlUtil {
    private HtmlUtil() {}

    /**
     * Escapes the characters with a meaning in HTML ({@code & < > "}), so the text shows as
     * is in an element or an attribute value. Other characters are kept: the cy3sbml pages
     * are UTF-8.
     *
     * @return the escaped text, null for null
     */
    public static String escape(String text) {
        if (text == null) {
            return null;
        }
        StringBuilder html = new StringBuilder(text.length() + 16);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&' -> html.append("&amp;");
                case '<' -> html.append("&lt;");
                case '>' -> html.append("&gt;");
                case '"' -> html.append("&quot;");
                default -> html.append(c);
            }
        }
        return html.toString();
    }
}
