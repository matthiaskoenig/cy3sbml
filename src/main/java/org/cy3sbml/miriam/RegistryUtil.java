package org.cy3sbml.miriam;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reading the MIRIAM registry of identifiers.org (the JSON of its resolution API) and
 * parsing identifiers.org resource URIs ({@code http(s)://identifiers.org/...} and
 * {@code urn:miriam:...}) without a registry lookup.
 */
public final class RegistryUtil {
    private static final Logger logger = LoggerFactory.getLogger(RegistryUtil.class);

    /** The registry JSON of the identifiers.org resolution API. */
    public static final String URL_MIRIAM_JSON =
            "https://registry.api.identifiers.org/resolutionApi/getResolverDataset";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Classpath location of the bundled offline copy of the registry. */
    static final String BUNDLED_REGISTRY = "/miriam/MiriamRegistry.json";

    /** Maximum size of a downloaded registry (the registry has about 3 MB). */
    static final int MAX_REGISTRY_BYTES = 50 * 1024 * 1024;

    /** The compiled identifier patterns of the data collections, empty for an invalid one. */
    private static final Map<String, Optional<Pattern>> PATTERNS = new ConcurrentHashMap<>();

    private RegistryUtil() {}

    /**
     * Load the offline copy of the registry bundled with the app.
     *
     * @return the data collections by prefix
     */
    public static Map<String, Namespace> loadBundledRegistry() throws IOException {
        try (InputStream in = RegistryUtil.class.getResourceAsStream(BUNDLED_REGISTRY)) {
            if (in == null) {
                throw new FileNotFoundException("Missing resource: " + BUNDLED_REGISTRY);
            }
            return parseRegistry(in.readAllBytes());
        }
    }

    /**
     * Parses the registry JSON into the data collections by prefix. A malformed data
     * collection is skipped.
     *
     * @throws IOException if the JSON is malformed or has no data collection
     */
    static Map<String, Namespace> parseRegistry(byte[] jsonBytes) throws IOException {
        JsonNode namespaces = MAPPER.readTree(jsonBytes).path("payload").path("namespaces");
        if (!namespaces.isArray()) {
            throw new IOException("Missing 'payload.namespaces' array");
        }
        Map<String, Namespace> result = new HashMap<>();
        int skipped = 0;
        for (JsonNode json : namespaces) {
            try {
                Namespace namespace = Namespace.fromJson(json);
                result.put(namespace.getPrefix(), namespace);
            } catch (IllegalArgumentException e) {
                logger.debug("Skipping malformed data collection: {}", e.getMessage());
                skipped++;
            }
        }
        if (skipped > 0) {
            logger.warn("Skipped {} malformed data collections of the MIRIAM registry", skipped);
        }
        if (result.isEmpty()) {
            throw new IOException("The registry has no data collections");
        }
        return result;
    }

    /**
     * Downloads and parses the registry.
     *
     * @param source registry URL, usually {@link #URL_MIRIAM_JSON}
     * @param timeout connect timeout and timeout of every read, so a stalled server fails
     * @return the data collections by prefix
     * @throws IOException if the registry cannot be downloaded or parsed, or is larger than
     *     {@value #MAX_REGISTRY_BYTES} bytes
     */
    public static Map<String, Namespace> download(URI source, Duration timeout) throws IOException {
        int timeoutMillis = Math.toIntExact(timeout.toMillis());
        HttpURLConnection connection = (HttpURLConnection) source.toURL().openConnection();
        try {
            connection.setConnectTimeout(timeoutMillis);
            connection.setReadTimeout(timeoutMillis);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new IOException("HTTP status " + status);
            }
            if (connection.getContentLengthLong() > MAX_REGISTRY_BYTES) {
                throw new IOException("The registry is larger than " + MAX_REGISTRY_BYTES + " bytes");
            }
            try (InputStream in = connection.getInputStream()) {
                byte[] body = in.readNBytes(MAX_REGISTRY_BYTES + 1);
                if (body.length > MAX_REGISTRY_BYTES) {
                    throw new IOException("The registry is larger than " + MAX_REGISTRY_BYTES + " bytes");
                }
                return parseRegistry(body);
            }
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Whether the given resource URI is an identifiers.org URI (http or https, host
     * {@code identifiers.org} or {@code www.identifiers.org}) or a urn:miriam URN, the forms
     * that {@link MiriamRegistry#resolve(String)} resolves against the registry.
     */
    public static boolean isIdentifiersURI(String uri) {
        if (uri == null) {
            return false;
        }
        if (uri.startsWith("urn:miriam:")) {
            return true;
        }
        String host = httpHost(uri);
        return "identifiers.org".equals(host) || "www.identifiers.org".equals(host);
    }

    /**
     * The lowercase host of an http or https URI, null for another URI. Parsed by hand since
     * annotation URIs often contain characters that {@link URI} rejects.
     */
    private static String httpHost(String uri) {
        int schemeEnd = uri.indexOf("://");
        if (schemeEnd < 0) {
            return null;
        }
        String scheme = uri.substring(0, schemeEnd);
        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) {
            return null;
        }
        int start = schemeEnd + 3;
        int end = start;
        while (end < uri.length() && "/?#".indexOf(uri.charAt(end)) < 0) {
            end++;
        }
        String authority = uri.substring(start, end);
        String hostAndPort = authority.substring(authority.lastIndexOf('@') + 1);
        int portStart = hostAndPort.indexOf(':');
        String host = portStart < 0 ? hostAndPort : hostAndPort.substring(0, portStart);
        return host.toLowerCase(Locale.ROOT);
    }

    private static boolean isUrn(String uri) {
        return uri != null && uri.startsWith("urn:");
    }

    /**
     * Decodes the percent-encoded characters of a URI part. Unlike in a form, a plus sign is
     * a plus sign (e.g. the charge in "CA+2"), not a space. The part is kept as it is if it
     * has a malformed percent encoding.
     */
    private static String percentDecode(String s) {
        try {
            return URLDecoder.decode(s.replace("+", "%2B"), StandardCharsets.UTF_8);
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
     * doesn't carry that information. {@code compactAccession} is the part after the prefix
     * colon of a compact URI ({@code https://identifiers.org/<prefix>:<accession>}, with the
     * prefix in any case), null for the other forms; resolving it to the identifier needs the
     * registry, see {@link MiriamRegistry#resolve(String)}.
     */
    record ParsedResourceUri(String namespace, String identifier, String dataCollectionPart, String compactAccession) {}

    /**
     * Splits a resource URI into namespace/identifier/data-collection parts, handling every
     * identifiers.org URI form without a registry lookup:
     * <ul>
     *     <li>{@code urn:miriam:<namespace>:<accession>}</li>
     *     <li>legacy {@code http(s)://identifiers.org/<namespace>/<accession>}</li>
     *     <li>compact {@code https://identifiers.org/<prefix>:<accession>} (a colon in the first
     *         path segment)</li>
     *     <li>a provider-less single path segment that is itself the accession</li>
     * </ul>
     * The accession may itself contain colons (e.g. GO's "GO:0042752") and slashes (e.g. a
     * DOI "10.1016/j.jtbi.2004.04.039"); only the namespace part is split off. Without the
     * registry, the prefix of a compact URI counts as a namespace only if it is lowercase (the
     * identifiers.org convention), so "GO:0042752" keeps "GO:0042752" as the identifier.
     * Returns null for a null or unparsable URI.
     */
    static ParsedResourceUri parse(String uri) {
        if (uri == null) {
            return null;
        }
        if (isUrn(uri)) {
            String[] parts = uri.split(":");
            if (parts.length < 4) {
                return null;
            }
            String namespace = parts[2];
            String identifier =
                    percentDecode(String.join(":", Arrays.asList(parts).subList(3, parts.length)));
            String dataCollectionPart = String.join(":", Arrays.asList(parts).subList(0, 3));
            return new ParsedResourceUri(namespace, identifier, dataCollectionPart, null);
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

        String first = segments.get(0);
        int colonPos = first.indexOf(':');
        if (colonPos == -1) {
            if (segments.size() == 1) {
                return new ParsedResourceUri(null, percentDecode(first), origin + "/", null);
            }
            // legacy form: <namespace>/<accession>, the accession is the rest of the path
            String identifier = percentDecode(String.join("/", segments.subList(1, segments.size())));
            return new ParsedResourceUri(first, identifier, origin + "/" + first + "/", null);
        }

        // compact form: <prefix>:<accession>, the accession is the rest of the path
        String prefix = first.substring(0, colonPos);
        String accession = percentDecode(String.join("/", segments).substring(colonPos + 1));
        String identifier = looksLikeNamespacePrefix(prefix) ? accession : prefix + ":" + accession;
        return new ParsedResourceUri(prefix, identifier, origin + "/" + prefix + "/", accession);
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
     * The patterns (of the data collections in the registry) are compiled once.
     */
    public static boolean checkRegexp(String identifier, String pattern) {
        if (identifier == null || identifier.isEmpty() || pattern == null || pattern.isEmpty()) {
            return false;
        }
        return PATTERNS.computeIfAbsent(pattern, RegistryUtil::compile)
                .map(compiled -> compiled.matcher(identifier).matches())
                .orElse(false);
    }

    private static Optional<Pattern> compile(String pattern) {
        try {
            return Optional.of(Pattern.compile(pattern));
        } catch (PatternSyntaxException e) {
            logger.debug("Invalid identifier pattern <{}>: {}", pattern, e.getMessage());
            return Optional.empty();
        }
    }
}
