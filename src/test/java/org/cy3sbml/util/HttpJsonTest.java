package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HttpJsonTest {
    private HttpServer server;
    private HttpJson httpJson;
    private static final int LIMIT = 1000;

    @TempDir
    Path tempDir;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/malformed", exchange -> respond(exchange, 200, "{not json"));
        server.createContext("/error", exchange -> respond(exchange, 500, ""));
        server.createContext("/empty", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.createContext("/notfound", exchange -> respond(exchange, 404, ""));
        server.createContext("/badrequest", exchange -> respond(exchange, 400, ""));
        server.createContext("/gone", exchange -> respond(exchange, 410, ""));
        server.createContext("/timeout", exchange -> respond(exchange, 408, ""));
        server.createContext("/toomanyrequests", exchange -> respond(exchange, 429, ""));
        server.createContext("/proxyauth", exchange -> respond(exchange, 407, ""));
        server.createContext("/small", exchange -> respond(exchange, 200, "a".repeat(LIMIT)));
        // the size is declared in the Content-Length header
        server.createContext("/large", exchange -> respond(exchange, 200, "a".repeat(LIMIT + 1)));
        // the size is not declared, the body is streamed in chunks
        server.createContext("/largechunked", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            try (var out = exchange.getResponseBody()) {
                for (int i = 0; i < 10; i++) {
                    out.write("a".repeat(LIMIT / 4).getBytes(StandardCharsets.UTF_8));
                    out.flush();
                }
            } catch (IOException e) {
                // the client stops reading once the limit is exceeded
            }
        });
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
    void fetchTreatsAMalformedJsonBodyAsTransient() {
        // a 200 response whose body is not valid JSON (e.g. a captive portal or an
        // intercepting proxy answering with an HTML page) is more likely a transient
        // network-path issue than a deterministic one for this URI
        assertEquals(FetchStatus.ERROR, httpJson.fetch(uri("/malformed")).status());
    }

    @Test
    void fetchTreatsAnEmptyBodyAsTransient() throws Exception {
        // a 2xx response without a body carries no JSON document; like a malformed
        // body it is treated as a transient error and not cached
        assertEquals(FetchStatus.ERROR, httpJson.fetch(uri("/empty")).status());
        assertEquals(FetchStatus.ERROR, httpJson.fetchAsync(uri("/empty")).get().status());
        assertTrue(httpJson.get(uri("/empty")).isEmpty());
    }

    /**
     * A 2xx response whose body is null (e.g. from a custom HttpClient) must never reach
     * {@code FetchResult.found}: it is an ERROR on every fetch path.
     */
    @Test
    @SuppressWarnings("unchecked")
    void fetchTreatsANullBodyAsTransient() throws Exception {
        HttpResponse<Object> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn(null);
        HttpClient client = mock(HttpClient.class);
        when(client.send(any(), any())).thenReturn(response);
        when(client.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(response));
        HttpJson nullBodyJson = new HttpJson(client, new ObjectMapper());
        URI uri = URI.create("http://localhost/null-body");

        assertEquals(FetchStatus.ERROR, nullBodyJson.fetchText(uri).status());
        assertEquals(FetchStatus.ERROR, nullBodyJson.fetch(uri).status());
        assertEquals(FetchStatus.ERROR, nullBodyJson.fetchAsync(uri).get().status());
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
    void fetchTreats407And408And429AsTransient() {
        // 407 (Proxy Authentication Required), 408 (Request Timeout) and 429 (Too Many
        // Requests) are about the server's (or an intermediate proxy's) momentary
        // state, not the request, so unlike other 4xx statuses they stay
        // transient/uncached, like a 5xx
        assertEquals(FetchStatus.ERROR, httpJson.fetch(uri("/proxyauth")).status());
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
        assertEquals(FetchStatus.ERROR, httpJson.fetchText(uri("/proxyauth")).status());
        assertEquals(FetchStatus.ERROR, httpJson.fetchText(uri("/timeout")).status());
        assertEquals(
                FetchStatus.ERROR, httpJson.fetchText(uri("/toomanyrequests")).status());
    }

    @Test
    void downloadWritesTheBodyUpToTheLimit() throws IOException {
        Path file = tempDir.resolve("small.txt");
        httpJson.download(uri("/small"), file, LIMIT);

        assertEquals(LIMIT, Files.size(file));
    }

    @Test
    void downloadRejectsADeclaredSizeAboveTheLimit() throws IOException {
        Path file = Files.createFile(tempDir.resolve("large.txt"));
        IOException e = assertThrows(IOException.class, () -> httpJson.download(uri("/large"), file, LIMIT));

        assertTrue(e.getMessage().contains("larger than the maximum of 1000 bytes"), e.getMessage());
        assertFalse(Files.exists(file));
    }

    @Test
    void downloadStopsAStreamedBodyAboveTheLimit() throws IOException {
        Path file = Files.createFile(tempDir.resolve("largechunked.txt"));
        IOException e = assertThrows(IOException.class, () -> httpJson.download(uri("/largechunked"), file, LIMIT));

        assertTrue(e.getMessage().contains("larger than the maximum of 1000 bytes"), e.getMessage());
        assertFalse(Files.exists(file));
    }

    @Test
    void downloadOfAnErrorStatusRemovesTheFile() throws IOException {
        Path file = Files.createFile(tempDir.resolve("notfound.txt"));
        IOException e = assertThrows(IOException.class, () -> httpJson.download(uri("/notfound"), file));

        assertTrue(e.getMessage().contains("HTTP status 404"), e.getMessage());
        assertFalse(Files.exists(file));
    }
}
