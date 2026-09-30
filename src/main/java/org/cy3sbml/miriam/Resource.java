package org.cy3sbml.miriam;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * A resource (provider) of a data collection in the MIRIAM registry: a web site that
 * shows the entries of the collection, e.g. the ChEBI or OLS page of a ChEBI identifier.
 */
public final class Resource {
    private final String urlPattern;
    private final String description;
    private final boolean official;
    private final String resourceHomeUrl;
    private final boolean deprecated;

    Resource(String urlPattern, String description, boolean official, String resourceHomeUrl, boolean deprecated) {
        this.urlPattern = urlPattern;
        this.description = description;
        this.official = official;
        this.resourceHomeUrl = resourceHomeUrl;
        this.deprecated = deprecated;
    }

    /**
     * The resource of the given element of the {@code resources} array of a data collection
     * in the registry JSON.
     *
     * @throws IllegalArgumentException if the resource has no URL pattern
     */
    static Resource fromJson(JsonNode json) {
        String urlPattern = json.path("urlPattern").asText(null);
        if (urlPattern == null || urlPattern.isEmpty()) {
            throw new IllegalArgumentException("Resource without urlPattern: " + json.path("id"));
        }
        return new Resource(
                urlPattern,
                json.path("description").asText(""),
                json.path("official").asBoolean(false),
                json.path("resourceHomeUrl").asText(""),
                json.path("deprecated").asBoolean(false));
    }

    /** The URL of an entry, with the placeholder {@code {$id}} for its identifier. */
    public String getUrlPattern() {
        return urlPattern;
    }

    /** The description of the resource, empty if it has none. */
    public String getDescription() {
        return description;
    }

    /** Whether this is the official resource of the data collection. */
    public boolean isOfficial() {
        return official;
    }

    /** The home page of the resource, empty if it has none. */
    public String getResourceHomeUrl() {
        return resourceHomeUrl;
    }

    /** Whether the resource is deprecated, i.e. its links only lead to a deprecation page. */
    public boolean isDeprecated() {
        return deprecated;
    }
}
