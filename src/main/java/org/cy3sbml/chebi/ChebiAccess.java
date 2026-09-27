package org.cy3sbml.chebi;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.time.Clock;
import java.util.Optional;
import org.apache.commons.text.StringEscapeUtils;
import org.cy3sbml.cache.MemoryCache;
import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.util.FetchResult;
import org.cy3sbml.util.HttpJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for the ChEBI public backend REST API.
 * <p>
 * Looks up compounds by ChEBI id (e.g. {@code CHEBI:15422}). Results are
 * cached in memory since compounds do not change within a Cytoscape session.
 */
public final class ChebiAccess {
    private static final Logger logger = LoggerFactory.getLogger(ChebiAccess.class);

    private final HttpJson http;
    private final MemoryCache<String, ChebiCompound> compoundCache;
    private final MemoryCache<String, String> structureCache;

    public ChebiAccess(HttpJson http) {
        this(http, Clock.systemUTC());
    }

    /** For tests: an injectable clock for the "not found" cache TTL. */
    ChebiAccess(HttpJson http, Clock clock) {
        this.http = http;
        this.compoundCache = new MemoryCache<>(5000, MemoryCache.DEFAULT_NOT_FOUND_TTL, clock);
        this.structureCache = new MemoryCache<>(5000, MemoryCache.DEFAULT_NOT_FOUND_TTL, clock);
    }

    /**
     * Gets the ChEBI compound for a given id, e.g. {@code CHEBI:15422}.
     * Returns empty if the compound could not be retrieved or parsed.
     * A "not found" result is cached for a short TTL; a transport or parse
     * error is retried on the next call rather than stuck for the session.
     */
    public Optional<ChebiCompound> compound(String chebiId) {
        return compoundCache.get(chebiId, this::lookupCompound);
    }

    private FetchResult<ChebiCompound> lookupCompound(String chebiId) {
        URI uri = URI.create(
                String.format("https://www.ebi.ac.uk/chebi/backend/api/public/compound/%s/", chebiNumber(chebiId)));
        FetchResult<JsonNode> json = http.fetch(uri);
        return switch (json.status()) {
            case FOUND -> parseCompound(json.value().orElseThrow(), chebiId);
            case NOT_FOUND -> FetchResult.notFound();
            case ERROR -> FetchResult.error();
        };
    }

    /**
     * Gets the structure image (SVG) for a given ChEBI id.
     * Cached the same way as {@link #compound}.
     */
    private Optional<String> structure(String chebiId) {
        return structureCache.get(chebiId, this::lookupStructure);
    }

    private FetchResult<String> lookupStructure(String chebiId) {
        URI uri = URI.create(String.format(
                "https://www.ebi.ac.uk/chebi/backend/api/public/compound/%s/structure/?width=300&height=300",
                chebiNumber(chebiId)));
        return http.fetchText(uri);
    }

    private static FetchResult<ChebiCompound> parseCompound(JsonNode json, String chebiId) {
        String name = json.path("name").asText(null);
        if (name == null) {
            logger.warn("ChEBI compound {} is missing a name", chebiId);
            return FetchResult.error();
        }
        JsonNode chemicalData = json.path("chemical_data");
        String formula = chemicalData.path("formula").asText(null);
        String charge = chemicalData.path("charge").asText(null);
        String mass = chemicalData.path("mass").asText(null);
        return FetchResult.found(new ChebiCompound(chebiId, name, formula, charge, mass));
    }

    private static String chebiNumber(String chebiId) {
        int idx = chebiId.indexOf(':');
        return idx >= 0 ? chebiId.substring(idx + 1) : chebiId;
    }

    /**
     * Creates the secondary-information HTML fragment for a ChEBI id,
     * for display in the SBase details panel. Composed fresh on every call
     * from the (independently cached) compound and structure lookups, so a
     * partial failure (e.g. while offline) is retried rather than cached.
     */
    public String html(String chebiId) {
        StringBuilder html = new StringBuilder();

        Optional<ChebiCompound> optionalCompound = compound(chebiId);
        if (optionalCompound.isPresent()) {
            ChebiCompound compound = optionalCompound.get();
            if (compound.formula() != null || compound.charge() != null || compound.mass() != null) {
                html.append(GUIConstants.TABLE_START)
                        .append(GUIConstants.TS)
                        .append("Formula")
                        .append(GUIConstants.TM)
                        .append(StringEscapeUtils.escapeHtml4(compound.formula()))
                        .append(GUIConstants.TE)
                        .append(GUIConstants.TS)
                        .append("Charge")
                        .append(GUIConstants.TM)
                        .append(StringEscapeUtils.escapeHtml4(compound.charge()))
                        .append(GUIConstants.TE)
                        .append(GUIConstants.TS)
                        .append("Mass")
                        .append(GUIConstants.TM)
                        .append(StringEscapeUtils.escapeHtml4(compound.mass()))
                        .append(GUIConstants.TE)
                        .append(GUIConstants.TABLE_END);
            }
        }

        Optional<String> svg = structure(chebiId);
        svg.ifPresent(s ->
                html.append(String.format("<a href=\"https://www.ebi.ac.uk/chebi/%s\">%s</a><br />\n", chebiId, s)));

        return html.toString();
    }
}
