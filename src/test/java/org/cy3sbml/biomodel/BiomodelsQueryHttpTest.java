package org.cy3sbml.biomodel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;
import org.cy3sbml.util.HttpJson;
import org.cytoscape.task.read.LoadNetworkFileTaskFactory;
import org.cytoscape.work.TaskIterator;
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
    private final List<String> searchPages = new CopyOnWriteArrayList<>();

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
            Map<String, String> parameters =
                    queryParameters(exchange.getRequestURI().getRawQuery());
            lastSearchQuery = parameters.get("query");
            searchPages.add(parameters.get("offset") + "+" + parameters.get("numResults"));
            if (lastSearchQuery.equals("many")) {
                respond(
                        exchange,
                        200,
                        "application/json",
                        manyModelsJson(
                                Integer.parseInt(parameters.get("offset")),
                                Integer.parseInt(parameters.get("numResults"))));
            } else {
                // the recorded response of 2 models of 173 matches, the same for every page
                respond(exchange, 200, "application/json", searchJson());
            }
        });
        server.createContext(
                "/new/MODEL1204270001",
                exchange -> respond(
                        exchange,
                        200,
                        "application/json",
                        "{\"submissionId\": \"MODEL1204270001\", \"name\": \"Koenig2012\", \"publication\": {}}"));
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

    /** Number of models found by the search for "many". */
    private static final int MANY = 250;

    /** A search response page of the models found by the search for "many". */
    private static String manyModelsJson(int offset, int numResults) {
        List<String> models = new ArrayList<>();
        for (int i = offset; i < Math.min(offset + numResults, MANY); i++) {
            models.add(String.format(
                    "{\"id\": \"MODEL%010d\", \"name\": \"Model %d\", \"submissionDate\": \"2012-04-27T00:00:00Z\"}",
                    i, i));
        }
        return "{\"matches\": " + MANY + ", \"models\": [" + String.join(", ", models) + "]}";
    }

    private static Map<String, String> queryParameters(String rawQuery) {
        Map<String, String> parameters = new HashMap<>();
        for (String parameter : rawQuery.split("&", -1)) {
            String[] tokens = parameter.split("=", 2);
            parameters.put(tokens[0], URLDecoder.decode(tokens.length > 1 ? tokens[1] : "", StandardCharsets.UTF_8));
        }
        return parameters;
    }

    private static String searchJson() throws IOException {
        try (InputStream in = BiomodelsQueryHttpTest.class.getResourceAsStream("/biomodel/search_glucose.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
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
        BiomodelsSearchResult result = query("/old/").search("glucose");

        assertEquals(173, result.matches());
        assertEquals(
                List.of("MODEL1204270001", "MODEL1209260000"),
                result.models().stream().map(BiomodelSummary::id).toList());
        assertEquals(
                new BiomodelSummary(
                        "MODEL1204270001", "Koenig2012 - Hepatic Glucose Metabolism", "2012-04-27T00:00:00Z", ""),
                result.models().get(0));
        // the next page brings no new model, the search stops
        assertEquals(List.of("0+100", "2+100"), searchPages);
        assertFalse(result.isComplete());
    }

    @Test
    void searchReadsAllPages() throws Exception {
        BiomodelsSearchResult result = query("/new/").search("many");

        assertEquals(List.of("0+100", "100+100", "200+100"), searchPages);
        assertEquals(MANY, result.matches());
        assertEquals(MANY, result.models().size());
        assertTrue(result.isComplete());
        assertEquals("MODEL0000000000", result.models().get(0).id());
        assertEquals("MODEL0000000249", result.models().get(MANY - 1).id());
    }

    @Test
    void searchReadsAtMostTheMaximumNumberOfModels() throws Exception {
        BiomodelsSearchResult result = query("/new/").search("many", 120);

        assertEquals(List.of("0+100", "100+20"), searchPages);
        assertEquals(MANY, result.matches());
        assertEquals(120, result.models().size());
        assertFalse(result.isComplete());
    }

    @Test
    void searchEscapesTheQuery() throws Exception {
        query("/new/").search("glucose & insulin OR \"liver\"");

        assertEquals("glucose & insulin OR \"liver\"", lastSearchQuery);
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
    void searchOfUnavailableServiceFails() {
        IOException e = assertThrows(IOException.class, () -> query("/down/").search("glucose"));

        assertTrue(e.getMessage().contains("glucose"), e.getMessage());
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
    void searchCombinesTheSearchTermsWithTheSearchModeAndGetsTheInformation() throws Exception {
        SearchContent content = new SearchContent(Map.of(
                SearchContent.CONTENT_NAME, "glucose, liver", SearchContent.CONTENT_MODE, SearchContent.CONNECT_OR));

        SearchBioModel.Result result = new SearchBioModel(query("/old/")).search(content);

        assertEquals("glucose OR liver", lastSearchQuery);
        assertEquals(173, result.matches());
        assertEquals(List.of("MODEL1204270001", "MODEL1209260000"), result.modelIds());
        assertEquals(
                "Koenig2012 - Hepatic Glucose Metabolism",
                result.summaries().get("MODEL1204270001").name());
        assertFalse(result.isParsed());
    }

    @Test
    void getDetailsSkipsModelsThatCannotBeLoaded() {
        // only the first model has details on the server
        Map<String, Biomodel> details =
                new SearchBioModel(query("/old/")).getDetails(List.of("MODEL1204270001", "MODEL1209260000"));

        assertEquals(List.of("MODEL1204270001"), List.copyOf(details.keySet()));
        assertEquals("Koenig2012", details.get("MODEL1204270001").name());
    }

    @Test
    void searchFailsIfTheSearchFailed() {
        SearchContent content = new SearchContent(
                Map.of(SearchContent.CONTENT_NAME, "glucose", SearchContent.CONTENT_MODE, SearchContent.CONNECT_AND));

        IOException e = assertThrows(IOException.class, () -> new SearchBioModel(query("/down/")).search(content));
        assertTrue(e.getMessage().contains("glucose"), e.getMessage());
    }

    @Test
    void loaderDownloadsTheModelsAndLoadsThem() throws Exception {
        LoadNetworkFileTaskFactory loadNetworkFileTaskFactory = mock(LoadNetworkFileTaskFactory.class);
        List<File> loaded = new ArrayList<>();
        when(loadNetworkFileTaskFactory.createTaskIterator(any(File.class))).thenAnswer(invocation -> {
            loaded.add(invocation.getArgument(0));
            return new TaskIterator();
        });
        Path directory = tempDir.resolve("biomodels");
        BiomodelLoader loader = new BiomodelLoader(query("/old/"), directory, loadNetworkFileTaskFactory);

        TaskIterator iterator = loader.createTaskIterator(List.of("BIOMD0000000012", "BIOMD9999999999", "../x"));
        TaskMonitor monitor = mock(TaskMonitor.class);
        IOException e = assertThrows(IOException.class, () -> {
            while (iterator.hasNext()) {
                iterator.next().run(monitor);
            }
        });

        // the downloaded model is loaded, the others are reported at the end
        Path file = directory.resolve("BIOMD0000000012.xml");
        assertEquals(List.of(file.toFile()), loaded);
        assertEquals(SBML, Files.readString(file));
        try (Stream<Path> files = Files.list(directory)) {
            assertEquals(List.of(file), files.toList());
        }
        assertTrue(e.getMessage().contains("BIOMD9999999999"), e.getMessage());
        assertTrue(e.getMessage().contains("HTTP status 404"), e.getMessage());
        assertTrue(e.getMessage().contains("Invalid BioModel id: ../x"), e.getMessage());
        assertFalse(e.getMessage().contains("BIOMD0000000012"), e.getMessage());
    }

    @Test
    void loaderReplacesAnEarlierDownloadOnlyOnSuccess() throws Exception {
        LoadNetworkFileTaskFactory loadNetworkFileTaskFactory = mock(LoadNetworkFileTaskFactory.class);
        Path directory = tempDir.resolve("biomodels");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("BIOMD0000000012.xml"), "old");
        Files.writeString(directory.resolve("BIOMD9999999999.xml"), "old");
        BiomodelLoader loader = new BiomodelLoader(query("/old/"), directory, loadNetworkFileTaskFactory);

        loader.download("BIOMD0000000012");
        assertThrows(IOException.class, () -> loader.download("BIOMD9999999999"));

        assertEquals(SBML, Files.readString(directory.resolve("BIOMD0000000012.xml")));
        assertEquals("old", Files.readString(directory.resolve("BIOMD9999999999.xml")));
    }
}
