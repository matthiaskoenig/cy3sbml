package org.cy3sbml.styles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

public class StyleFactoryTest {

    @TempDir
    Path tempDir;  // JUnit 5's replacement for TemporaryFolder

    @Test
    public void createStyle01() throws Exception {
        File tempFile = tempDir.resolve("test.xml").toFile();
        StyleInfo_cy3sbml info = new StyleInfo_cy3sbml();
        StyleFactory.createStyle(info, tempFile);
    }

    @Test
    public void createStyle02() throws Exception {
        File tempFile = tempDir.resolve("test.xml").toFile();
        StyleInfo_cy3sbmlDark info = new StyleInfo_cy3sbmlDark();
        StyleFactory.createStyle(info, tempFile);
    }
}