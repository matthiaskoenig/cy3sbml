package org.cy3sbml.ols;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.cy3sbml.cache.MemoryCache;
import org.cy3sbml.miriam.Resource;
import org.cy3sbml.util.FetchResult;
import org.cy3sbml.util.FetchStatus;
import org.cy3sbml.util.HttpJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small client for the OLS4 (Ontology Lookup Service) REST API.
 * <p>
 * Looks up ontology terms by their CURIE (e.g. {@code GO:0042752}) or OBO short
 * form (e.g. {@code GO_0042752}). Results are cached in memory since terms do
 * not change within a Cytoscape session.
 */
public final class OlsClient {
    private static final Logger logger = LoggerFactory.getLogger(OlsClient.class);
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^([A-Za-z]+)[:_](.+)$");
    private static final String OLS_BASE_URL = "https://www.ebi.ac.uk/ols4/api/ontologies/";

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
     * Gets the OLS term for a given identifier.
     * Example: "GO:0042752" or "GO_0042752".
     * <p>
     * Returns empty if not an ontology identifier, or no term found.
     */
    public Optional<OlsTerm> term(String identifier) {
        return cache.get(identifier, this::lookup);
    }

    private FetchResult<OlsTerm> lookup(String identifier) {
        Matcher matcher = IDENTIFIER_PATTERN.matcher(identifier);
        if (!matcher.matches()) {
            // deterministic for this identifier: it will never become an ontology one
            logger.warn("Identifier is not an ontology identifier: {}", identifier);
            return FetchResult.notFound();
        }
        String prefix = matcher.group(1);
        String local = matcher.group(2);

        // OBO prefixes are case sensitive in OLS (e.g. NCBITaxon, VariO), annotations
        // sometimes write them in lower case (e.g. go:0006915)
        FetchResult<OlsTerm> term = query(prefix, prefix, local);
        String upperPrefix = prefix.toUpperCase(Locale.ROOT);
        if (term.status() != FetchStatus.FOUND && !upperPrefix.equals(prefix)) {
            term = combine(term, query(prefix, upperPrefix, local));
        }
        return term;
    }

    /** Found wins; else an error on either attempt wins (stay uncached); else not found. */
    private static <T> FetchResult<T> combine(FetchResult<T> first, FetchResult<T> second) {
        if (first.status() == FetchStatus.FOUND) {
            return first;
        }
        if (second.status() == FetchStatus.FOUND) {
            return second;
        }
        if (first.status() == FetchStatus.ERROR || second.status() == FetchStatus.ERROR) {
            return FetchResult.error();
        }
        return FetchResult.notFound();
    }

    private FetchResult<OlsTerm> query(String ontologyPrefix, String oboPrefix, String local) {
        String oboId = oboPrefix + ":" + local;
        URI uri = URI.create(OLS_BASE_URL + ontologyPrefix.toLowerCase(Locale.ROOT) + "/terms?obo_id="
                + URLEncoder.encode(oboId, StandardCharsets.UTF_8));
        FetchResult<JsonNode> json = http.fetch(uri);
        return switch (json.status()) {
            case FOUND -> parseTerm(json.value().orElseThrow(), ontologyPrefix);
            case NOT_FOUND -> FetchResult.notFound();
            case ERROR -> FetchResult.error();
        };
    }

    private static FetchResult<OlsTerm> parseTerm(JsonNode json, String prefix) {
        JsonNode terms = json.path("_embedded").path("terms");
        if (!terms.isArray() || terms.isEmpty()) {
            return FetchResult.notFound();
        }
        JsonNode term = terms.get(0);
        String label = term.path("label").asText(null);
        String iri = term.path("iri").asText(null);
        if (label == null || iri == null) {
            // a structurally incomplete 200 body is deterministic for this identifier
            logger.warn("OLS term is missing label or iri: {}", term);
            return FetchResult.notFound();
        }
        String ontologyName = term.path("ontology_name").asText(null);
        if (ontologyName == null) {
            ontologyName = prefix.toLowerCase(Locale.ROOT);
        }
        return FetchResult.found(new OlsTerm(
                iri,
                label,
                ontologyName,
                term.path("obo_id").asText(null),
                toStringList(term.path("synonyms")),
                toStringList(term.path("description"))));
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

    /**
     * Is a given resource physically located at OLS, i.e. an ontology in OLS.
     */
    public static boolean isOlsResource(Resource resource) {
        return resource.getResourceHomeUrl().contains(org.cy3sbml.IdentifiersConstants.OLS_BASE_URL);
    }
}
