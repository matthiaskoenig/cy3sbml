package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class FetchResultTest {

    @Test
    void foundHoldsTheValue() {
        FetchResult<String> result = FetchResult.found("value");
        assertEquals(FetchStatus.FOUND, result.status());
        assertEquals(Optional.of("value"), result.value());
    }

    @Test
    void foundRejectsNull() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> FetchResult.found(null));
        assertTrue(e.getMessage().contains("null"), e.getMessage());
    }

    @Test
    void theStatusMustMatchThePresenceOfTheValue() {
        assertThrows(IllegalArgumentException.class, () -> new FetchResult<>(FetchStatus.FOUND, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new FetchResult<>(FetchStatus.NOT_FOUND, Optional.of("x")));
        assertThrows(IllegalArgumentException.class, () -> new FetchResult<>(FetchStatus.ERROR, Optional.of("x")));
        assertThrows(IllegalArgumentException.class, () -> new FetchResult<>(null, Optional.empty()));
        assertThrows(IllegalArgumentException.class, () -> new FetchResult<>(FetchStatus.ERROR, null));
    }
}
