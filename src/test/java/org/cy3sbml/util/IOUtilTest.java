package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Test IOUtil.
 */
public class IOUtilTest {

    @TempDir
    Path tempDir; // JUnit 5 temporary directory

    @Test
    public void copyInputStream() throws Exception {
        String text1 = "test string";
        InputStream s1 = IOUtil.string2InputStream(text1);
        InputStream s2 = IOUtil.copyInputStream(s1);
        String text2 = new String(s2.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(text1, text2);
    }

    @Test
    public void createUniqueFile() throws Exception {
        File directory = tempDir.toFile(); // Use the temp directory
        String fileName = "test";
        String extension = ".xml";
        File f1 = IOUtil.createUniqueFile(directory, fileName, extension);
        assertNotNull(f1);
    }
}
