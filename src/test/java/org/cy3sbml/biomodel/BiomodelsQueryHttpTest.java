package org.cy3sbml.biomodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import org.cy3sbml.util.HttpJson;
import org.cytoscape.work.TaskMonitor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests the BioModels HTTP access against a local server that, like the real
 * BioModels service, answers the old addresses with a 301 redirect to the new ones.
 */
class BiomodelsQueryHttpTest {
    private static final String MODEL_JSON = """
            {"submissionId": "MODEL1234", "publicationId": "BIOMD0000000012",
             "name": "Elowitz2000 - Repressilator", "description": "A repressilator.",
             "publication": {"accession": "10659856", "authors": [{"name": "Elowitz MB"}]}}
            """;
    private static final String SBML = "<?xml version='1.0' encoding='UTF-8'?><sbml level=\"3\" version=\"2\"/>";

    private HttpServer server;
    private String origin;
    private volatile String lastSearchQuery;

    @TempDir
    Path tempDir;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        origin = "http://127.0.0.1:" + server.getAddress().getPort();
        // the old addresses redirect permanently to the new ones
        server.createContext("/old/", exchange -> {
            URI uri = exchange.getRequestURI();
            String location = origin
                    + uri.getRawPath().replaceFirst("^/old/", "/new/")
                    + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery());
            exchange.getResponseHeaders().add("Location", location);
            respond(exchange, 301, "text/html", "<html>Moved Permanently</html>");
        });
        server.createContext("/new/search", exchange -> {
            lastSearchQuery = URLDecoder.decode(
                    exchange.getRequestURI().getRawQuery().replaceFirst("^query=([^&]*).*$", "$1"),
                    StandardCharsets.UTF_8);
            respond(exchange, 200, "application/json", searchJson());
        });
        server.createContext(
                "/new/BIOMD0000000012", exchange -> respond(exchange, 200, "application/json", MODEL_JSON));
        server.createContext("/new/model/download/", exchange -> {
            if (exchange.getRequestURI().getPath().endsWith("/BIOMD0000000012")) {
                respond(exchange, 200, "application/xml", SBML);
            } else {
                respond(exchange, 404, "text/html", "<html>Not Found</html>");
            }
        });
        // a service that is down
        server.createContext("/down/", exchange -> respond(exchange, 503, "text/html", "<html>Unavailable</html>"));
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private static String searchJson() throws IOException {
        try (InputStream in = BiomodelsQueryHttpTest.class.getResourceAsStream("/biomodel/search_glucose.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private BiomodelsQuery query(String basePath) {
        return new BiomodelsQuery(HttpJson.createDefault(), URI.create(origin + basePath));
    }

    @Test
    void searchFollowsRedirects() throws Exception {
        BiomodelsQueryResult result = query("/old/").performSearchQuery("glucose");

        assertTrue(result.success());
        assertEquals(List.of("MODEL1204270001", "MODEL1209260000"), result.getBiomodelIdsFromSearch());
    }

    @Test
    void biomodelQueryFollowsRedirects() throws Exception {
        Biomodel biomodel =
                query("/old/").performBiomodelQuery("BIOMD0000000012").join();

        assertEquals("BIOMD0000000012", biomodel.id());
        assertEquals("Elowitz2000 - Repressilator", biomodel.name());
    }

    @Test
    void biomodelQueryOfUnknownIdFails() {
        CompletionException e = assertThrows(
                CompletionException.class,
                () -> query("/old/").performBiomodelQuery("BIOMD9999999999").join());

        assertTrue(
                e.getCause().getMessage().contains("BIOMD9999999999"),
                e.getCause().getMessage());
    }

    @Test
    void searchOfUnavailableServiceIsUnsuccessful() {
        BiomodelsQueryResult result = query("/down/").performSearchQuery("glucose");

        assertFalse(result.success());
        assertEquals(List.of(), result.getBiomodelIdsFromSearch());
    }

    @Test
    void downloadFollowsRedirects() throws Exception {
        Path file = tempDir.resolve("model.xml");
        query("/old/").downloadSBML("BIOMD0000000012", file);

        assertEquals(SBML, Files.readString(file));
    }

    @Test
    void downloadOfUnknownIdFailsWithTheStatus() {
        Path file = tempDir.resolve("model.xml");
        IOException e = assertThrows(IOException.class, () -> query("/old/").downloadSBML("BIOMD9999999999", file));

        assertTrue(e.getMessage().contains("HTTP status 404"), e.getMessage());
        assertTrue(e.getMessage().contains("BIOMD9999999999"), e.getMessage());
    }

    @Test
    void downloadFromUnreachableServerFailsWithAnIOException() {
        // nothing listens on port 1, the connection is refused immediately
        BiomodelsQuery unreachable = new BiomodelsQuery(HttpJson.createDefault(), URI.create("http://127.0.0.1:1/"));
        Path file = tempDir.resolve("model.xml");

        IOException e = assertThrows(IOException.class, () -> unreachable.downloadSBML("BIOMD0000000012", file));
        assertTrue(e.getMessage().startsWith("Could not download http://127.0.0.1:1/"), e.getMessage());
    }

    @Test
    void searchTaskCombinesTheSearchTermsWithTheSearchMode() throws Exception {
        SearchContent content = new SearchContent(Map.of(
                SearchContent.CONTENT_NAME, "glucose, liver", SearchContent.CONTENT_MODE, SearchContent.CONNECT_OR));
        SearchBioModelTask task = new SearchBioModelTask(content, query("/old/"));

        task.run(mock(TaskMonitor.class));

        assertEquals("glucose OR liver", lastSearchQuery);
        assertEquals(List.of("MODEL1204270001", "MODEL1209260000"), task.getIds());
    }

    @Test
    void searchTaskFailsIfTheSearchFailed() {
        SearchContent content = new SearchContent(
                Map.of(SearchContent.CONTENT_NAME, "glucose", SearchContent.CONTENT_MODE, SearchContent.CONNECT_AND));
        SearchBioModelTask task = new SearchBioModelTask(content, query("/down/"));

        IOException e = assertThrows(IOException.class, () -> task.run(mock(TaskMonitor.class)));
        assertTrue(e.getMessage().contains("glucose"), e.getMessage());
    }
}
