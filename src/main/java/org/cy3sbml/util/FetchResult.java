package org.cy3sbml.util;

import java.util.Optional;

/** The result of a fetch: its {@link FetchStatus} and the value, if found. */
public record FetchResult<T>(FetchStatus status, Optional<T> value) {

    public static <T> FetchResult<T> found(T value) {
        return new FetchResult<>(FetchStatus.FOUND, Optional.of(value));
    }

    public static <T> FetchResult<T> notFound() {
        return new FetchResult<>(FetchStatus.NOT_FOUND, Optional.empty());
    }

    public static <T> FetchResult<T> error() {
        return new FetchResult<>(FetchStatus.ERROR, Optional.empty());
    }
}
