package org.cy3sbml.sbml4humans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.cy3sbml.util.HttpJson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests the upload to sbml4humans against a local server in the place of its api. */
class Sbml4HumansClientTest {
    private HttpServer server;
    private String origin;
    private volatile String answer = "{\"id\": \"abc\", \"expires\": \"2026-10-02T10:00:00Z\"}";
    private volatile int status = 200;
    private volatile byte[] request;
    private final AtomicInteger requests = new AtomicInteger();

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        origin = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/api/upload", exchange -> {
            requests.incrementAndGet();
            request = exchange.getRequestBody().readAllBytes();
            respond(exchange, status, answer);
        });
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

    private Sbml4HumansClient client() {
        return new Sbml4HumansClient(HttpJson.createDefault(), URI.create(origin + "/"), URI.create(origin + "/api/"));
    }

    @Test
    void uploadsAndReturnsTheAddressOfTheReport() throws Exception {
        URI report = client().upload(new byte[] {1, 2, 3}, "./model.xml", "m1");

        assertEquals(URI.create(origin + "/report?upload=abc&entry=.%2Fmodel.xml&model=m1"), report);
        String body = new String(request, StandardCharsets.ISO_8859_1);
        assertTrue(body.contains("name=\"source\"; filename=\"model.omex\""), body);
    }

    @Test
    void withoutModelTheAddressHasNoModel() throws Exception {
        URI report = client().upload(new byte[] {1}, "./model.xml", null);

        assertEquals(URI.create(origin + "/report?upload=abc&entry=.%2Fmodel.xml"), report);
    }

    @Test
    void errorContractBecomesAnIOException() {
        answer = "{\"errors\": [\"No SBML model could be read\", \"Traceback ...\"], \"warnings\": [], \"info\": {}}";

        IOException error = assertThrows(IOException.class, () -> client().upload(new byte[] {1}, "./m.xml", null));

        assertEquals("sbml4humans could not read the model: No SBML model could be read", error.getMessage());
    }

    @Test
    void answerWithoutIdBecomesAnIOException() {
        answer = "{\"expires\": \"2026-10-02T10:00:00Z\"}";

        IOException error = assertThrows(IOException.class, () -> client().upload(new byte[] {1}, "./m.xml", null));

        assertEquals("sbml4humans gave no upload id", error.getMessage());
    }

    @Test
    void httpErrorBecomesAnIOException() {
        status = 502;

        IOException error = assertThrows(IOException.class, () -> client().upload(new byte[] {1}, "./m.xml", null));

        assertTrue(error.getMessage().endsWith("HTTP status 502"), error.getMessage());
    }

    @Test
    void tooLargeArchiveIsNotSent() {
        byte[] archive = new byte[(int) Sbml4HumansClient.MAX_UPLOAD_BYTES + 1];

        IOException error = assertThrows(IOException.class, () -> client().upload(archive, "./m.xml", null));

        assertEquals("The model is larger than 100 MB, the limit of sbml4humans.", error.getMessage());
        assertEquals(0, requests.get());
    }

    @Test
    void propertiesGiveTheAddresses() {
        Sbml4HumansClient defaults = Sbml4HumansClient.fromProperties(new Properties());
        assertEquals(URI.create("https://sbml4humans.de/"), defaults.url());
        assertEquals(URI.create("https://sbml4humans.de/api/"), defaults.api());

        Properties local = new Properties();
        local.setProperty(Sbml4HumansClient.PROPERTY_URL, "http://localhost:3456");
        local.setProperty(Sbml4HumansClient.PROPERTY_API, "http://localhost:1444/api");
        Sbml4HumansClient client = Sbml4HumansClient.fromProperties(local);
        assertEquals(URI.create("http://localhost:3456/"), client.url());
        assertEquals(URI.create("http://localhost:1444/api/"), client.api());
    }

    @Test
    void blankPropertiesAreTheDefaults() {
        Properties blank = new Properties();
        blank.setProperty(Sbml4HumansClient.PROPERTY_URL, " ");
        assertEquals(
                URI.create("https://sbml4humans.de/"),
                Sbml4HumansClient.fromProperties(blank).url());
    }
}
