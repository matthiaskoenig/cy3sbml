package org.cy3sbml.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small JSON-over-HTTP helper for the REST web services (OLS, ...).
 * <p>
 * {@link #get} and {@link #getText} return an empty {@link Optional} on any
 * non-2xx response, IO error, timeout or malformed JSON body, logging the
 * reason at warn level. {@link #fetch} and {@link #fetchText} return the
 * same information plus a {@link FetchStatus}, distinguishing a deterministic
 * failure for the given URI (a 404 or other non-transient 4xx status) from a
 * transient transport error (a timeout, connection failure, a malformed body -
 * e.g. a captive portal or proxy answering with an HTML page instead of JSON -
 * or a 5xx/407/408/429 status), so callers can cache the former without
 * caching the latter: retrying the deterministic ones would only get the same
 * result again, while the transient ones may well succeed on the next attempt
 * (e.g. once back online). A well-formed JSON body that is missing the fields
 * a specific client needs is deterministic too, but that is for the client
 * (which knows what "required" means for its own response shape) to decide,
 * not this generic helper.
 */
public class HttpJson {
    private static final Logger logger = LoggerFactory.getLogger(HttpJson.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient client;
    private final ObjectMapper mapper;

    public HttpJson(HttpClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /**
     * Creates a default instance that honors the Cytoscape system proxy settings
     * (set as system properties by {@code ConnectionProxy}).
     */
    public static HttpJson createDefault() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .proxy(ProxySelector.getDefault())
                // Force HTTP/1.1: HTTP/2 to ebi.ac.uk fails with "EOF reached while
                // reading" through some network paths (same root cause as the
                // long-standing ChEBI HTTP/2 failures), while HTTP/1.1 is reliable.
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        return new HttpJson(client, new ObjectMapper());
    }

    /**
     * Fetches and parses the JSON body at the given URI.
     * Empty on a non-2xx response, an IO error, a timeout or malformed JSON.
     */
    public Optional<JsonNode> get(URI uri) {
        return fetch(uri).value();
    }

    /**
     * Fetches the raw text body at the given URI (e.g. an SVG image response).
     * Empty on a non-2xx response, an IO error or a timeout.
     */
    public Optional<String> getText(URI uri) {
        return fetchText(uri).value();
    }

    /**
     * Fetches and parses the JSON body at the given URI, distinguishing "not
     * found" (HTTP 404) from a transport or parse error.
     */
    public FetchResult<JsonNode> fetch(URI uri) {
        FetchResult<String> text = fetchText(uri);
        return switch (text.status()) {
            case FOUND -> parse(uri, text.value().orElseThrow());
            case NOT_FOUND -> FetchResult.notFound();
            case ERROR -> FetchResult.error();
        };
    }

    /**
     * Fetches the raw text body at the given URI, distinguishing "not found"
     * (HTTP 404) from a transport error.
     */
    public FetchResult<String> fetchText(URI uri) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .timeout(TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (isDeterministicClientError(status)) {
                // a client error other than a transient rate limit/timeout is
                // deterministic for this URI: retrying it gets the same status again
                logger.debug("Deterministic client error {} for {}", status, uri);
                return FetchResult.notFound();
            }
            if (status < 200 || status >= 300) {
                logger.warn("Unexpected HTTP status {} for {}", status, uri);
                return FetchResult.error();
            }
            return FetchResult.found(response.body());
        } catch (IOException e) {
            logger.warn("Error retrieving {}: {}", uri, e.getMessage());
            return FetchResult.error();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // a cancelled render interrupts its lookups, this is not an error
            logger.debug("Interrupted while retrieving {}", uri);
            return FetchResult.error();
        }
    }

    /**
     * A 4xx status other than 407 (Proxy Authentication Required), 408 (Request Timeout)
     * and 429 (Too Many Requests) is a deterministic client error: the request itself is
     * what is wrong (a bad id, an unsupported ontology prefix, ...), so retrying it
     * against the same URI gets the same status again. 407, 408 and 429 are about the
     * server's (or an intermediate proxy's) momentary state rather than the request
     * itself, so they stay transient/uncached like a 5xx.
     */
    private static boolean isDeterministicClientError(int status) {
        return status >= 400 && status < 500 && status != 407 && status != 408 && status != 429;
    }

    private FetchResult<JsonNode> parse(URI uri, String body) {
        try {
            return FetchResult.found(mapper.readTree(body));
        } catch (IOException e) {
            // A 200 response whose body is not valid JSON is more likely a transient
            // network-path issue (e.g. a captive portal or an intercepting proxy
            // answering with an HTML page instead of the expected JSON) than a
            // permanent, deterministic one for this URI, so it stays uncached/ERROR,
            // unlike a well-formed JSON body missing required fields (a client's own
            // concern, see e.g. OlsClient.parseTerm).
            logger.warn("Error parsing JSON from {}: {}", uri, e.getMessage());
            return FetchResult.error();
        }
    }
}
