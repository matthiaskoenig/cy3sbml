package org.cy3sbml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the HTML fragments of the info panel from the bundled template
 * {@code gui/linktemplate.html}, in which every fragment is enclosed in a pair of comments
 * {@code <!-- NAME -->} and {@code <!-- /NAME -->}.
 */
public final class HtmlTemplateParser {
    private static final String TEMPLATE = "gui/linktemplate.html";
    private static final Pattern SECTION = Pattern.compile("<!--\\s*(\\w+)\\s*-->([\\s\\S]*?)<!--\\s*/\\1\\s*-->");

    private HtmlTemplateParser() {}

    /**
     * Reads the bundled template.
     *
     * @throws UncheckedIOException if the template cannot be read
     */
    public static String load() {
        try (InputStream stream = HtmlTemplateParser.class.getClassLoader().getResourceAsStream(TEMPLATE)) {
            if (stream == null) {
                throw new IOException("Resource not found");
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load template: " + TEMPLATE, e);
        }
    }

    /**
     * Extracts the template sections of the HTML, in the order of the template.
     *
     * @param htmlTemplate the template
     * @return the trimmed content of every section by section name
     */
    public static Map<String, String> parseTemplateSections(String htmlTemplate) {
        Map<String, String> sections = new LinkedHashMap<>();
        Matcher matcher = SECTION.matcher(htmlTemplate);
        while (matcher.find()) {
            sections.put(matcher.group(1), matcher.group(2).trim());
        }
        return sections;
    }
}
