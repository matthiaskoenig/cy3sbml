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
 * same information plus a {@link FetchStatus}, distinguishing "not found"
 * (HTTP 404) from a transport or parse error, so callers can cache a "not
 * found" result without caching a transient error.
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
            if (status == 404) {
                logger.debug("Not found (404) for {}", uri);
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

    private FetchResult<JsonNode> parse(URI uri, String body) {
        try {
            return FetchResult.found(mapper.readTree(body));
        } catch (IOException e) {
            logger.warn("Error parsing JSON from {}: {}", uri, e.getMessage());
            return FetchResult.error();
        }
    }
}
