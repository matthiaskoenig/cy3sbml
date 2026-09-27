package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

/**
 * Tests for GUIUtil.
 */
class GUIUtilTest {

    /**
     * The WebViewPanel has no HTML set yet (getHtml() returns null) when the browser link
     * is clicked before any SBase was ever selected. openSBaseHTMLInBrowser must not fail
     * on that null, it should log and return.
     */
    @Test
    void openSBaseHTMLInBrowserWithNoHtmlYetDoesNotThrow() {
        assertDoesNotThrow(() -> GUIUtil.openSBaseHTMLInBrowser(null));
    }
}
