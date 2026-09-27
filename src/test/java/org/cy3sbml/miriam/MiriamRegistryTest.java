package org.cy3sbml.miriam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpServer;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * The registry starts from the bundled copy and is replaced by a downloaded one only when
 * the download succeeds. Uses local servers, no network access.
 */
class MiriamRegistryTest {
    /** A registry with a single data collection, a copy of GO under another prefix. */
    private static String smallRegistry() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode bundled;
        try (InputStream in = MiriamRegistryTest.class.getResourceAsStream(RegistryUtil.BUNDLED_REGISTRY)) {
            bundled = mapper.readTree(in);
        }
        for (JsonNode namespace : bundled.path("payload").path("namespaces")) {
            if ("go".equals(namespace.path("prefix").asText())) {
                ObjectNode copy = namespace.deepCopy();
                copy.put("prefix", "testcollection");
                ObjectNode root = mapper.createObjectNode();
                root.putObject("payload").putArray("namespaces").add(copy);
                return mapper.writeValueAsString(root);
            }
        }
        throw new IllegalStateException("go missing in bundled registry");
    }

    private HttpServer server;
    private final CountDownLatch release = new CountDownLatch(1);

    @AfterEach
    void stopServer() {
        release.countDown();
        if (server != null) {
            server.stop(0);
        }
    }

    private URI serve(String path, com.sun.net.httpserver.HttpHandler handler) throws Exception {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(path, handler);
        server.start();
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort() + path);
    }

    @Test
    void bundledRegistryIsUsableWithoutNetwork() {
        MiriamRegistry registry = MiriamRegistry.bundled();
        assertNotNull(registry.get("go"));
        assertNotNull(registry.get("chebi"));
        assertNull(registry.get(null));
    }

    @ParameterizedTest
    @CsvSource({
        // legacy <namespace>/<accession>
        "http://identifiers.org/chebi/CHEBI:36927, chebi",
        "https://identifiers.org/uniprot/P12345, uniprot",
        "http://identifiers.org/kegg.pathway/hsa:04360, kegg.pathway",
        // compact <namespace>:<accession>
        "https://identifiers.org/chebi:CHEBI:36927, chebi",
        // accessions with an embedded, uppercase prefix
        "https://identifiers.org/CHEBI:36927, chebi",
        // provider-qualified namespace
        "http://identifiers.org/obo.go/GO:0042752, go",
        "http://identifiers.org/GO:0042752, go",
        // urn:miriam
        "urn:miriam:chebi:CHEBI%3A36927, chebi",
    })
    void findByUriResolvesTheDataCollection(String uri, String prefix) {
        MiriamRegistry registry = MiriamRegistry.bundled();
        Namespace dataCollection = registry.findByURI(uri);
        assertNotNull(dataCollection, uri);
        assertEquals(prefix, dataCollection.getPrefix());
    }

    @Test
    void findByUriOfAnUnknownCollectionIsNull() {
        MiriamRegistry registry = MiriamRegistry.bundled();
        assertNull(registry.findByURI("https://identifiers.org/nosuchcollection/123"));
        assertNull(registry.findByURI("https://identifiers.org/"));
        assertNull(registry.findByURI(null));
    }

    @Test
    void failedRefreshKeepsTheBundledRegistry() {
        MiriamRegistry registry = MiriamRegistry.bundled();
        Map<String, Namespace> before = registry.snapshot();

        // nothing listens on port 1, the connection is refused immediately
        assertFalse(registry.refresh(URI.create("http://127.0.0.1:1/registry"), Duration.ofSeconds(1)));

        assertSame(before, registry.snapshot());
        assertNotNull(registry.get("go"));
    }

    @Test
    void refreshHonorsTheTimeoutOfAServerThatNeverAnswers() throws Exception {
        URI uri = serve("/registry", exchange -> {
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        MiriamRegistry registry = MiriamRegistry.bundled();

        long start = System.nanoTime();
        boolean refreshed = registry.refresh(uri, Duration.ofMillis(500));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertFalse(refreshed);
        assertTrue(elapsedMillis < 5000, "refresh took " + elapsedMillis + " ms");
        assertNotNull(registry.get("go"));
    }

    @Test
    void refreshReplacesTheWholeRegistryAtOnce() throws Exception {
        byte[] body = smallRegistry().getBytes(StandardCharsets.UTF_8);
        URI uri = serve("/registry", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        MiriamRegistry registry = MiriamRegistry.bundled();
        Map<String, Namespace> before = registry.snapshot();

        assertTrue(registry.refreshInBackground(uri).get(10, TimeUnit.SECONDS));

        assertNotNull(registry.get("testcollection"));
        assertNull(registry.get("go"));
        // the previous content is not modified, readers holding it see a consistent registry
        assertNotNull(before.get("go"));
        assertNull(before.get("testcollection"));
    }
}
