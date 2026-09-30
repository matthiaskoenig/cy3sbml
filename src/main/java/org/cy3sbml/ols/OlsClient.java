package org.cy3sbml.ols;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.cy3sbml.IdentifiersConstants;
import org.cy3sbml.cache.MemoryCache;
import org.cy3sbml.miriam.Resource;
import org.cy3sbml.util.FetchResult;
import org.cy3sbml.util.HttpJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small client for the OLS4 (Ontology Lookup Service) REST API.
 * <p>
 * Looks up the ontology term shown by an OLS term page, i.e. the URL an OLS resource of the
 * identifiers.org registry creates for an identifier (e.g.
 * {@code https://www.ebi.ac.uk/ols4/ontologies/go/terms?obo_id=GO:0042752}). The page URL names
 * the ontology and the term (by OBO id, short form or IRI), so no ontology has to be guessed from
 * the identifier prefix. Results are cached in memory since terms do not change within a
 * Cytoscape session.
 */
public final class OlsClient {
    private static final Logger logger = LoggerFactory.getLogger(OlsClient.class);
    private static final String OLS_API_URL = "https://www.ebi.ac.uk/ols4/api/ontologies/";

    /** Term page with the term as query parameter, e.g. {@code go/terms?obo_id=GO:0042752}. */
    private static final Pattern QUERY_PAGE =
            Pattern.compile("^([^/?#]+)/(?:terms|classes)\\?(obo_id|short_form|curie)=([^&#]+)$");

    /** Term page with the (twice URL encoded) term IRI as path segment. */
    private static final Pattern IRI_PAGE = Pattern.compile("^([^/?#]+)/(?:terms|classes)/([^/?#]+)$");

    /** How the OLS API selects a term: by one of these fields of the term. */
    enum TermField {
        OBO_ID("obo_id"),
        SHORT_FORM("short_form"),
        IRI("iri");

        private final String name;

        TermField(String name) {
            this.name = name;
        }
    }

    /** An OLS API query for one term of one ontology. */
    record TermQuery(String ontology, TermField field, String value) {
        URI uri() {
            return URI.create(OLS_API_URL + URLEncoder.encode(ontology, StandardCharsets.UTF_8) + "/terms?" + field.name
                    + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
    }

    private final HttpJson http;
    private final MemoryCache<String, OlsTerm> cache;

    public OlsClient(HttpJson http) {
        this(http, Clock.systemUTC());
    }

    /** For tests: an injectable clock for the "not found" cache TTL. */
    OlsClient(HttpJson http, Clock clock) {
        this.http = http;
        this.cache = new MemoryCache<>(5000, MemoryCache.DEFAULT_NOT_FOUND_TTL, clock);
    }

    /**
     * Gets the OLS term shown by the given OLS term page URL.
     * <p>
     * Returns empty if the URL is no OLS term page, or no term found.
     */
    public Optional<OlsTerm> termForPage(String pageUrl) {
        return cache.get(pageUrl, this::lookup);
    }

    private FetchResult<OlsTerm> lookup(String pageUrl) {
        Optional<TermQuery> query = parsePage(pageUrl);
        if (query.isEmpty()) {
            // deterministic for this URL: it will never become an OLS term page
            logger.warn("URL is not an OLS term page: {}", pageUrl);
            return FetchResult.notFound();
        }
        TermQuery termQuery = query.get();
        FetchResult<JsonNode> json = http.fetch(termQuery.uri());
        return switch (json.status()) {
            case FOUND -> parseTerm(json.value().orElseThrow(), termQuery);
            case NOT_FOUND -> FetchResult.notFound();
            case ERROR -> FetchResult.error();
        };
    }

    /**
     * The API query for an OLS term page URL, empty if the URL is no OLS term page.
     * A {@code curie} parameter is queried as OBO id, since the API ignores it.
     */
    static Optional<TermQuery> parsePage(String pageUrl) {
        if (pageUrl == null || !pageUrl.startsWith(IdentifiersConstants.OLS_BASE_URL)) {
            return Optional.empty();
        }
        String page = pageUrl.substring(IdentifiersConstants.OLS_BASE_URL.length());
        try {
            Matcher matcher = QUERY_PAGE.matcher(page);
            if (matcher.matches()) {
                TermField field = "short_form".equals(matcher.group(2)) ? TermField.SHORT_FORM : TermField.OBO_ID;
                return Optional.of(new TermQuery(matcher.group(1), field, decode(matcher.group(3))));
            }
            matcher = IRI_PAGE.matcher(page);
            if (matcher.matches()) {
                return Optional.of(new TermQuery(matcher.group(1), TermField.IRI, decode(decode(matcher.group(2)))));
            }
        } catch (IllegalArgumentException e) {
            // malformed percent encoding
            logger.warn("Malformed OLS term page URL: {}", pageUrl);
        }
        return Optional.empty();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static FetchResult<OlsTerm> parseTerm(JsonNode json, TermQuery query) {
        JsonNode terms = json.path("_embedded").path("terms");
        if (!terms.isArray() || terms.isEmpty()) {
            return FetchResult.notFound();
        }
        JsonNode term = terms.get(0);
        // the API ignores unknown or unsupported parameters and then lists all terms
        String selected = term.path(query.field().name).asText(null);
        if (!query.value().equalsIgnoreCase(selected)) {
            logger.warn("OLS term <{}> does not match the query {}", selected, query);
            return FetchResult.notFound();
        }
        String label = term.path("label").asText(null);
        String iri = term.path("iri").asText(null);
        if (label == null || iri == null) {
            // a structurally incomplete 200 body is deterministic for this term
            logger.warn("OLS term is missing label or iri: {}", term);
            return FetchResult.notFound();
        }
        String ontologyName = term.path("ontology_name").asText(null);
        if (ontologyName == null) {
            ontologyName = query.ontology();
        }
        return FetchResult.found(new OlsTerm(
                iri, label, ontologyName, toStringList(term.path("synonyms")), toStringList(term.path("description"))));
    }

    private static List<String> toStringList(JsonNode arrayNode) {
        if (!arrayNode.isArray()) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (JsonNode value : arrayNode) {
            values.add(value.asText());
        }
        return List.copyOf(values);
    }

    /** Whether the given resource of a data collection is an ontology in OLS. */
    public static boolean isOlsResource(Resource resource) {
        return resource.getResourceHomeUrl().contains(IdentifiersConstants.OLS_BASE_URL);
    }
}
