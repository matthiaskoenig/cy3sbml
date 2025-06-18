package org.cy3sbml.miriam;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Namespace {
    @Getter
    private final int id;
    private final String name;
    private final String pattern;
    private final Boolean namespaceEmbeddedInLui;
    private final String description;
    private final String mirId;
    private final List<Resource> resources;
    private final String created;
    private final String modified;
    private final String sampleId;
    private Boolean deprecated = false;
    private final String deprecationDate;

    public Namespace(Map<Object, Object> value) {
        this.id = (int) value.get("id");
        this.name = (String) value.get("name");
        this.pattern = (String) value.get("pattern");
        this.namespaceEmbeddedInLui = (Boolean) value.get("namespaceEmbeddedInLui");
        this.description = (String) value.get("description");
        this.mirId = (String) value.get("mirId");
        this.resources = (List<Resource>) value.get("resources");
        this.created = (String) value.get("created");
        this.modified = (String) value.get("modified");
        this.sampleId = (String) value.get("sampleId");
        this.deprecated = (Boolean) value.get("deprecated");
        this.deprecationDate = (String) value.get("deprecationDate");
        if (this.name == null){
            throw new IllegalArgumentException("Namespace name cannot be null");
        }

    }
    public int getId() { return id; }
    public String getName() { return name; }
    public String getPattern() { return pattern; }
    public Boolean getNamespaceEmbeddedInLui() { return namespaceEmbeddedInLui; }
    public String getDescription() { return description; }
    public String getMirId() { return mirId; }
    public List<Resource> getResources() { return resources; }
    public String getCreated() { return created; }
    public String getModified() { return modified; }
    public String getSampleId() { return sampleId; }
    public Boolean getDeprecated() { return deprecated; }
    public String getDeprecationDate() { return deprecationDate; }

}
