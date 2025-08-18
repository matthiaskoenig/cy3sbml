package org.cy3sbml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlTemplateParser {

    public static String load() {
        try {
            return new String(
                    HtmlTemplateParser.class.getClassLoader()
                            .getResourceAsStream("gui/" + "linktemplate.html")
                            .readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template: " + "linktemplate.html", e);
        }
    }
    /**
     * Extracts template sections from HTML into a map preserving insertion order
     * @param htmlTemplate read from the .html file
     * @return Map of template sections (key = section name, value = template content)
     */


    public static Map<String, String> parseTemplateSections(String htmlTemplate) {

        Map<String, String> sections = new LinkedHashMap<>();
        Pattern pattern = Pattern.compile("<!--\\s*(\\w+)\\s*-->([\\s\\S]*?)<!--\\s*/\\1\\s*-->");
        Matcher matcher = pattern.matcher(htmlTemplate);

        while (matcher.find()) {
            String sectionName = matcher.group(1);
            String templateContent = matcher.group(2).trim();
            sections.put(sectionName, templateContent);
        }

        return sections;
    }

    public static void main(String[] args) {
        String htmlTemplate = "<!DOCTYPE html>\n" +
                "<!-- UNIPROT_LINK -->\n" +
                "<a href=\"{BASE_URL}\"><img src=\"./images/logos/uniprot_icon.png\"/></a>\n" +
                "<!-- /UNIPROT_LINK -->\n" +
                "\n" +
                "<!-- FUNCTION_COMMENT -->\n" +
                "<span class=\"comment\">Function</span> {COMMENT_TEXT}<br/>\n" +
                "<!-- /FUNCTION_COMMENT -->";

        Map<String, String> templateMap = parseTemplateSections(htmlTemplate);

        templateMap.forEach((key, value) -> {
            System.out.println("[" + key + "]");
            System.out.println(value);
            System.out.println();
        });
    }
}