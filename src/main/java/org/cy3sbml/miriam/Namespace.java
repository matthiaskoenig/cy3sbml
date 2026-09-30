package org.cy3sbml.miriam;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;

/**
 * A data collection (namespace) of the MIRIAM registry, e.g. ChEBI with the prefix
 * {@code chebi}: its identifier pattern and the resources that show its entries.
 */
public final class Namespace {
    private final int id;
    private final String prefix;
    private final String name;
    private final String pattern;
    private final Boolean namespaceEmbeddedInLui;
    private final List<Resource> resources;

    Namespace(
            int id,
            String prefix,
            String name,
            String pattern,
            Boolean namespaceEmbeddedInLui,
            List<Resource> resources) {
        this.id = id;
        this.prefix = prefix;
        this.name = name;
        this.pattern = pattern;
        this.namespaceEmbeddedInLui = namespaceEmbeddedInLui;
        this.resources = List.copyOf(resources);
    }

    /**
     * The data collection of the given element of the {@code namespaces} array of the
     * registry JSON.
     *
     * @throws IllegalArgumentException if the data collection has no prefix or name, or one
     *     of its resources has no URL pattern
     */
    static Namespace fromJson(JsonNode json) {
        String prefix = json.path("prefix").asText(null);
        String name = json.path("name").asText(null);
        if (prefix == null || prefix.isEmpty() || name == null) {
            throw new IllegalArgumentException("Namespace without prefix or name: " + json.path("id"));
        }
        List<Resource> resources = new ArrayList<>();
        for (JsonNode resource : json.path("resources")) {
            resources.add(Resource.fromJson(resource));
        }
        JsonNode embedded = json.path("namespaceEmbeddedInLui");
        return new Namespace(
                json.path("id").asInt(),
                prefix,
                name,
                json.path("pattern").asText(null),
                embedded.isBoolean() ? embedded.booleanValue() : null,
                resources);
    }

    /** The id of the data collection in the registry. */
    public int getId() {
        return id;
    }

    /** The prefix of the data collection, e.g. {@code chebi}. */
    public String getPrefix() {
        return prefix;
    }

    /** The name of the data collection, e.g. {@code ChEBI}. */
    public String getName() {
        return name;
    }

    /** The regular expression of the identifiers of the data collection, or null. */
    public String getPattern() {
        return pattern;
    }

    /**
     * Whether the identifiers of the data collection start with its prefix, e.g.
     * {@code CHEBI:36927}; null if the registry does not say.
     */
    public Boolean getNamespaceEmbeddedInLui() {
        return namespaceEmbeddedInLui;
    }

    /** The resources of the data collection, unmodifiable. */
    public List<Resource> getResources() {
        return resources;
    }

    /**
     * The resource to link to: the official resource if it is not deprecated, else the first
     * resource that is not deprecated, else the first resource (a deprecated resource only leads
     * to a deprecation page). Null if the namespace has no resources.
     */
    public Resource getPrimaryResource() {
        Resource fallback = null;
        for (Resource resource : resources) {
            if (resource.isDeprecated()) {
                continue;
            }
            if (resource.isOfficial()) {
                return resource;
            }
            if (fallback == null) {
                fallback = resource;
            }
        }
        if (fallback != null) {
            return fallback;
        }
        return resources.isEmpty() ? null : resources.get(0);
    }
}
