package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import org.cy3sbml.SBMLManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    /** A temporary file, e.g. in a Windows user directory with a space, is opened by a valid URL. */
    @Test
    void fileUrlIsAValidUri(@TempDir Path directory) {
        File file = directory.resolve("user name #1").resolve("model.html").toFile();

        String url = GUIUtil.fileUrl(file);

        assertEquals(file.getAbsoluteFile(), new File(URI.create(url)));
    }

    /** The link to open the SBML of the current network, clicked without a current network. */
    @Test
    void openCurrentSBMLInBrowserWithoutDocumentDoesNotThrow() {
        assertDoesNotThrow(() -> GUIUtil.openCurrentSBMLInBrowser(mock(SBMLManager.class)));
    }
}
