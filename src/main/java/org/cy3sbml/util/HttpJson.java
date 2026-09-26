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
 * Returns an empty {@link Optional} on any non-2xx response, IO error,
 * timeout or malformed JSON body, logging the reason at warn level.
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .timeout(TIMEOUT)
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status < 200 || status >= 300) {
                logger.warn("Unexpected HTTP status {} for {}", status, uri);
                return Optional.empty();
            }
            return Optional.of(mapper.readTree(response.body()));
        } catch (IOException e) {
            logger.warn("Error retrieving JSON from {}: {}", uri, e.getMessage());
            return Optional.empty();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while retrieving JSON from {}", uri);
            return Optional.empty();
        }
    }
}
