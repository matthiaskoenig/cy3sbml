package org.cy3sbml.gui;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SBaseHTMLThread} that do not need the OLS/UniProt/ChEBI web
 * services; see {@link SBaseHtmlThreadTest} (tagged {@code network}) for the full,
 * web-service-backed HTML generation tests.
 */
class SBaseHTMLThreadTest {

    /**
     * An empty object set means there is nothing new to show; it must not blank out
     * whatever the panel is currently displaying by posting {@code setText(null)}.
     */
    @Test
    void emptyObjectSetPostsNothingToThePanel() {
        InfoPanel panel = mock(InfoPanel.class);
        SBaseHTMLFactory htmlFactory = mock(SBaseHTMLFactory.class);

        new SBaseHTMLThread(Set.of(), panel, htmlFactory).run();

        verify(panel, never()).setText(org.mockito.ArgumentMatchers.any());
    }
}
