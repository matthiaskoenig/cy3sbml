package org.cy3sbml.miriam;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;


    public class Resource {

        private  int id;
        private  String providerCode;
        private  String name;
        private  String urlPattern;
        private  String mirId;
        private  String description;
        private  boolean official;
        private  String sampleId;
        private  String resourceHomeUrl;
        private  LinkedHashMap institution;
        private  LinkedHashMap location;
        private  boolean deprecated;
        private  String deprecationDate;
        private  String deprecationOfflineDate;
        private  String renderDeprecatedLanding;
        private  String deprecationStatetement;
        private  boolean protectedUrls;
        private  boolean renderProtectedLanding;
        private  String authHelpUrl;
        private  String authHelpDescription;

        public static Resource fromMap(Map<String, Object> value) {
            Resource resource = new Resource();
            resource.id = (int) value.get("id");
            resource.providerCode = value.get("providerCode").toString();
            resource.name = value.get("name").toString();
            resource.urlPattern = value.get("urlPattern").toString();
            resource.mirId = (String) value.get("mirId");
            resource.description = value.get("description").toString();
            resource.official = value.get("official").toString().equals("true");
            resource.sampleId = (String) value.get("sampleId");
            resource.resourceHomeUrl = (String) value.get("resourceHomeUrl");
            resource.institution = (LinkedHashMap) value.get("institution");
            resource.location = (LinkedHashMap) value.get("location");
            resource.deprecated = value.get("deprecated").toString().equals("true");
            resource.deprecationDate = (String) Optional.ofNullable(value.get("deprecationDate"))
                    .orElse("No description available");
            resource.deprecationOfflineDate = (String) Optional.ofNullable(value.get("deprecationOfflineDate"))
                    .orElse("No description available");
            resource.renderDeprecatedLanding = value.get("renderDeprecatedLanding").toString();
            resource.deprecationStatetement =(String) Optional.ofNullable(value.get("deprecationStatement"))
                    .orElse("No description available");
            resource.protectedUrls = value.get("protectedUrls").toString().equals("true");
            resource.renderProtectedLanding = value.get("renderProtectedLanding").toString().equals("true");
            resource.authHelpUrl = (String) Optional.ofNullable(value.get("authHelpUrl"))
                    .orElse("No description available");
            resource.authHelpDescription = (String) Optional.ofNullable(value.get("authHelpDescription"))
                    .orElse("No description available");
            return resource;
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
    public int getId() { return id; }
        public String getName() { return name; }
        public String getProviderCode() { return providerCode; }
        public String getUrlPattern() { return urlPattern; }
        public String getMirId() { return mirId; }
        public String getDescription() { return description; }
        public boolean isOfficial() { return official; }
        public String getSampleId() { return sampleId; }
        public String getResourceHomeUrl() { return resourceHomeUrl; }
        public LinkedHashMap getInstitution() { return institution; }
        public LinkedHashMap getLocation() { return location; }
        public boolean isDeprecated() { return deprecated; }
        public String getDeprecationDate() { return deprecationDate; }
        public String getDeprecationOfflineDate() { return deprecationOfflineDate; }
        public String getRenderDeprecatedLanding() { return renderDeprecatedLanding; }
        public String getDeprecationStatetement() { return deprecationStatetement; }
        public boolean isProtectedUrls() { return protectedUrls; }
        public boolean isRenderProtectedLanding() { return renderProtectedLanding; }
        public String getAuthHelpUrl() { return authHelpUrl; }
        public String getAuthHelpDescription() { return authHelpDescription; }


}

