package org.cy3sbml.miriam;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.cy3sbml.miriam.Fields.*;

@Getter
public class Namespace {

    private int id;
    private String prefix;
    private String name;
    private String pattern;
    private Boolean namespaceEmbeddedInLui;
    private String description;
    private String mirId;
    private List<Resource> resources;
    private String created;
    private String modified;
    private String sampleId;
    private Boolean deprecated = false;
    private String deprecationDate;

    public Namespace(Map<Object, Object> value) {
        this.id = (int) value.get(ID);
        this.prefix = (String) value.get(PREFIX);
        this.name = (String) value.get(NAME);
        this.pattern = (String) value.get(PATTERN);
        this.namespaceEmbeddedInLui = (Boolean) value.get(NAMESPACE_EMBEDDED_IN_LUI);
        this.description = (String) value.get(DESCRIPTION);
        this.mirId = (String) value.get(MIR_ID);
        //this.resources = (List<Resource>) value.get("resources");
        this.created = (String) value.get(CREATED);
        this.modified = (String) value.get(MODIFIED);
        this.sampleId = (String) value.get(SAMPLE_ID);
        this.deprecated = (Boolean) value.get(DEPRECATED);
        this.deprecationDate = (String) value.get(DEPRECATION_DATE);
        if (this.name == null) {
            throw new IllegalArgumentException("Namespace name cannot be null");
        }
        Object resourcesRaw = value.get(RESOURCES);
        if (resourcesRaw instanceof List) {
            this.resources = new ArrayList<>();
            for (Object item : (List<?>) resourcesRaw) {
                this.resources.add(Resource.fromMap((Map<String, Object>) item));
            }
        } else {
            this.resources = Collections.emptyList();
        }
    }

    public Namespace(List<Resource> resources) {
        this.resources = resources;
    }

}
