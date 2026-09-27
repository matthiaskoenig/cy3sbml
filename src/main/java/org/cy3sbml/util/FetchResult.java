package org.cy3sbml.util;

import java.util.Optional;

/**
 * The result of a fetch: its {@link FetchStatus} and the value, if found.
 * <p>
 * The value is present if, and only if, the status is {@link FetchStatus#FOUND}. A fetch
 * that got no value (e.g. an empty or missing response body) is an error, not a found
 * {@code null}: callers map it to {@link #error()} before creating a result.
 */
public record FetchResult<T>(FetchStatus status, Optional<T> value) {

    /**
     * @throws IllegalArgumentException if the status is missing, the value is a
     *     {@code null} Optional, or the presence of the value does not match the status
     */
    public FetchResult {
        if (status == null || value == null) {
            throw new IllegalArgumentException("FetchResult status and value must not be null");
        }
        if ((status == FetchStatus.FOUND) != value.isPresent()) {
            throw new IllegalArgumentException("FetchResult with status " + status + " must "
                    + (value.isPresent() ? "not " : "") + "have a value");
        }
    }

    /**
     * A found value.
     *
     * @throws IllegalArgumentException if {@code value} is null: a fetch without a value
     *     is an {@link #error()}
     */
    public static <T> FetchResult<T> found(T value) {
        if (value == null) {
            throw new IllegalArgumentException("A found FetchResult needs a value, not null; use error() instead");
        }
        return new FetchResult<>(FetchStatus.FOUND, Optional.of(value));
    }

    public static <T> FetchResult<T> notFound() {
        return new FetchResult<>(FetchStatus.NOT_FOUND, Optional.empty());
    }

    public static <T> FetchResult<T> error() {
        return new FetchResult<>(FetchStatus.ERROR, Optional.empty());
    }
}
