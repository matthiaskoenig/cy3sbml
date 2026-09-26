package org.cy3sbml.miriam;

import static org.cy3sbml.miriam.Fields.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.cy3sbml.util.IOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public static final String URL_MIRIAM_JSON =
            "https://registry.api.identifiers.org/resolutionApi/getResolverDataset";
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
            Map<Object, Object> nsData = MAPPER.convertValue(nsNode, new TypeReference<Map<Object, Object>>() {});

            result.put(prefix, new Namespace(nsData));
        }

        return result;
    }

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
            logger.error("Could not update the MIRIAM registry", e);
        }
        return result;
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
     * True if the given single path segment's prefix (the part before its first colon) is a
     * genuine identifiers.org namespace, recognized purely by the identifiers.org convention
     * that namespace prefixes are lowercase -- no registry lookup involved. This distinguishes
     * the compact form "chebi:CHEBI:36927" (namespace "chebi", accession "CHEBI:36927") from a
     * provider-less URI whose single segment is itself an accession that happens to embed a
     * colon, such as GO's own accessions ("GO:0042752"): "GO" is not lowercase, so the whole
     * segment is kept as the accession instead of being split.
     */
    private static boolean looksLikeNamespacePrefix(String candidate) {
        return !candidate.isEmpty() && candidate.equals(candidate.toLowerCase(Locale.ROOT));
    }

    /**
     * The result of splitting an identifiers.org resource URI (or urn:miriam URN) into its
     * namespace, identifier and data-collection parts. Any of the three may be null if the URI
     * doesn't carry that information.
     */
    private record ParsedResourceUri(String namespace, String identifier, String dataCollectionPart) {}

    /**
     * Splits a resource URI into namespace/identifier/data-collection parts, handling every
     * identifiers.org URI form without a registry lookup:
     * <ul>
     *     <li>{@code urn:miriam:<namespace>:<accession>}</li>
     *     <li>legacy {@code http(s)://identifiers.org/<namespace>/<accession>} (two path
     *         segments)</li>
     *     <li>compact {@code https://identifiers.org/<namespace>:<accession>} (one path segment,
     *         lowercase namespace prefix)</li>
     *     <li>a provider-less single path segment that is itself the accession (kept as-is when
     *         its prefix before the first colon isn't lowercase)</li>
     * </ul>
     * The accession/identifier part may itself contain colons (e.g. GO's "GO:0042752"); only the
     * namespace-prefix colon, if any, is stripped. Returns null for a null or unparsable URI.
     */
    private static ParsedResourceUri parse(String uri) {
        if (uri == null) {
            return null;
        }
        if (isUrn(uri)) {
            String[] parts = uri.split(":");
            if (parts.length < 4) {
                return null;
            }
            String namespace = parts[2];
            String identifier = urlDecode(String.join(":", Arrays.asList(parts).subList(3, parts.length)));
            String dataCollectionPart = String.join(":", Arrays.asList(parts).subList(0, 3));
            return new ParsedResourceUri(namespace, identifier, dataCollectionPart);
        }

        int schemeEnd = uri.indexOf("://");
        int pathStart;
        if (schemeEnd != -1) {
            int slash = uri.indexOf('/', schemeEnd + 3);
            pathStart = (slash == -1) ? uri.length() : slash;
        } else {
            pathStart = 0;
        }
        String origin = uri.substring(0, pathStart);
        String path = uri.substring(pathStart);

        int queryPos = path.indexOf('?');
        if (queryPos != -1) {
            path = path.substring(0, queryPos);
        }
        int hashPos = path.indexOf('#');
        if (hashPos != -1) {
            path = path.substring(0, hashPos);
        }

        List<String> segments = new ArrayList<>();
        for (String segment : path.split("/", -1)) {
            if (!segment.isEmpty()) {
                segments.add(segment);
            }
        }
        if (segments.isEmpty()) {
            return null;
        }

        if (segments.size() >= 2) {
            // legacy form: <namespace>/<accession>; the accession (last segment) may itself
            // contain colons (e.g. GO's "GO:0042752"), so it is kept whole, not split further.
            String namespace = segments.get(0);
            String identifier = urlDecode(segments.get(segments.size() - 1));
            String dataCollectionPart = origin + "/" + namespace + "/";
            return new ParsedResourceUri(namespace, identifier, dataCollectionPart);
        }

        // single path segment: either the compact "<namespace>:<accession>" form, or a
        // provider-less URI whose segment is itself the (possibly colon-containing) accession.
        String segment = segments.get(0);
        int colonPos = segment.indexOf(':');
        if (colonPos == -1) {
            return new ParsedResourceUri(null, urlDecode(segment), origin + "/");
        }
        String candidatePrefix = segment.substring(0, colonPos);
        String identifier = looksLikeNamespacePrefix(candidatePrefix)
                ? urlDecode(segment.substring(colonPos + 1))
                : urlDecode(segment);
        String dataCollectionPart = origin + "/" + candidatePrefix + "/";
        return new ParsedResourceUri(candidatePrefix, identifier, dataCollectionPart);
    }

    /**
     * Extracts the identifier (accession) part from an identifiers.org resource URI, e.g.
     * "http://identifiers.org/chebi/CHEBI:36927", "https://identifiers.org/chebi:CHEBI:36927"
     * or "urn:miriam:chebi:CHEBI:36927" all give "CHEBI:36927". Returns null if no identifier
     * part can be found.
     */
    public static String getIdentifierFromURI(String uri) {
        ParsedResourceUri parsed = parse(uri);
        return parsed == null ? null : parsed.identifier();
    }

    /**
     * Extracts the data collection part from an identifiers.org resource URI, e.g.
     * "http://identifiers.org/chebi/CHEBI:36927" gives "http://identifiers.org/chebi/" and
     * "urn:miriam:chebi:CHEBI:36927" gives "urn:miriam:chebi". Used for diagnostics when the
     * identifier could not be resolved against the bundled MIRIAM registry.
     */
    public static String getDataCollectionPartFromURI(String uri) {
        ParsedResourceUri parsed = parse(uri);
        return parsed == null ? null : parsed.dataCollectionPart();
    }

    /**
     * Extracts the short namespace prefix from an identifiers.org resource URI, e.g.
     * "http://identifiers.org/chebi/CHEBI:36927" or the compact
     * "https://identifiers.org/chebi:CHEBI:36927" both give "chebi". Returns null if no
     * namespace prefix can be found.
     */
    public static String getNamespaceFromURI(String uri) {
        ParsedResourceUri parsed = parse(uri);
        return parsed == null ? null : parsed.namespace();
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
