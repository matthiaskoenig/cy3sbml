package org.cy3sbml.miriam;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.cy3sbml.miriam.Fields.*;


@Getter
public class Resource {
    private int id;
    private String providerCode;
    private String name;
    private String urlPattern;
    private String mirId;
    private String description;
    private boolean official;
    private String sampleId;
    private String resourceHomeUrl;
    private LinkedHashMap institution;
    private LinkedHashMap location;
    private boolean deprecated;
    private String deprecationDate;
    private String deprecationOfflineDate;
    private String renderDeprecatedLanding;
    private String deprecationStatetement;
    private boolean protectedUrls;
    private boolean renderProtectedLanding;
    private String authHelpUrl;
    private String authHelpDescription;

    public static Resource fromMap(Map<String, Object> value) {
        Resource resource = new Resource();
        resource.id = (int) value.get(ID);
        resource.providerCode = value.get(PROVIDER_CODE).toString();
        resource.name = value.get(NAME).toString();
        resource.urlPattern = value.get(URL_PATTERN).toString();
        resource.mirId = (String) value.get(ID1);
        resource.description = value.get(DESCRIPTION).toString();
        resource.official = value.get(OFFICIAL).toString().equals(TRUE);
        resource.sampleId = (String) value.get(ID2);
        resource.resourceHomeUrl = (String) value.get(RESOURCE_HOME_URL);
        resource.institution = (LinkedHashMap) value.get(INSTITUTION);
        resource.location = (LinkedHashMap) value.get(LOCATION);
        resource.deprecated = value.get(DEPRECATED).toString().equals(TRUE);
        resource.deprecationDate = (String) Optional.ofNullable(value.get(DEPRECATION_DATE))
                .orElse(NO_DESCRIPTION_AVAILABLE);
        resource.deprecationOfflineDate = (String) Optional.ofNullable(value.get(DEPRECATION_OFFLINE_DATE))
                .orElse(NO_DESCRIPTION_AVAILABLE);
        resource.renderDeprecatedLanding = value.get(RENDER_DEPRECATED_LANDING).toString();
        resource.deprecationStatetement = (String) Optional.ofNullable(value.get(DEPRECATION_STATEMENT))
                .orElse(NO_DESCRIPTION_AVAILABLE);
        resource.protectedUrls = value.get(PROTECTED_URLS).toString().equals(TRUE);
        resource.renderProtectedLanding = value.get(RENDER_PROTECTED_LANDING).toString().equals(TRUE);
        resource.authHelpUrl = (String) Optional.ofNullable(value.get(AUTH_HELP_URL))
                .orElse(NO_DESCRIPTION_AVAILABLE);
        resource.authHelpDescription = (String) Optional.ofNullable(value.get(AUTH_HELP_DESCRIPTION))
                .orElse(NO_DESCRIPTION_AVAILABLE);
        return resource;
    }

    @Getter
    public static class Institution {
        private int id;
        private String name;
        private String homeUrl;
        private String description;
        private String rorId;
        private Location location;

        @JsonCreator
        public Institution(
                @JsonProperty(ID) Integer id,
                @JsonProperty(NAME) String name,
                @JsonProperty("homeUrl") String homeUrl,
                @JsonProperty(DESCRIPTION) String description,
                @JsonProperty("rorId") String rorId,
                @JsonProperty(LOCATION) Location location) {
            this.id = id;
            this.name = name;
            this.homeUrl = homeUrl;
            this.description = description;
            this.rorId = rorId;
            this.location = location;
        }
    }

    @Getter
    public static class Location {
        private String countryCode;
        private String countryName;

        @JsonCreator
        public Location(
                @JsonProperty("countryCode") String countryCode,
                @JsonProperty("countryName") String countryName) {
            this.countryCode = countryCode;
            this.countryName = countryName;
        }
    }
}

