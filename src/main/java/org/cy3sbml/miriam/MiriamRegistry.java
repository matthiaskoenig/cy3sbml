package org.cy3sbml.miriam;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MIRIAM registry of data collections used to resolve annotation URIs.
 * <p>
 * Starts with the copy bundled with the app, so it is usable immediately and without
 * network. {@link #refreshInBackground(URI)} replaces it with the current registry from
 * identifiers.org once that download succeeds; readers see either the complete old or the
 * complete new registry.
 */
public final class MiriamRegistry {
    private static final Logger logger = LoggerFactory.getLogger(MiriamRegistry.class);

    /** The current registry at identifiers.org. */
    public static final URI ONLINE_REGISTRY = URI.create(RegistryUtil.URL_MIRIAM_JSON);

    /** Connect and read timeout of the registry download. */
    public static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(10);

    private final AtomicReference<Map<String, Namespace>> namespaces;

    MiriamRegistry(Map<String, Namespace> namespaces) {
        this.namespaces = new AtomicReference<>(Map.copyOf(namespaces));
    }

    /**
     * Creates the registry from the copy bundled with the app.
     */
    public static MiriamRegistry bundled() {
        try {
            return new MiriamRegistry(RegistryUtil.loadBundledRegistry());
        } catch (IOException e) {
            throw new UncheckedIOException("Bundled MIRIAM registry could not be loaded", e);
        }
    }

    /**
     * The data collection with the given prefix, or null.
     */
    public Namespace get(String prefix) {
        return prefix == null ? null : namespaces.get().get(prefix);
    }

    /**
     * The data collection of the given identifiers.org resource URI or urn:miriam URN, or
     * null. The namespace of the URI is matched case-insensitively (e.g. "NCBITaxon" gives
     * "ncbitaxon"); a namespace with a provider part such as "obo.go" falls back to the part
     * after the dot.
     */
    public Namespace findByURI(String resourceURI) {
        String namespace = RegistryUtil.getNamespaceFromURI(resourceURI);
        if (namespace == null) {
            return null;
        }
        String prefix = namespace.toLowerCase(Locale.ROOT);
        Namespace dataCollection = get(prefix);
        if (dataCollection == null && prefix.contains(".")) {
            dataCollection = get(prefix.substring(prefix.indexOf('.') + 1));
        }
        return dataCollection;
    }

    /**
     * The current content, an unmodifiable map from prefix to data collection.
     */
    public Map<String, Namespace> snapshot() {
        return namespaces.get();
    }

    /**
     * Replaces the registry with the one downloaded from the source.
     * On failure the current registry stays in use.
     *
     * @return true if the registry was replaced
     */
    public boolean refresh(URI source, Duration timeout) {
        try {
            Map<String, Namespace> downloaded = RegistryUtil.download(source, timeout);
            namespaces.set(Map.copyOf(downloaded));
            logger.info("Updated MIRIAM registry from {}", source);
            return true;
        } catch (IOException | RuntimeException e) {
            logger.warn(
                    "MIRIAM registry could not be updated from {}, using the bundled registry: {}",
                    source,
                    e.toString());
            return false;
        }
    }

    /**
     * Refreshes the registry from the source on a background thread, so the caller
     * (e.g. the bundle start) is not blocked by the network.
     *
     * @return completes with the result of {@link #refresh(URI, Duration)}
     */
    public CompletableFuture<Boolean> refreshInBackground(URI source) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        Thread thread =
                new Thread(() -> result.complete(refresh(source, DOWNLOAD_TIMEOUT)), "cy3sbml-miriam-registry-refresh");
        thread.setDaemon(true);
        thread.start();
        return result;
    }
}
