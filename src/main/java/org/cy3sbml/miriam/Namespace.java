package org.cy3sbml.miriam;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Namespace {
    @Getter
    private  int id;
    private String prefix;
    private  String name;
    private  String pattern;
    private  Boolean namespaceEmbeddedInLui;
    private  String description;
    private  String mirId;
    private  List<Resource> resources;
    private  String created;
    private  String modified;
    private  String sampleId;
    private Boolean deprecated = false;
    private  String deprecationDate;

    public Namespace(Map<Object, Object> value) {
        this.id = (int) value.get("id");
        this.prefix = (String) value.get("prefix");
        this.name = (String) value.get("name");
        this.pattern = (String) value.get("pattern");
        this.namespaceEmbeddedInLui = (Boolean) value.get("namespaceEmbeddedInLui");
        this.description = (String) value.get("description");
        this.mirId = (String) value.get("mirId");
        //this.resources = (List<Resource>) value.get("resources");
        this.created = (String) value.get("created");
        this.modified = (String) value.get("modified");
        this.sampleId = (String) value.get("sampleId");
        this.deprecated = (Boolean) value.get("deprecated");
        this.deprecationDate = (String) value.get("deprecationDate");
        if (this.name == null){
            throw new IllegalArgumentException("Namespace name cannot be null");
        }
        Object resourcesRaw = value.get("resources");
        if (resourcesRaw instanceof List) {
            this.resources = new ArrayList<>();
            for (Object item : (List<?>) resourcesRaw) {
                if (item instanceof Map) {
                    this.resources.add(Resource.fromMap((Map<String, Object>) item));
                }
            }
        } else {
            this.resources = Collections.emptyList();
        }

    }

    public Namespace(List<Resource> resources) {
        this.resources = resources;
    }

    public int getId() { return id; }
    public String getPrefix() { return prefix; }
    public String getName() { return name; }
    public String getPattern() { return pattern; }
    public Boolean getNamespaceEmbeddedInLui() { return namespaceEmbeddedInLui; }
    public String getDescription() { return description; }
    public String getMirId() { return mirId; }
    public List<Resource> getResources() {

        return resources; }
    public String getCreated() { return created; }
    public String getModified() { return modified; }
    public String getSampleId() { return sampleId; }
    public Boolean getDeprecated() { return deprecated; }
    public String getDeprecationDate() { return deprecationDate; }

}
