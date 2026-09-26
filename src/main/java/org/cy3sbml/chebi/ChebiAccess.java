package org.cy3sbml.chebi;

import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;
import java.util.Optional;

import org.apache.commons.text.StringEscapeUtils;
import org.cy3sbml.cache.MemoryCache;
import org.cy3sbml.gui.GUIConstants;
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
    private static final String COMPOUND_URL = "https://www.ebi.ac.uk/chebi/backend/api/public/compound/%s/";
    private static final String STRUCTURE_URL =
            "https://www.ebi.ac.uk/chebi/backend/api/public/compound/%s/structure/?width=300&height=300";

    private final HttpJson http;
    private final MemoryCache<String, ChebiCompound> cache = new MemoryCache<>(5000);

    public ChebiAccess(HttpJson http) {
        this.http = http;
    }

    /**
     * Gets the ChEBI compound for a given id, e.g. {@code CHEBI:15422}.
     * Returns empty if the compound could not be retrieved or parsed.
     */
    public Optional<ChebiCompound> compound(String chebiId) {
        return cache.get(chebiId, this::lookup);
    }

    private Optional<ChebiCompound> lookup(String chebiId) {
        URI uri = URI.create(String.format(COMPOUND_URL, chebiNumber(chebiId)));
        return http.get(uri).flatMap(json -> parseCompound(json, chebiId));
    }

    private static Optional<ChebiCompound> parseCompound(JsonNode json, String chebiId) {
        String name = json.path("name").asText(null);
        if (name == null) {
            logger.warn("ChEBI compound {} is missing a name", chebiId);
            return Optional.empty();
        }
        JsonNode chemicalData = json.path("chemical_data");
        String formula = chemicalData.path("formula").asText(null);
        String charge = chemicalData.has("charge") ? chemicalData.path("charge").asText() : null;
        String mass = chemicalData.path("mass").asText(null);
        return Optional.of(new ChebiCompound(chebiId, name, formula, charge, mass));
    }

    private static String chebiNumber(String chebiId) {
        int idx = chebiId.indexOf(':');
        return idx >= 0 ? chebiId.substring(idx + 1) : chebiId;
    }

    /**
     * Creates the secondary-information HTML fragment for a ChEBI id,
     * for display in the SBase details panel.
     */
    public String html(String chebiId) {
        StringBuilder html = new StringBuilder();

        Optional<ChebiCompound> optionalCompound = compound(chebiId);
        if (optionalCompound.isPresent()) {
            ChebiCompound compound = optionalCompound.get();
            if (compound.formula() != null || compound.charge() != null || compound.mass() != null) {
                html.append(GUIConstants.TABLE_START)
                        .append(GUIConstants.TS).append("Formula").append(GUIConstants.TM)
                        .append(StringEscapeUtils.escapeHtml4(compound.formula())).append(GUIConstants.TE)
                        .append(GUIConstants.TS).append("Charge").append(GUIConstants.TM)
                        .append(StringEscapeUtils.escapeHtml4(compound.charge())).append(GUIConstants.TE)
                        .append(GUIConstants.TS).append("Mass").append(GUIConstants.TM)
                        .append(StringEscapeUtils.escapeHtml4(compound.mass())).append(GUIConstants.TE)
                        .append(GUIConstants.TABLE_END);
            }
        }

        String structureUri = String.format(STRUCTURE_URL, chebiNumber(chebiId));
        Optional<String> svg = http.getText(URI.create(structureUri));
        svg.ifPresent(s -> html.append(String.format(
                "<a href=\"https://www.ebi.ac.uk/chebi/%s\">%s</a><br />\n",
                chebiId, s)));

        return html.toString();
    }
}
