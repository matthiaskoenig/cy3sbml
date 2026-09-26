package org.cy3sbml.miriam;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.cy3sbml.util.IOUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.cy3sbml.miriam.Fields.*;

/**
 * Tools for working with Miriam registry.
 * Here the MIRIAM xml file is loaded or updated.
 * http://www.ebi.ac.uk/miriam/main/export/
 */
public class RegistryUtil {
    public static final String PAYLOAD = "payload";
    public static final String NAMESPACES = "namespaces";
    public static final String PREFIX = "prefix";
    private static final Logger logger = LoggerFactory.getLogger(RegistryUtil.class);
    public static final String URL_MIRIAM_JSON = "https://registry.api.identifiers.org/resolutionApi/getResolverDataset";
    private static final ObjectMapper MAPPER = new ObjectMapper();


    /**
     * Load the registry from the resources.
     *
     * @param file MIRIAM json file
     */

    public static Map<String, Namespace> loadRegistry(File file) throws IOException {

        byte[] jsonBytes = Files.readAllBytes(file.toPath());
        JsonNode root = MAPPER.readTree(jsonBytes);
        JsonNode payload = root.path(PAYLOAD);
        if (payload.isMissingNode()) {
            throw new IllegalArgumentException("Missing 'payload' object");
        }
        JsonNode namespaces = payload.path(NAMESPACES);
        if (!namespaces.isArray()) {
            throw new IllegalArgumentException("Missing 'namespaces' array");
        }
        Map<String, Namespace> result = new HashMap<>();
        for (JsonNode nsNode : namespaces) {
            String prefix = nsNode.path(PREFIX).asText(null);
            if (prefix == null || prefix.isEmpty()) continue;
            Map<Object, Object> nsData = MAPPER.convertValue(nsNode, new TypeReference<Map<Object, Object>>() {
            });

            result.put(prefix, new Namespace(nsData));
        }

        return result;
    }

    /**
     * Load the registry from the resources.
     */

    /**
     * Only update MIRIAM if newer version is available.
     * Check last modified and use for update.
     */


    /**
     * Updates the MIRIAM registry file.
     * Downloads json from MIRIAM and saves in file.
     *
     * @param file MIRIAM json file
     */
    public static void updateMiriamJSON(File file) {
        try {
            URL miriamURL = new URL(URL_MIRIAM_JSON);
            IOUtil.saveURLasFile(miriamURL, file);
            logger.info("Updated MIRIAM: " + file.getAbsolutePath());
        } catch (MalformedURLException e) {
            logger.error("MalformedURLException", e);
            e.printStackTrace();
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Script for updating the packaged MIRIAM XML file in src/main/resources.
     */
    public static Map<String, Namespace> getMiriamContent() {
        File f = null;
        Map<String, Namespace> result = null;
        try {
            f = File.createTempFile("MiriamRegistry", ".json");
            RegistryUtil.updateMiriamJSON(f);


            result = RegistryUtil.loadRegistry(f);
        } catch (IOException e) {

        }
        return result;
    }

    public static void main(String[] args) throws FileNotFoundException, MalformedURLException {


    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Small helpers for identifiers.org resource URIs (http(s)://identifiers.org/... and
    // urn:miriam:... URNs), replacing the org.identifiers.registry:registry-lib dependency
    // (org.identifiers.registry.RegistryUtilities) previously used from AnnotationUtil and
    // SBaseHTMLFactory.
    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    private static boolean isUrn(String uri) {
        return uri != null && uri.startsWith("urn:");
    }

    private static String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return s;
        }
    }

    /**
     * Extracts the identifier (accession) part from an identifiers.org resource URI, e.g.
     * "http://identifiers.org/chebi/CHEBI:36927" or "urn:miriam:chebi:CHEBI:36927" both give
     * "CHEBI:36927". Returns null if no identifier part can be found.
     */
    public static String getIdentifierFromURI(String uri) {
        if (uri == null) {
            return null;
        }
        if (isUrn(uri)) {
            String[] parts = uri.split(":");
            if (parts.length < 4) {
                return null;
            }
            String encoded = String.join(":", Arrays.asList(parts).subList(3, parts.length));
            return urlDecode(encoded);
        }
        String element;
        int hashPos = uri.lastIndexOf('#');
        if (hashPos != -1) {
            element = uri.substring(hashPos + 1);
        } else {
            int slashPos = uri.lastIndexOf('/');
            if (slashPos == -1) {
                return null;
            }
            element = uri.substring(slashPos + 1);
        }
        int queryPos = element.indexOf('?');
        if (queryPos != -1) {
            element = element.substring(0, queryPos);
        }
        return urlDecode(element);
    }

    /**
     * Extracts the data collection (namespace) part from an identifiers.org resource URI, e.g.
     * "http://identifiers.org/chebi/CHEBI:36927" gives "http://identifiers.org/chebi" and
     * "urn:miriam:chebi:CHEBI:36927" gives "urn:miriam:chebi". Used for diagnostics when the
     * identifier could not be resolved against the bundled MIRIAM registry.
     */
    public static String getDataCollectionPartFromURI(String uri) {
        if (uri == null) {
            return null;
        }
        if (isUrn(uri)) {
            String[] parts = uri.split(":");
            if (parts.length < 3) {
                return null;
            }
            return String.join(":", Arrays.asList(parts).subList(0, 3));
        }
        int hashPos = uri.lastIndexOf('#');
        if (hashPos != -1) {
            return uri.substring(0, hashPos);
        }
        int slashPos = uri.lastIndexOf('/');
        if (slashPos == -1) {
            return uri;
        }
        return uri.substring(0, slashPos);
    }

    /**
     * Extracts the short namespace prefix from an identifiers.org resource URI, e.g.
     * "http://identifiers.org/chebi/CHEBI:36927" or the compact
     * "https://identifiers.org/chebi:CHEBI:36927" both give "chebi". Returns null if no
     * namespace prefix can be found.
     */
    public static String getNamespaceFromURI(String uri) {
        if (uri == null) {
            return null;
        }
        if (isUrn(uri)) {
            String[] parts = uri.split(":");
            return parts.length >= 3 ? parts[2] : null;
        }
        String withoutScheme = uri.replaceFirst("^https?://[^/]+/", "");
        int slashPos = withoutScheme.indexOf('/');
        if (slashPos != -1) {
            return withoutScheme.substring(0, slashPos);
        }
        int colonPos = withoutScheme.indexOf(':');
        return colonPos == -1 ? null : withoutScheme.substring(0, colonPos);
    }

    /**
     * Returns true if the identifier matches the given regular expression pattern.
     * False if either argument is null, empty, or the pattern is not a valid regex.
     */
    public static boolean checkRegexp(String identifier, String pattern) {
        if (identifier == null || identifier.isEmpty() || pattern == null || pattern.isEmpty()) {
            return false;
        }
        try {
            return Pattern.matches(pattern, identifier);
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

}
