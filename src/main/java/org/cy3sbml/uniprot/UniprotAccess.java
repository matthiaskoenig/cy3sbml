package org.cy3sbml.uniprot;

import static org.cy3sbml.uniprot.UniprotHTMLFields.*;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cy3sbml.cache.MemoryCache;
import org.cy3sbml.gui.GUIConstants;
import org.cy3sbml.util.FetchResult;
import org.cy3sbml.util.HtmlUtil;
import org.cy3sbml.util.HttpJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Client for the UniProtKB REST API.
 * <p>
 * Looks up entries by accession (e.g. {@code P10415}). Results are cached in
 * memory since entries do not change within a Cytoscape session.
 */
public final class UniprotAccess {
    private static final Logger logger = LoggerFactory.getLogger(UniprotAccess.class);
    private static final String UNIPROTKB_URL = "https://rest.uniprot.org/uniprotkb/";
    private static final Map<String, String> htmlFragments = GUIConstants.htmlFragments;

    private final HttpJson http;
    private final MemoryCache<String, UniprotEntry> cache;

    public UniprotAccess(HttpJson http) {
        this(http, Clock.systemUTC());
    }

    /** For tests: an injectable clock for the "not found" cache TTL. */
    UniprotAccess(HttpJson http, Clock clock) {
        this.http = http;
        this.cache = new MemoryCache<>(5000, MemoryCache.DEFAULT_NOT_FOUND_TTL, clock);
    }

    /**
     * Gets the UniProt entry for a given accession, e.g. {@code P10415}.
     * Returns empty if the entry could not be retrieved or parsed.
     */
    public Optional<UniprotEntry> entry(String accession) {
        return cache.get(accession, this::lookup);
    }

    private FetchResult<UniprotEntry> lookup(String accession) {
        URI uri = URI.create(UNIPROTKB_URL + URLEncoder.encode(accession, StandardCharsets.UTF_8) + ".json");
        FetchResult<JsonNode> json = http.fetch(uri);
        return switch (json.status()) {
            case FOUND -> parseEntry(json.value().orElseThrow(), accession);
            case NOT_FOUND -> FetchResult.notFound();
            case ERROR -> FetchResult.error();
        };
    }

    private static FetchResult<UniprotEntry> parseEntry(JsonNode json, String accession) {
        String primaryAccession = json.path("primaryAccession").asText(null);
        String uniProtId = json.path("uniProtkbId").asText(null);
        if (primaryAccession == null || uniProtId == null) {
            // a structurally incomplete 200 body is deterministic for this accession
            logger.warn("UniProt entry {} is missing its accession or id", accession);
            return FetchResult.notFound();
        }

        JsonNode description = json.path("proteinDescription");
        JsonNode recommendedName = description.path("recommendedName");
        String fullName = recommendedName.path("fullName").path("value").asText(null);
        List<String> ecNumbers = textValues(recommendedName.path("ecNumbers"), "value");

        List<String> alternativeNames = new ArrayList<>();
        for (JsonNode altName : description.path("alternativeNames")) {
            String value = altName.path("fullName").path("value").asText(null);
            if (value != null) {
                alternativeNames.add(value);
            }
        }

        JsonNode organism = json.path("organism");
        String scientificName = organism.path("scientificName").asText(null);
        String commonName = organism.path("commonName").asText(null);

        List<String> geneNames = new ArrayList<>();
        for (JsonNode gene : json.path("genes")) {
            String value = gene.path("geneName").path("value").asText(null);
            if (value != null) {
                geneNames.add(value);
            }
        }

        List<String> functionComments = new ArrayList<>();
        List<String> catalyticActivities = new ArrayList<>();
        List<String> pathways = new ArrayList<>();
        for (JsonNode comment : json.path("comments")) {
            String commentType = comment.path("commentType").asText("");
            switch (commentType) {
                case "FUNCTION" -> functionComments.addAll(textValues(comment.path("texts"), "value"));
                case "CATALYTIC ACTIVITY" -> {
                    String reactionName = comment.path("reaction").path("name").asText(null);
                    if (reactionName != null) {
                        catalyticActivities.add(reactionName);
                    }
                }
                case "PATHWAY" -> pathways.addAll(textValues(comment.path("texts"), "value"));
                default -> {
                    // other comment types are not shown in the secondary information panel
                }
            }
        }

        return FetchResult.found(new UniprotEntry(
                primaryAccession,
                uniProtId,
                fullName,
                List.copyOf(ecNumbers),
                List.copyOf(alternativeNames),
                scientificName,
                commonName,
                List.copyOf(geneNames),
                List.copyOf(functionComments),
                List.copyOf(catalyticActivities),
                List.copyOf(pathways)));
    }

    private static List<String> textValues(JsonNode arrayNode, String field) {
        List<String> values = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            String value = node.path(field).asText(null);
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    /**
     * Creates the secondary-information HTML fragment for a UniProt accession,
     * for display in the SBase details panel.
     */
    public String html(String accession) {
        StringBuilder html = new StringBuilder("\t<br />\n");
        Optional<UniprotEntry> optionalEntry = entry(accession);
        if (optionalEntry.isEmpty()) {
            return html.toString();
        }
        UniprotEntry entry = optionalEntry.get();

        html.append(htmlFragments
                .get(UNIPROT_LINK)
                .replace(BASE_URL, UNIPROT_URL)
                .replace(ACCESSION, HtmlUtil.escape(accession))
                .replace(UNIPROT_ID, HtmlUtil.escape(entry.uniProtId())));

        if (entry.fullName() != null) {
            html.append("\t<b>").append(HtmlUtil.escape(entry.fullName())).append("</b><br />\n");
        }
        for (String ecNumber : entry.ecNumbers()) {
            html.append("\t<b>EC</b>: ").append(HtmlUtil.escape(ecNumber)).append("<br />\n");
        }

        if (entry.scientificName() != null) {
            String organismStr = entry.scientificName();
            if (entry.commonName() != null) {
                organismStr += " (" + entry.commonName() + ")";
            }
            html.append("\t<b>Organism</b>: ")
                    .append(HtmlUtil.escape(organismStr))
                    .append("<br />\n");
        }

        for (String geneName : entry.geneNames()) {
            html.append("\t<b>Gene</b>: ").append(HtmlUtil.escape(geneName)).append("<br />\n");
        }

        if (!entry.alternativeNames().isEmpty()) {
            html.append("\t<span class=\"comment\">Synonyms</span>");
            for (String altName : entry.alternativeNames()) {
                html.append(HtmlUtil.escape(altName)).append("; ");
            }
            html.append("<br />\n");
        }

        for (String functionComment : entry.functionComments()) {
            html.append(htmlFragments.get(FUNCTION_COMMENT).replace(COMMENT_TEXT, HtmlUtil.escape(functionComment)));
        }
        for (String reactionName : entry.catalyticActivities()) {
            html.append(htmlFragments.get(CATALYTIC_ACTIVITY).replace(REACTION_NAME, HtmlUtil.escape(reactionName)));
        }
        for (String pathway : entry.pathways()) {
            html.append(htmlFragments.get(PATHWAY).replace(PATHWAY_NAME, HtmlUtil.escape(pathway)));
        }

        return html.toString();
    }
}
