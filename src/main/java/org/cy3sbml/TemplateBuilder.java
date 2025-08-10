package org.cy3sbml;

import java.util.HashMap;
import java.util.Map;

public class TemplateBuilder {
    private final String fullTemplate;
    private final String blockName;
    private final Map<String, String> replacements = new HashMap<>();

    private TemplateBuilder(String fullTemplate, String blockName) {
        this.fullTemplate = fullTemplate;
        this.blockName = blockName;
    }

    public static TemplateBuilder create(String fullTemplate, String blockName) {
        return new TemplateBuilder(fullTemplate, blockName);
    }

    public TemplateBuilder with(String key, String value) {
        replacements.put(key, value);
        return this;
    }

    public String render() {
        return TemplateLoader.renderTemplateBlock(fullTemplate, blockName, replacements);
    }
}