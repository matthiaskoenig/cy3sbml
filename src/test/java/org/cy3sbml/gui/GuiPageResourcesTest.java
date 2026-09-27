package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Every local image, stylesheet and script a bundled GUI page references exists, so the
 * pages render without broken images in the info panel. Pages and resources are resolved
 * on the classpath, so the packaged resources are checked.
 */
class GuiPageResourcesTest {
    private static final Pattern REFERENCE = Pattern.compile("(?:src|href)=\"([^\"]+)\"");

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

    private static boolean isLocalFile(String reference) {
        // template placeholders such as %s or {URL} are filled in at render time
        return !reference.contains(":")
                && !reference.startsWith("#")
                && !reference.contains("%s")
                && !reference.contains("{");
    }
}
