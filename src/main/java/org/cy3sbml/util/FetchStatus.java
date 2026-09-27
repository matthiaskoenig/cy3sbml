package org.cy3sbml.util;

/**
 * Outcome of a single {@link HttpJson} fetch, distinguishing "not found" (e.g.
 * an HTTP 404, or an equivalent empty-result response) from a transport or
 * parse error (e.g. a timeout, connection failure or malformed body).
 * <p>
 * The distinction matters for caching: a "not found" result is safe to cache
 * for a short time, while an error should not be cached, so a transient
 * outage (e.g. while offline) recovers on the next lookup.
 */
public enum FetchStatus {
    FOUND,
    NOT_FOUND,
    ERROR
}
