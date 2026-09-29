package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Every local image, stylesheet and script a bundled GUI page references exists, so the
 * pages render without broken images in the info panel. Pages and resources are resolved
 * on the classpath, so the packaged resources are checked.
 */
class GuiPageResourcesTest {
    private static final Pattern REFERENCE = Pattern.compile("(?:src|href)=\"([^\"]+)\"");
    private static final Pattern REMOTE_RESOURCE =
            Pattern.compile("<(?:link|script)\\b[^>]*(?:src|href)=\"(?:https?:)?//[^\"]*\"[^>]*>");

    @ParameterizedTest
    @ValueSource(strings = {"help.html", "examples.html", "icons.html", "linktemplate.html"})
    void localReferencesExist(String page) throws Exception {
        URL pageUrl = GuiPageResourcesTest.class.getResource("/gui/" + page);
        assertNotNull(pageUrl, page);
        URI pageUri = pageUrl.toURI();
        String html = Files.readString(Path.of(pageUri));
        // commented out markup is not rendered
        html = html.replaceAll("(?s)<!--.*?-->", "");
        List<String> missing = new ArrayList<>();
        Matcher matcher = REFERENCE.matcher(html);
        while (matcher.find()) {
            String reference = matcher.group(1);
            if (isLocalFile(reference) && !Files.exists(Path.of(pageUri.resolve(reference)))) {
                missing.add(reference);
            }
        }
        assertEquals(List.of(), missing);
    }

    /**
     * The pages of the info panel render without network access: they load no stylesheets or
     * scripts from the web, e.g. no icon fonts from a CDN (#440).
     */
    @ParameterizedTest
    @ValueSource(strings = {"help.html", "examples.html", "linktemplate.html"})
    void noRemoteStylesheetsOrScripts(String page) throws Exception {
        URL pageUrl = GuiPageResourcesTest.class.getResource("/gui/" + page);
        assertNotNull(pageUrl, page);
        String html = Files.readString(Path.of(pageUrl.toURI())).replaceAll("(?s)<!--.*?-->", "");
        List<String> remote = new ArrayList<>();
        Matcher matcher = REMOTE_RESOURCE.matcher(html);
        while (matcher.find()) {
            remote.add(matcher.group());
        }
        assertEquals(List.of(), remote);
    }

    private static boolean isLocalFile(String reference) {
        // template placeholders such as %s or {URL} are filled in at render time
        return !reference.contains(":")
                && !reference.startsWith("#")
                && !reference.contains("%s")
                && !reference.contains("{");
    }

    /** The cofactor splitting is hidden until it is complete (#405). */
    @Test
    void helpDoesNotShowTheCofactorSplitting() throws Exception {
        URL pageUrl = GuiPageResourcesTest.class.getResource("/gui/help.html");
        String html = Files.readString(Path.of(pageUrl.toURI())).replaceAll("(?s)<!--.*?-->", "");

        assertFalse(html.toLowerCase(Locale.ROOT).contains("cofactor"), html);
    }
}
