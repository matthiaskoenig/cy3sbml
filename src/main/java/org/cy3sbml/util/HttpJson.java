package org.cy3sbml.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small JSON-over-HTTP helper for the REST web services (OLS, ...).
 * <p>
 * {@link #get} and {@link #getText} return an empty {@link Optional} on any
 * non-2xx response, IO error, timeout or malformed JSON body, logging the
 * reason at warn level. {@link #fetch} and {@link #fetchText} return the
 * same information plus a {@link FetchStatus}, distinguishing a deterministic
 * failure for the given URI (a 404 or other non-transient 4xx status) from a
 * transient transport error (a timeout, connection failure, a malformed body -
 * e.g. a captive portal or proxy answering with an HTML page instead of JSON -
 * or a 5xx/407/408/429 status), so callers can cache the former without
 * caching the latter: retrying the deterministic ones would only get the same
 * result again, while the transient ones may well succeed on the next attempt
 * (e.g. once back online). A well-formed JSON body that is missing the fields
 * a specific client needs is deterministic too, but that is for the client
 * (which knows what "required" means for its own response shape) to decide,
 * not this generic helper.
 * <p>
 * {@link #fetchAsync} is the asynchronous variant of {@link #fetch}, {@link #download}
 * streams a (possibly large) body into a file and throws an {@link IOException} with a
 * clear message on any failure.
 */
public class HttpJson {
    private static final Logger logger = LoggerFactory.getLogger(HttpJson.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    /** Timeout of a whole file download, which may be large. */
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(2);
    /** Maximum size of a file download. */
    static final long MAX_DOWNLOAD_BYTES = 100L * 1024 * 1024;

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
        return fetch(uri).value();
    }

    /**
     * Fetches the raw text body at the given URI (e.g. an SVG image response).
     * Empty on a non-2xx response, an IO error or a timeout.
     */
    public Optional<String> getText(URI uri) {
        return fetchText(uri).value();
    }

    /**
     * Fetches and parses the JSON body at the given URI, distinguishing "not
     * found" (HTTP 404) from a transport or parse error.
     */
    public FetchResult<JsonNode> fetch(URI uri) {
        FetchResult<String> text = fetchText(uri);
        return switch (text.status()) {
            case FOUND -> parse(uri, text.value().orElseThrow());
            case NOT_FOUND -> FetchResult.notFound();
            case ERROR -> FetchResult.error();
        };
    }

    /**
     * Fetches the raw text body at the given URI, distinguishing "not found"
     * (HTTP 404) from a transport error.
     */
    public FetchResult<String> fetchText(URI uri) {
        try {
            return textResult(uri, client.send(jsonRequest(uri), HttpResponse.BodyHandlers.ofString()));
        } catch (IOException e) {
            logger.warn("Error retrieving {}: {}", uri, e.getMessage());
            return FetchResult.error();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // a cancelled render interrupts its lookups, this is not an error
            logger.debug("Interrupted while retrieving {}", uri);
            return FetchResult.error();
        }
    }

    /**
     * Fetches and parses the JSON body at the given URI asynchronously, see {@link #fetch}.
     * The future always completes normally.
     */
    public CompletableFuture<FetchResult<JsonNode>> fetchAsync(URI uri) {
        return client.sendAsync(jsonRequest(uri), HttpResponse.BodyHandlers.ofString())
                .handle((response, error) -> {
                    if (error != null) {
                        Throwable cause = error instanceof CompletionException ? error.getCause() : error;
                        logger.warn("Error retrieving {}: {}", uri, describe(cause));
                        return FetchResult.<String>error();
                    }
                    return textResult(uri, response);
                })
                .thenApply(text -> switch (text.status()) {
                    case FOUND -> parse(uri, text.value().orElseThrow());
                    case NOT_FOUND -> FetchResult.<JsonNode>notFound();
                    case ERROR -> FetchResult.<JsonNode>error();
                });
    }

    /**
     * Downloads the body at the given URI into the given file, streaming it.
     * The download is limited to {@value #MAX_DOWNLOAD_BYTES} bytes.
     *
     * @throws IOException with a message naming the URI and the reason if the
     *     download failed: a non-2xx status, a body above the size limit, a transport
     *     error or a timeout. The file is removed on any failure.
     */
    public void download(URI uri, Path file) throws IOException {
        download(uri, file, MAX_DOWNLOAD_BYTES);
    }

    /**
     * Downloads the body at the given URI into the given file, failing if it is
     * larger than {@code maxBytes}; see {@link #download(URI, Path)}.
     */
    void download(URI uri, Path file, long maxBytes) throws IOException {
        try {
            downloadUnchecked(uri, file, maxBytes);
        } catch (IOException | RuntimeException e) {
            try {
                Files.deleteIfExists(file);
            } catch (IOException deleteError) {
                e.addSuppressed(deleteError);
            }
            throw e;
        }
    }

    private void downloadUnchecked(URI uri, Path file, long maxBytes) throws IOException {
        HttpRequest request =
                HttpRequest.newBuilder().uri(uri).timeout(TIMEOUT).GET().build();
        CompletableFuture<HttpResponse<Path>> future = client.sendAsync(
                request,
                info -> isSuccess(info.statusCode())
                        ? new LimitedBodySubscriber(
                                HttpResponse.BodySubscribers.ofFile(file),
                                info.headers().firstValueAsLong("Content-Length"),
                                maxBytes)
                        : HttpResponse.BodySubscribers.replacing(null));
        HttpResponse<Path> response;
        try {
            response = future.get(DOWNLOAD_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            throw new IOException("Could not download " + uri + ": " + describe(e.getCause()), e.getCause());
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new HttpTimeoutException(
                    "Could not download " + uri + ": timed out after " + DOWNLOAD_TIMEOUT.toSeconds() + " s");
        } catch (InterruptedException e) {
            future.cancel(true);
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while downloading " + uri);
        }
        if (!isSuccess(response.statusCode())) {
            throw new IOException("Could not download " + uri + ": HTTP status " + response.statusCode());
        }
    }

    /**
     * Passes the body on to the delegate as long as it is not larger than the maximum,
     * judged by the declared Content-Length and by counting the received bytes, and
     * cancels the download with an {@link IOException} otherwise.
     */
    private static final class LimitedBodySubscriber implements HttpResponse.BodySubscriber<Path> {
        private final HttpResponse.BodySubscriber<Path> delegate;
        private final OptionalLong declaredLength;
        private final long maxBytes;
        private Flow.Subscription subscription;
        private long received;
        private boolean failed;

        LimitedBodySubscriber(HttpResponse.BodySubscriber<Path> delegate, OptionalLong declaredLength, long maxBytes) {
            this.delegate = delegate;
            this.declaredLength = declaredLength;
            this.maxBytes = maxBytes;
        }

        @Override
        public CompletionStage<Path> getBody() {
            return delegate.getBody();
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            delegate.onSubscribe(subscription);
            if (declaredLength.isPresent() && declaredLength.getAsLong() > maxBytes) {
                fail();
            }
        }

        @Override
        public void onNext(List<ByteBuffer> buffers) {
            if (failed) {
                return;
            }
            for (ByteBuffer buffer : buffers) {
                received += buffer.remaining();
            }
            if (received > maxBytes) {
                fail();
                return;
            }
            delegate.onNext(buffers);
        }

        @Override
        public void onError(Throwable throwable) {
            if (!failed) {
                delegate.onError(throwable);
            }
        }

        @Override
        public void onComplete() {
            if (!failed) {
                delegate.onComplete();
            }
        }

        private void fail() {
            failed = true;
            subscription.cancel();
            delegate.onError(new IOException("the file is larger than the maximum of " + maxBytes + " bytes"));
        }
    }

    private static HttpRequest jsonRequest(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .timeout(TIMEOUT)
                .GET()
                .build();
    }

    private static FetchResult<String> textResult(URI uri, HttpResponse<String> response) {
        int status = response.statusCode();
        if (isDeterministicClientError(status)) {
            // a client error other than a transient rate limit/timeout is
            // deterministic for this URI: retrying it gets the same status again
            logger.debug("Deterministic client error {} for {}", status, uri);
            return FetchResult.notFound();
        }
        if (!isSuccess(status)) {
            logger.warn("Unexpected HTTP status {} for {}", status, uri);
            return FetchResult.error();
        }
        return FetchResult.found(response.body());
    }

    private static boolean isSuccess(int status) {
        return status >= 200 && status < 300;
    }

    /** The message of the given error, its type if it has none (e.g. an EOFException). */
    private static String describe(Throwable error) {
        return error.getMessage() != null
                ? error.getMessage()
                : error.getClass().getSimpleName();
    }

    /**
     * A 4xx status other than 407 (Proxy Authentication Required), 408 (Request Timeout)
     * and 429 (Too Many Requests) is a deterministic client error: the request itself is
     * what is wrong (a bad id, an unsupported ontology prefix, ...), so retrying it
     * against the same URI gets the same status again. 407, 408 and 429 are about the
     * server's (or an intermediate proxy's) momentary state rather than the request
     * itself, so they stay transient/uncached like a 5xx.
     */
    private static boolean isDeterministicClientError(int status) {
        return status >= 400 && status < 500 && status != 407 && status != 408 && status != 429;
    }

    private FetchResult<JsonNode> parse(URI uri, String body) {
        try {
            return FetchResult.found(mapper.readTree(body));
        } catch (IOException e) {
            // A 200 response whose body is not valid JSON is more likely a transient
            // network-path issue (e.g. a captive portal or an intercepting proxy
            // answering with an HTML page instead of the expected JSON) than a
            // permanent, deterministic one for this URI, so it stays uncached/ERROR,
            // unlike a well-formed JSON body missing required fields (a client's own
            // concern, see e.g. OlsClient.parseTerm).
            logger.warn("Error parsing JSON from {}: {}", uri, e.getMessage());
            return FetchResult.error();
        }
    }
}
