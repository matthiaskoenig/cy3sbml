package org.cy3sbml.styles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The bundled styles {@code /styles/cy3sbml*.xml} are the styles {@link StyleFactory}
 * generates from their templates and {@link StyleInfo}, so they stay consistent.
 */
class StyleFactoryTest {

    @TempDir
    Path tempDir;

    @Test
    void cy3sbmlStyleIsGenerated() throws Exception {
        assertGenerated(new StyleInfo_cy3sbml(), "/styles/cy3sbml.xml");
    }

    @Test
    void cy3sbmlDarkStyleIsGenerated() throws Exception {
        assertGenerated(new StyleInfo_cy3sbmlDark(), "/styles/cy3sbml-dark.xml");
    }

    private void assertGenerated(StyleInfo info, String resource) throws Exception {
        File file = tempDir.resolve("style.xml").toFile();
        StyleFactory.createStyle(info, file);

        URL bundled = StyleFactoryTest.class.getResource(resource);
        assertNotNull(bundled, resource);
        assertEquals(
                Files.readString(Path.of(bundled.toURI())).strip(),
                Files.readString(file.toPath()).strip(),
                "regenerate " + resource + " with StyleFactory.createStyle");
    }
}
