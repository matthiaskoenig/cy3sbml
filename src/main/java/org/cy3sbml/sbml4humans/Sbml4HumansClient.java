package org.cy3sbml.sbml4humans;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.cy3sbml.util.HttpJson;

/**
 * The upload of a model to sbml4humans: {@code POST <api>upload} keeps the model (an SBML file
 * or COMBINE archive) for 24 hours and answers its id, whose report is at
 * {@code <url>report?upload=<id>}.
 * <p>
 * The address of the reports and of the api come from the cy3sbml properties
 * {@value #PROPERTY_URL} and {@value #PROPERTY_API}, so a local sbml4humans can be used, whose
 * frontend and api run on different ports.
 */
public final class Sbml4HumansClient {
    /** The property of the address of the reports. */
    public static final String PROPERTY_URL = "cy3sbml.sbml4humans.url";
    /** The property of the address of the api, by default {@code <url>api/}. */
    public static final String PROPERTY_API = "cy3sbml.sbml4humans.api";
    /** The address of sbml4humans. */
    public static final String DEFAULT_URL = "https://sbml4humans.de/";
    /** The largest upload sbml4humans takes. */
    public static final long MAX_UPLOAD_BYTES = 100L * 1024 * 1024;

    private final HttpJson http;
    private final URI url;
    private final URI api;

    /**
     * @param url the address of the reports, ending with {@code /}
     * @param api the address of the api, ending with {@code /}
     */
    public Sbml4HumansClient(HttpJson http, URI url, URI api) {
        this.http = http;
        this.url = url;
        this.api = api;
    }

    /** The client of the addresses of the cy3sbml properties, with the defaults for missing ones. */
    public static Sbml4HumansClient fromProperties(Properties properties) {
        URI url = URI.create(directory(properties.getProperty(PROPERTY_URL), DEFAULT_URL));
        String api = properties.getProperty(PROPERTY_API);
        return new Sbml4HumansClient(
                HttpJson.createDefault(),
                url,
                api == null || api.isBlank() ? url.resolve("api/") : URI.create(directory(api, null)));
    }

    /** The address with a trailing {@code /}, the default for a blank one. */
    private static String directory(String value, String fallback) {
        String address = value == null || value.isBlank() ? fallback : value.strip();
        return address.endsWith("/") ? address : address + "/";
    }

    /** The address of the reports. */
    public URI url() {
        return url;
    }

    /** The address of the api. */
    public URI api() {
        return api;
    }

    /**
     * Uploads the archive and returns the address of its report, opened at the entry and the
     * model.
     *
     * @param entry the location of the SBML entry in the archive, e.g. {@code ./model.xml}
     * @param model the id of the model to open, null for the main model
     * @throws IOException if the archive is too large, the upload failed or sbml4humans could
     *     not read the model, with a message for the user
     */
    public URI upload(byte[] archive, String entry, String model) throws IOException {
        if (archive.length > MAX_UPLOAD_BYTES) {
            throw new IOException("The model is larger than 100 MB, the limit of sbml4humans.");
        }
        JsonNode answer = http.postMultipart(api.resolve("upload"), "source", "model.omex", archive);
        // the error contract of sbml4humans: status 200 with the errors in the body
        JsonNode errors = answer.path("errors");
        if (errors.isArray() && !errors.isEmpty()) {
            throw new IOException(
                    "sbml4humans could not read the model: " + errors.get(0).asText());
        }
        JsonNode id = answer.path("id");
        if (!id.isTextual() || id.asText().isBlank()) {
            throw new IOException("sbml4humans gave no upload id");
        }
        String query = "report?upload=" + encode(id.asText()) + "&entry=" + encode(entry)
                + (model != null ? "&model=" + encode(model) : "");
        return url.resolve(query);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
