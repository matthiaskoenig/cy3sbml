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

    @ParameterizedTest
    @CsvSource({
        // compact identifiers of Faure2006_MammalianCellCycle (#394)
        "https://identifiers.org/BAO:0000362, bao, 0000362",
        "https://identifiers.org/DOI:10.1016/j.jtbi.2004.04.039, doi, 10.1016/j.jtbi.2004.04.039",
        "https://identifiers.org/GO:0007049, go, GO:0007049",
        "https://identifiers.org/biomodels.db:MODEL2006080001, biomodels.db, MODEL2006080001",
        "https://identifiers.org/taxonomy:40674, taxonomy, 40674",
        // namespace embedded in the identifier, with and without the prefix
        "https://identifiers.org/go:GO:0007049, go, GO:0007049",
        "https://identifiers.org/chebi:CHEBI:36927, chebi, CHEBI:36927",
        "https://identifiers.org/CHEBI:36927, chebi, CHEBI:36927",
        // legacy and urn forms
        "http://identifiers.org/go/GO:0042752, go, GO:0042752",
        "https://identifiers.org/doi/10.1016/j.jtbi.2004.04.039, doi, 10.1016/j.jtbi.2004.04.039",
        "urn:miriam:chebi:CHEBI%3A36927, chebi, CHEBI:36927",
    })
    void resolveGivesTheDataCollectionAndAnIdentifierMatchingItsPattern(String uri, String prefix, String identifier) {
        MiriamRegistry.ResolvedURI resolved = MiriamRegistry.bundled().resolve(uri);
        assertNotNull(resolved.dataCollection(), uri);
        assertEquals(prefix, resolved.dataCollection().getPrefix());
        assertEquals(identifier, resolved.identifier());
        assertTrue(
                RegistryUtil.checkRegexp(identifier, resolved.dataCollection().getPattern()), uri);
    }

    @Test
    void resolveOfAnUnknownCollectionKeepsTheIdentifier() {
        MiriamRegistry.ResolvedURI resolved =
                MiriamRegistry.bundled().resolve("https://identifiers.org/nosuchcollection/123");
        assertNull(resolved.dataCollection());
        assertEquals("123", resolved.identifier());
        assertNull(MiriamRegistry.bundled().resolve(null));
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

    private URI serveBody(byte[] body) throws Exception {
        return serve("/registry", exchange -> {
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
    }

    /** A registry without data collections would leave every annotation unresolved. */
    @Test
    void refreshKeepsTheRegistryIfTheDownloadHasNoDataCollections() throws Exception {
        URI uri = serveBody("{\"payload\": {\"namespaces\": []}}".getBytes(StandardCharsets.UTF_8));
        MiriamRegistry registry = MiriamRegistry.bundled();

        assertFalse(registry.refresh(uri, Duration.ofSeconds(5)));
        assertNotNull(registry.get("go"));
    }

    @Test
    void refreshKeepsTheRegistryIfTheDownloadIsTooLarge() throws Exception {
        URI uri = serve("/registry", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            byte[] chunk = new byte[1024 * 1024];
            try (var out = exchange.getResponseBody()) {
                for (int i = 0; i <= RegistryUtil.MAX_REGISTRY_BYTES / chunk.length; i++) {
                    out.write(chunk);
                }
            } catch (java.io.IOException e) {
                // the client stops reading at the limit
            }
        });
        MiriamRegistry registry = MiriamRegistry.bundled();

        assertFalse(registry.refresh(uri, Duration.ofSeconds(5)));
        assertNotNull(registry.get("go"));
    }

    /** A malformed data collection is skipped instead of failing the whole update. */
    @Test
    void parseSkipsAMalformedDataCollection() throws Exception {
        String json = """
                {"payload": {"namespaces": [
                  {"id": 1, "prefix": "noname"},
                  {"id": 2, "prefix": "badresource", "name": "Bad", "resources": [{"id": 3}]},
                  {"id": 4, "prefix": "good", "name": "Good", "pattern": "^\\\\d+$",
                   "namespaceEmbeddedInLui": false,
                   "resources": [{"id": 5, "urlPattern": "https://example.org/{$id}", "official": true}]}
                ]}}""";

        Map<String, Namespace> namespaces = RegistryUtil.parseRegistry(json.getBytes(StandardCharsets.UTF_8));

        assertEquals(1, namespaces.size());
        Namespace good = namespaces.get("good");
        assertEquals("Good", good.getName());
        assertEquals("^\\d+$", good.getPattern());
        assertEquals(false, good.getNamespaceEmbeddedInLui());
        assertEquals("https://example.org/{$id}", good.getPrimaryResource().getUrlPattern());
        assertEquals("", good.getPrimaryResource().getResourceHomeUrl());
    }

    /** The primary resource is the official one, skipping deprecated resources. */
    @Test
    void primaryResourceSkipsDeprecatedResources() {
        Namespace sbo = MiriamRegistry.bundled().findByURI("urn:miriam:biomodels.sbo:SBO%3A0000247");
        assertNotNull(sbo);
        assertTrue(sbo.getResources().get(0).isDeprecated());

        Resource primary = sbo.getPrimaryResource();

        assertFalse(primary.isDeprecated());
        assertTrue(primary.isOfficial());
    }
}
