package org.cy3sbml.util;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.CancellationException;
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
 * Small JSON-over-HTTP helper for the REST web services (OLS, ChEBI, UniProt, BioModels).
 * <p>
 * {@link #fetch} and {@link #fetchText} return the body together with a {@link FetchStatus},
 * distinguishing a deterministic failure for the given URI (a 404 or other non-transient 4xx
 * status) from a transient transport error (a timeout, connection failure, a malformed body -
 * e.g. a captive portal or proxy answering with an HTML page instead of JSON - or a
 * 5xx/407/408/429 status), so callers can cache the former without caching the latter:
 * retrying the deterministic ones would only get the same result again, while the transient
 * ones may well succeed on the next attempt (e.g. once back online). A well-formed JSON body
 * that is missing the fields a specific client needs is deterministic too, but that is for the
 * client (which knows what "required" means for its own response shape) to decide, not this
 * generic helper. The reason of a failure is logged at warn level.
 * <p>
 * Every request has a timeout for the connection and the response headers, and a whole
 * response (including its body) must arrive within {@value #RESPONSE_TIMEOUT_SECONDS} s and
 * be at most {@value #MAX_RESPONSE_BYTES} bytes, otherwise it is a transient error; so a slow
 * or endless response neither blocks the caller nor exhausts memory.
 * <p>
 * {@link #fetchAsync} is the asynchronous variant of {@link #fetch}, {@link #download}
 * streams a (possibly large) body into a file and throws an {@link IOException} with a
 * clear message on any failure.
 */
public class HttpJson {
    private static final Logger logger = LoggerFactory.getLogger(HttpJson.class);
    /** Timeout of the connection and of the response headers. */
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    /** Timeout of a whole JSON or text response, including its body. */
    static final long RESPONSE_TIMEOUT_SECONDS = 30;
    /** Maximum size of a JSON or text response. */
    static final long MAX_RESPONSE_BYTES = 20L * 1024 * 1024;
    /** Timeout of a whole file download, which may be large. */
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofMinutes(2);
    /** Maximum size of a file download. */
    static final long MAX_DOWNLOAD_BYTES = 100L * 1024 * 1024;
    /**
     * Timeout of an upload, including the answer: a web service may process the upload
     * before it answers (sbml4humans reports a genome scale model in minutes).
     */
    static final Duration UPLOAD_TIMEOUT = Duration.ofMinutes(15);

    private final HttpClient client;
    private final ObjectMapper mapper;
    private final Duration responseTimeout;
    private final long maxResponseBytes;

    /**
     * Creates the helper for the given client, with the default response timeout and size
     * limit; see {@link #createDefault()} for the client the app uses.
     */
    public HttpJson(HttpClient client, ObjectMapper mapper) {
        this(client, mapper, Duration.ofSeconds(RESPONSE_TIMEOUT_SECONDS), MAX_RESPONSE_BYTES);
    }

    /** For tests: an injectable timeout and size limit of a JSON or text response. */
    HttpJson(HttpClient client, ObjectMapper mapper, Duration responseTimeout, long maxResponseBytes) {
        this.client = client;
        this.mapper = mapper;
        this.responseTimeout = responseTimeout;
        this.maxResponseBytes = maxResponseBytes;
    }

    /**
     * Creates a default instance that honors the Cytoscape system proxy settings
     * (set as system properties by {@code ConnectionProxy}). The client verifies TLS
     * certificates with the default trust store and follows redirects, except from
     * https to http.
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
     * Fetches and parses the JSON body at the given URI, distinguishing a deterministic
     * failure (e.g. HTTP 404) from a transient transport or parse error.
     */
    public FetchResult<JsonNode> fetch(URI uri) {
        return parse(uri, fetchText(uri));
    }

    /**
     * Fetches the raw text body at the given URI (e.g. an SVG image), distinguishing a
     * deterministic failure (e.g. HTTP 404) from a transient transport error.
     */
    public FetchResult<String> fetchText(URI uri) {
        CompletableFuture<HttpResponse<String>> response = send(uri);
        try {
            return textResult(uri, response.get());
        } catch (ExecutionException e) {
            return transportError(uri, e.getCause());
        } catch (CancellationException e) {
            // timed out, logged when the response timeout cancelled it
            return FetchResult.error();
        } catch (InterruptedException e) {
            response.cancel(true);
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
        return send(uri)
                .handle((response, error) -> error == null
                        ? textResult(uri, response)
                        : HttpJson.<String>transportError(
                                uri, error instanceof CompletionException ? error.getCause() : error))
                .thenApply(text -> parse(uri, text));
    }

    /**
     * Sends a GET request for JSON, reading at most {@link #maxResponseBytes} of the body.
     * The response is cancelled if it has not arrived completely within {@link #responseTimeout}.
     */
    private CompletableFuture<HttpResponse<String>> send(URI uri) {
        return send(jsonRequest(uri), responseTimeout);
    }

    /**
     * Sends the request, reading at most {@link #maxResponseBytes} of the body. The response
     * is cancelled if it has not arrived completely within the timeout.
     */
    private CompletableFuture<HttpResponse<String>> send(HttpRequest request, Duration timeout) {
        CompletableFuture<HttpResponse<String>> response = client.sendAsync(
                request,
                info -> new LimitedBodySubscriber<>(
                        HttpResponse.BodyHandlers.ofString().apply(info),
                        info.headers().firstValueAsLong("Content-Length"),
                        maxResponseBytes));
        CompletableFuture.delayedExecutor(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .execute(() -> {
                    if (response.cancel(true)) {
                        logger.warn("Timed out after {} s requesting {}", timeout.toSeconds(), request.uri());
                    }
                });
        return response;
    }

    /**
     * Posts the content as a file in the field of a multipart/form-data request and parses
     * the JSON answer, e.g. an upload of a model to a web service. The request and its
     * answer must complete within {@link #UPLOAD_TIMEOUT}.
     *
     * @param field    the name of the form field
     * @param fileName the file name of the content
     * @throws IOException with a message naming the URI and the reason if the upload
     *     failed: a non-2xx status, an answer which is no JSON, a transport error or a timeout
     */
    public JsonNode postMultipart(URI uri, String field, String fileName, byte[] content) throws IOException {
        checkHeaderValue(field);
        checkHeaderValue(fileName);
        String boundary = "cy3sbml-" + UUID.randomUUID();
        byte[] head = ("--" + boundary + "\r\n"
                        + "Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + fileName + "\"\r\n"
                        + "Content-Type: application/octet-stream\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8);
        byte[] tail = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .timeout(UPLOAD_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(List.of(head, content, tail)))
                .build();
        CompletableFuture<HttpResponse<String>> response = send(request, UPLOAD_TIMEOUT);
        HttpResponse<String> answer;
        try {
            answer = response.get();
        } catch (ExecutionException e) {
            throw new IOException("Could not upload to " + uri + ": " + describe(e.getCause()), e.getCause());
        } catch (CancellationException e) {
            throw new IOException(
                    "Could not upload to " + uri + ": timed out after " + UPLOAD_TIMEOUT.toSeconds() + " s", e);
        } catch (InterruptedException e) {
            response.cancel(true);
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while uploading to " + uri);
        }
        if (!isSuccess(answer.statusCode())) {
            throw new IOException("Could not upload to " + uri + ": HTTP status " + answer.statusCode());
        }
        try {
            JsonNode node = answer.body() == null ? null : mapper.readTree(answer.body());
            if (node == null || node.isMissingNode()) {
                throw new IOException("Could not upload to " + uri + ": the answer is empty");
            }
            return node;
        } catch (JsonProcessingException e) {
            throw new IOException("Could not upload to " + uri + ": the answer is no JSON", e);
        }
    }

    /** A value of a header parameter is written in quotes, so it cannot hold one or a line break. */
    private static void checkHeaderValue(String value) {
        if (value.contains("\"") || value.contains("\r") || value.contains("\n")) {
            throw new IllegalArgumentException("Invalid form field or file name: " + value);
        }
    }

    private static <T> FetchResult<T> transportError(URI uri, Throwable error) {
        if (!(error instanceof CancellationException)) {
            // a cancellation is a timeout, logged when the response timeout cancelled it
            logger.warn("Error retrieving {}: {}", uri, describe(error));
        }
        return FetchResult.error();
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
                        ? new LimitedBodySubscriber<>(
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
     * cancels the response with an {@link IOException} otherwise.
     */
    private static final class LimitedBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {
        private final HttpResponse.BodySubscriber<T> delegate;
        private final OptionalLong declaredLength;
        private final long maxBytes;
        private Flow.Subscription subscription;
        private long received;
        private boolean failed;

        LimitedBodySubscriber(HttpResponse.BodySubscriber<T> delegate, OptionalLong declaredLength, long maxBytes) {
            this.delegate = delegate;
            this.declaredLength = declaredLength;
            this.maxBytes = maxBytes;
        }

        @Override
        public CompletionStage<T> getBody() {
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
            delegate.onError(new IOException("the response is larger than the maximum of " + maxBytes + " bytes"));
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
        String body = response.body();
        if (body == null) {
            // a 2xx response without a body carries no value; like an empty JSON body
            // (see parse) this is a transient error, never a found null
            logger.warn("No response body from {}", uri);
            return FetchResult.error();
        }
        return FetchResult.found(body);
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

    private FetchResult<JsonNode> parse(URI uri, FetchResult<String> text) {
        return switch (text.status()) {
            case FOUND -> parse(uri, text.value().orElseThrow());
            case NOT_FOUND -> FetchResult.notFound();
            case ERROR -> FetchResult.error();
        };
    }

    private FetchResult<JsonNode> parse(URI uri, String body) {
        try {
            JsonNode node = mapper.readTree(body);
            if (node == null || node.isMissingNode()) {
                // A 2xx response with an empty body carries no JSON document. Like a
                // malformed body (see below) this is treated as transient/ERROR.
                logger.warn("Empty JSON body from {}", uri);
                return FetchResult.error();
            }
            return FetchResult.found(node);
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
