package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HttpJsonTest {
    private HttpServer server;
    private HttpJson httpJson;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/malformed", exchange -> respond(exchange, 200, "{not json"));
        server.createContext("/error", exchange -> respond(exchange, 500, ""));
        server.createContext("/notfound", exchange -> respond(exchange, 404, ""));
        server.createContext("/badrequest", exchange -> respond(exchange, 400, ""));
        server.createContext("/gone", exchange -> respond(exchange, 410, ""));
        server.createContext("/timeout", exchange -> respond(exchange, 408, ""));
        server.createContext("/toomanyrequests", exchange -> respond(exchange, 429, ""));
        server.start();
        httpJson = new HttpJson(java.net.http.HttpClient.newHttpClient(), new ObjectMapper());
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String body)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + server.getAddress().getPort() + path);
    }

    @Test
    void returnsEmptyOnMalformedJson() {
        assertTrue(httpJson.get(uri("/malformed")).isEmpty());
    }

    @Test
    void returnsEmptyOnHttpError() {
        assertTrue(httpJson.get(uri("/error")).isEmpty());
    }

    @Test
    void fetchTreatsNotFoundAsDeterministic() {
        assertEquals(FetchStatus.NOT_FOUND, httpJson.fetch(uri("/notfound")).status());
    }

    @Test
    void fetchTreatsAMalformedJsonBodyAsDeterministic() {
        // a 200 response whose body is not valid JSON is a deterministic server-side
        // issue for this URI, not a transient one: retrying gets the same body again
        assertEquals(FetchStatus.NOT_FOUND, httpJson.fetch(uri("/malformed")).status());
    }

    @Test
    void fetchTreatsA5xxStatusAsTransient() {
        assertEquals(FetchStatus.ERROR, httpJson.fetch(uri("/error")).status());
    }

    @Test
    void fetchTreatsDeterministicClientErrorsAsNotFound() {
        assertEquals(FetchStatus.NOT_FOUND, httpJson.fetch(uri("/badrequest")).status());
        assertEquals(FetchStatus.NOT_FOUND, httpJson.fetch(uri("/gone")).status());
    }

    @Test
    void fetchTreats408And429AsTransient() {
        // 408 (Request Timeout) and 429 (Too Many Requests) are about the server's
        // momentary state, not the request, so unlike other 4xx statuses they stay
        // transient/uncached, like a 5xx
        assertEquals(FetchStatus.ERROR, httpJson.fetch(uri("/timeout")).status());
        assertEquals(FetchStatus.ERROR, httpJson.fetch(uri("/toomanyrequests")).status());
    }

    @Test
    void fetchTextTreatsDeterministicClientErrorsAsNotFound() {
        assertEquals(FetchStatus.NOT_FOUND, httpJson.fetchText(uri("/notfound")).status());
        assertEquals(
                FetchStatus.NOT_FOUND, httpJson.fetchText(uri("/badrequest")).status());
        assertEquals(FetchStatus.NOT_FOUND, httpJson.fetchText(uri("/gone")).status());
        assertEquals(FetchStatus.ERROR, httpJson.fetchText(uri("/error")).status());
        assertEquals(FetchStatus.ERROR, httpJson.fetchText(uri("/timeout")).status());
        assertEquals(
                FetchStatus.ERROR, httpJson.fetchText(uri("/toomanyrequests")).status());
    }
}
