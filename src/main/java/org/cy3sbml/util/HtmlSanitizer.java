package org.cy3sbml.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Reduces the XHTML of the notes of a model to formatting markup before it is shown in the
 * info panel, with an allowlist of elements, attributes and link schemes.
 * <p>
 * JavaScript is disabled in the panel, but markup alone can load or navigate: a
 * {@code <meta http-equiv="refresh">} moves the panel to another page without a click, a
 * form, an image map or a frame loads a URL the link handling never sees. So elements that
 * load, embed or submit are removed with their content, and other unknown elements (e.g.
 * {@code html}, {@code body} or RDF put into the notes) are replaced by their content.
 */
public final class HtmlSanitizer {

    /** The formatting elements of XHTML that the notes of models use. */
    private static final Set<String> ALLOWED_ELEMENTS = Set.of(
            "a",
            "abbr",
            "acronym",
            "address",
            "b",
            "big",
            "blockquote",
            "br",
            "caption",
            "center",
            "cite",
            "code",
            "col",
            "colgroup",
            "dd",
            "del",
            "dfn",
            "div",
            "dl",
            "dt",
            "em",
            "font",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "hr",
            "i",
            "img",
            "ins",
            "kbd",
            "li",
            "ol",
            "p",
            "pre",
            "q",
            "s",
            "samp",
            "small",
            "span",
            "strike",
            "strong",
            "sub",
            "sup",
            "table",
            "tbody",
            "td",
            "tfoot",
            "th",
            "thead",
            "tr",
            "tt",
            "u",
            "ul",
            "var");

    /** Elements that load, embed, submit or hold no text to show; removed with their content. */
    private static final Set<String> REMOVED_ELEMENTS = Set.of(
            "applet",
            "area",
            "audio",
            "base",
            "button",
            "canvas",
            "embed",
            "form",
            "frame",
            "frameset",
            "head",
            "iframe",
            "input",
            "link",
            "map",
            "math",
            "meta",
            "noscript",
            "object",
            "option",
            "param",
            "portal",
            "script",
            "select",
            "source",
            "style",
            "svg",
            "template",
            "textarea",
            "title",
            "track",
            "video");

    /** Formatting attributes allowed on every allowed element. */
    private static final Set<String> ALLOWED_ATTRIBUTES = Set.of(
            "align",
            "alt",
            "bgcolor",
            "border",
            "cellpadding",
            "cellspacing",
            "class",
            "color",
            "colspan",
            "dir",
            "face",
            "height",
            "lang",
            "rowspan",
            "size",
            "style",
            "summary",
            "title",
            "valign",
            "width");

    /** The schemes of the links the panel opens in the system browser. */
    private static final Set<String> LINK_SCHEMES = Set.of("http", "https", "ftp", "mailto");

    /** The schemes of images. */
    private static final Set<String> IMAGE_SCHEMES = Set.of("http", "https");

    private HtmlSanitizer() {}

    /** Sanitizes the children of the element, the element itself is kept. */
    public static void sanitizeChildren(Element parent) {
        // a copy, the children change while they are sanitized
        List<Node> children = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int k = 0; k < nodes.getLength(); k++) {
            children.add(nodes.item(k));
        }
        for (Node child : children) {
            sanitize(child);
        }
    }

    private static void sanitize(Node node) {
        switch (node.getNodeType()) {
            case Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> {
                // text is escaped when the node is written
            }
            case Node.ELEMENT_NODE -> sanitizeElement((Element) node);
            default -> node.getParentNode().removeChild(node);
        }
    }

    private static void sanitizeElement(Element element) {
        String name = name(element);
        if (REMOVED_ELEMENTS.contains(name)) {
            element.getParentNode().removeChild(element);
            return;
        }
        sanitizeChildren(element);
        if (!ALLOWED_ELEMENTS.contains(name)) {
            // replaced by its (sanitized) content
            Node parent = element.getParentNode();
            while (element.getFirstChild() != null) {
                parent.insertBefore(element.getFirstChild(), element);
            }
            parent.removeChild(element);
            return;
        }
        sanitizeAttributes(element, name);
    }

    private static void sanitizeAttributes(Element element, String elementName) {
        NamedNodeMap attributes = element.getAttributes();
        List<Attr> removed = new ArrayList<>();
        for (int k = 0; k < attributes.getLength(); k++) {
            Attr attribute = (Attr) attributes.item(k);
            if (!isAllowed(elementName, attribute)) {
                removed.add(attribute);
            }
        }
        for (Attr attribute : removed) {
            element.removeAttributeNode(attribute);
        }
    }

    private static boolean isAllowed(String elementName, Attr attribute) {
        String name = attribute.getName().toLowerCase(Locale.ROOT);
        if (name.equals("xmlns") || name.startsWith("xmlns:")) {
            // namespace declarations
            return true;
        }
        String value = attribute.getValue();
        return switch (name) {
            case "href" -> elementName.equals("a") && hasScheme(value, LINK_SCHEMES);
            case "src" -> elementName.equals("img") && hasScheme(value, IMAGE_SCHEMES);
            // CSS can load resources
            case "style" -> !loadsResource(value);
            default -> ALLOWED_ATTRIBUTES.contains(name);
        };
    }

    private static boolean hasScheme(String value, Set<String> schemes) {
        try {
            String scheme = new URI(value.strip()).getScheme();
            return scheme != null && schemes.contains(scheme.toLowerCase(Locale.ROOT));
        } catch (URISyntaxException e) {
            return false;
        }
    }

    private static boolean loadsResource(String style) {
        String css = style.toLowerCase(Locale.ROOT).replaceAll("\\s", "");
        return css.contains("url(") || css.contains("@import") || css.contains("expression(");
    }

    /** The lower case local name of the element, without namespace prefix. */
    private static String name(Element element) {
        String name = element.getLocalName() != null ? element.getLocalName() : element.getTagName();
        int colon = name.indexOf(':');
        return (colon >= 0 ? name.substring(colon + 1) : name).toLowerCase(Locale.ROOT);
    }
}
