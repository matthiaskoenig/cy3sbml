package org.cy3sbml.reader;

import java.util.Properties;
import org.apache.commons.lang3.StringUtils;
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
            XMLNode notes = sbase.getNotes();
            XMLNode parent = notes;
            XMLNode body = notes.getChildElement("body", (String) null);
            if (body == null) {
                body = notes.getChildElement("p", (String) null);
            }
            if (body == null) {
                body = notes.getChildElement("html", (String) null);
            }
            if (body != null) {
                parent = body;
            }
            for (XMLNode pNode : parent.getChildElements("p", (String) null)) {
                if (pNode.getChildCount() > 0) {
                    String content = pNode.getChild(0).getCharacters();
                    int colonCount = StringUtils.countMatches(content, ":");
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
}
