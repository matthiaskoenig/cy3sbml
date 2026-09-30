package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Tests for OpenBrowser: URLs that are not opened, without starting a browser.
 */
class OpenBrowserTest {

    /** A link of a model with an invalid URL must not throw on the event dispatch thread. */
    @Test
    void invalidUrlIsNotOpened() {
        assertFalse(OpenBrowser.openURL("http://example.org/a b"));
    }

    /** A relative URL could be taken as an option of a browser command. */
    @Test
    void relativeUrlIsNotOpened() {
        assertFalse(OpenBrowser.openURL("--new-instance"));
    }
}
