package org.cy3sbml.miriam;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


    public class Resource {
        @Getter
        private final int id;
        private final String providerCode;
        private final String name;
        private final String urlPattern;
        private final String mirId;
        private final String description;
        private final boolean official;
        private final String sampleId;
        private final String resourceHomeUrl;
        private final LinkedHashMap institution;
        private final LinkedHashMap location;
        private final boolean deprecated;
        private final String deprecationDate;
        private final String deprecationOfflineDate;
        private final String renderDeprecatedLanding;
        private final String deprecationStatetement;
        private final boolean protectedUrls;
        private final boolean renderProtectedLanding;
        private final String authHelpUrl;
        private final String authHelpDescription;

        public Resource(Map<Object, Object> value) {

            this.id = (int) value.get("id");
            this.providerCode = value.get("providerCode").toString();
            this.name = value.get("name").toString();
            this.urlPattern = value.get("urlPattern").toString();
            this.mirId = (String) value.get("mirId");
            this.description = value.get("description").toString();
            this.official = value.get("official").toString().equals("true");
            this.sampleId = (String) value.get("sampleId");
            this.resourceHomeUrl = (String) value.get("resourceHomeUrl");
            this.institution = (LinkedHashMap) value.get("institution");
            this.location = (LinkedHashMap) value.get("location");
            this.deprecated = value.get("deprecated").toString().equals("true");
            this.deprecationDate = (String) Optional.ofNullable(value.get("deprecationDate"))
                    .orElse("No description available");
            this.deprecationOfflineDate = (String) Optional.ofNullable(value.get("deprecationOfflineDate"))
                    .orElse("No description available");
            this.renderDeprecatedLanding = value.get("renderDeprecatedLanding").toString();
            this.deprecationStatetement =(String) Optional.ofNullable(value.get("deprecationStatement"))
                    .orElse("No description available");
            this.protectedUrls = value.get("protectedUrls").toString().equals("true");
            this.renderProtectedLanding = value.get("renderProtectedLanding").toString().equals("true");
            this.authHelpUrl = (String) Optional.ofNullable(value.get("authHelpUrl"))
                    .orElse("No description available");
            this.authHelpDescription = (String) Optional.ofNullable(value.get("authHelpDescription"))
                    .orElse("No description available");
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
                @JsonProperty("id") Integer id,
                @JsonProperty("name") String name,
                @JsonProperty("homeUrl") String homeUrl,
                @JsonProperty("description") String description,
                @JsonProperty("rorId") String rorId,
                @JsonProperty("location") Location location) {
            this.id = id;
            this.name = name;
            this.homeUrl = homeUrl;
            this.description = description;
            this.rorId = rorId;
            this.location = location;
        }
        public int getId() { return id; }
        public String getName() { return name; }
        public String getHomeUrl() { return homeUrl; }
        public String getDescription() { return description; }
        public String getRorId() { return rorId; }
        public Location getLocation() { return location; }

    }
    public static class Location {
        private String countryCode;
        private String countryName;
        @JsonCreator // For Jackson deserialization
        public Location(
                @JsonProperty("countryCode") String countryCode,
                @JsonProperty("countryName") String countryName) {
            this.countryCode = countryCode;
            this.countryName = countryName;
        }
        public String getCountryCode() { return countryCode; }
        public String getCountryName() { return countryName; }
    }

}

