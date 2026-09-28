package org.cy3sbml.miriam;

import static org.cy3sbml.miriam.Fields.*;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

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
    private Map<?, ?> institution;
    private Map<?, ?> location;
    private boolean deprecated;
    private String deprecationDate;
    private String deprecationOfflineDate;
    private String renderDeprecatedLanding;
    private String deprecationStatetement;
    private boolean protectedUrls;
    private boolean renderProtectedLanding;
    private String authHelpUrl;
    private String authHelpDescription;

    public static Resource fromMap(Map<?, ?> value) {
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
        resource.institution = (Map<?, ?>) value.get(INSTITUTION);
        resource.location = (Map<?, ?>) value.get(LOCATION);
        resource.deprecated = value.get(DEPRECATED).toString().equals(TRUE);
        resource.deprecationDate = stringOrDefault(value, DEPRECATION_DATE);
        resource.deprecationOfflineDate = stringOrDefault(value, DEPRECATION_OFFLINE_DATE);
        resource.renderDeprecatedLanding = value.get(RENDER_DEPRECATED_LANDING).toString();
        resource.deprecationStatetement = stringOrDefault(value, DEPRECATION_STATEMENT);
        resource.protectedUrls = value.get(PROTECTED_URLS).toString().equals(TRUE);
        resource.renderProtectedLanding =
                value.get(RENDER_PROTECTED_LANDING).toString().equals(TRUE);
        resource.authHelpUrl = stringOrDefault(value, AUTH_HELP_URL);
        resource.authHelpDescription = stringOrDefault(value, AUTH_HELP_DESCRIPTION);
        return resource;
    }

    /** The string value of the key, or {@code NO_DESCRIPTION_AVAILABLE} if it has none. */
    private static String stringOrDefault(Map<?, ?> value, String key) {
        Object item = value.get(key);
        return item == null ? NO_DESCRIPTION_AVAILABLE : (String) item;
    }

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

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getHomeUrl() {
            return homeUrl;
        }

        public String getDescription() {
            return description;
        }

        public String getRorId() {
            return rorId;
        }

        public Location getLocation() {
            return location;
        }
    }

    public static class Location {
        private String countryCode;
        private String countryName;

        @JsonCreator
        public Location(
                @JsonProperty("countryCode") String countryCode, @JsonProperty("countryName") String countryName) {
            this.countryCode = countryCode;
            this.countryName = countryName;
        }

        public String getCountryCode() {
            return countryCode;
        }

        public String getCountryName() {
            return countryName;
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getProviderCode() {
        return providerCode;
    }

    public String getUrlPattern() {
        return urlPattern;
    }

    public String getMirId() {
        return mirId;
    }

    public String getDescription() {
        return description;
    }

    public boolean isOfficial() {
        return official;
    }

    public String getSampleId() {
        return sampleId;
    }

    public String getResourceHomeUrl() {
        return resourceHomeUrl;
    }

    public Map<?, ?> getInstitution() {
        return institution;
    }

    public Map<?, ?> getLocation() {
        return location;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public String getDeprecationDate() {
        return deprecationDate;
    }

    public String getDeprecationOfflineDate() {
        return deprecationOfflineDate;
    }

    public String getRenderDeprecatedLanding() {
        return renderDeprecatedLanding;
    }

    public String getDeprecationStatetement() {
        return deprecationStatetement;
    }

    public boolean isProtectedUrls() {
        return protectedUrls;
    }

    public boolean isRenderProtectedLanding() {
        return renderProtectedLanding;
    }

    public String getAuthHelpUrl() {
        return authHelpUrl;
    }

    public String getAuthHelpDescription() {
        return authHelpDescription;
    }
}
