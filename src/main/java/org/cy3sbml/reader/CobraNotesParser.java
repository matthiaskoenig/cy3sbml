package org.cy3sbml.reader;

import java.util.Properties;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.xml.XMLNode;

/**
 * Parses the COBRA key value pairs from the notes of an SBase.
 * <p>
 * COBRA models store information like gene associations as paragraphs
 * {@code <p>KEY: value</p>} in the notes.
 */
final class CobraNotesParser {

    private CobraNotesParser() {}

    /**
     * Parsing of COBRA properties
     */
    static Properties parse(SBase sbase) {
        Properties props = new Properties();
        if (sbase.isSetNotes()) {
            XMLNode parent = paragraphParent(sbase.getNotes());
            for (XMLNode pNode : parent.getChildElements("p", (String) null)) {
                if (pNode.getChildCount() > 0) {
                    String content = pNode.getChild(0).getCharacters();
                    long colonCount = content == null
                            ? 0
                            : content.chars().filter(c -> c == ':').count();
                    if (colonCount == 1) {
                        int firstColonIndex = content.indexOf(":");
                        String key = content.substring(0, firstColonIndex).trim();
                        String value = content.substring(firstColonIndex + 1).trim();
                        // no whitespaces in key
                        if (!key.contains(" ")) {
                            props.setProperty(key, value);
                        }
                    }
                }
            }
        }

        return props;
    }

    /**
     * Returns the element whose {@code <p>} children hold the key value pairs: the
     * {@code <body>} of the notes (directly, or inside an {@code <html>} element), the
     * {@code <html>} element if it has no body, or else the notes element itself, for
     * paragraphs placed directly in the notes.
     */
    private static XMLNode paragraphParent(XMLNode notes) {
        XMLNode body = notes.getChildElement("body", (String) null);
        if (body != null) {
            return body;
        }
        XMLNode html = notes.getChildElement("html", (String) null);
        if (html != null) {
            XMLNode htmlBody = html.getChildElement("body", (String) null);
            return htmlBody != null ? htmlBody : html;
        }
        return notes;
    }
}
