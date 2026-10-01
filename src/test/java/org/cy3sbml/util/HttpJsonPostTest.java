package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests the multipart POST of {@link HttpJson} against a local server. */
class HttpJsonPostTest {
    private HttpServer server;
    private String origin;
    private volatile String contentType;
    private volatile byte[] body;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        origin = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/api/upload", exchange -> {
            contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            body = exchange.getRequestBody().readAllBytes();
            respond(exchange, 200, "{\"id\": \"abc\"}");
        });
        server.createContext("/api/broken", exchange -> respond(exchange, 500, "{}"));
        server.createContext("/api/html", exchange -> respond(exchange, 200, "<html>login</html>"));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static void respond(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private static final byte[] CONTENT = {0x50, 0x4b, 0x03, 0x04, 0x00, (byte) 0xff, 0x0d, 0x0a};

    @Test
    void postsTheContentAsMultipartFormData() throws Exception {
        JsonNode answer = HttpJson.createDefault()
                .postMultipart(URI.create(origin + "/api/upload"), "source", "model.omex", CONTENT);

        assertEquals("abc", answer.get("id").asText());
        assertTrue(contentType.startsWith("multipart/form-data; boundary="), contentType);
        String boundary = contentType.substring(contentType.indexOf("boundary=") + "boundary=".length());
        String text = new String(body, StandardCharsets.ISO_8859_1);
        assertTrue(text.startsWith("--" + boundary + "\r\n"), text);
        assertTrue(text.contains("Content-Disposition: form-data; name=\"source\"; filename=\"model.omex\"\r\n"), text);
        assertTrue(text.contains("\r\n\r\n" + new String(CONTENT, StandardCharsets.ISO_8859_1) + "\r\n--"), text);
        assertTrue(text.endsWith("\r\n--" + boundary + "--\r\n"), text);
    }

    @Test
    void failsWithTheStatus() {
        URI uri = URI.create(origin + "/api/broken");
        IOException error = assertThrows(
                IOException.class, () -> HttpJson.createDefault().postMultipart(uri, "source", "m.omex", CONTENT));

        assertEquals("Could not upload to " + uri + ": HTTP status 500", error.getMessage());
    }

    @Test
    void failsForABodyWhichIsNoJson() {
        URI uri = URI.create(origin + "/api/html");
        IOException error = assertThrows(
                IOException.class, () -> HttpJson.createDefault().postMultipart(uri, "source", "m.omex", CONTENT));

        assertEquals("Could not upload to " + uri + ": the answer is no JSON", error.getMessage());
    }

    @Test
    void failsForAnUnreachableServer() {
        // nothing listens on port 1, the connection is refused immediately
        URI uri = URI.create("http://127.0.0.1:1/api/upload");
        IOException error = assertThrows(
                IOException.class, () -> HttpJson.createDefault().postMultipart(uri, "source", "m.omex", CONTENT));

        assertTrue(error.getMessage().startsWith("Could not upload to " + uri + ": "), error.getMessage());
    }

    @Test
    void refusesAFieldWithQuotes() {
        URI uri = URI.create(origin + "/api/upload");
        assertThrows(
                IllegalArgumentException.class,
                () -> HttpJson.createDefault().postMultipart(uri, "source", "a\"b.omex", CONTENT));
    }
}
