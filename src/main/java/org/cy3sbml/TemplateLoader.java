package org.cy3sbml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class TemplateLoader {
    public static String load(String filename) {
        try {
            return new String(
                    TemplateLoader.class.getClassLoader()
                            .getResourceAsStream("gui/" + filename)
                            .readAllBytes(),
                    StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to load template: " + filename, e);
        }
    }

    public static String renderTemplateBlock(String fullTemplate, String blockName, Map<String, String> replacements) {

        String blockTemplate = extractTemplateBlock(fullTemplate, blockName);


        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            blockTemplate = blockTemplate.replace("{" + entry.getKey() + "}", entry.getValue());
        }

        return blockTemplate;
    }

    private static String extractTemplateBlock(String fullTemplate, String blockName) {
        String startMarker = "<!-- " + blockName + " -->";
        String endMarker = "<!-- /" + blockName + " -->";

        int start = fullTemplate.indexOf(startMarker);
        if (start == -1) {
            throw new RuntimeException("Start marker not found for: " + blockName);
        }
        start += startMarker.length();

        int end = fullTemplate.indexOf(endMarker, start);
        if (end == -1) {
            throw new RuntimeException("End marker not found for: " + blockName);
        }

        return fullTemplate.substring(start, end).trim();
    }

}
