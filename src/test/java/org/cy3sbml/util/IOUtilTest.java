package org.cy3sbml.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import static org.junit.Assert.*;

/**
 * Test IOUtil.
 */
public class IOUtilTest {

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Test
    public void copyInputStream() throws Exception {
        String text1 = "test string";
        InputStream s1 = IOUtil.string2InputStream(text1);
        InputStream s2 = IOUtil.copyInputStream(s1);
        String text2 = IOUtil.inputStream2String(s2);
        assertTrue(text2.equals(text1));
    }

    @Test
    public void createUniqueFile() throws Exception {
        File directory = testFolder.newFolder();
        String fileName = "test";
        String extension = ".xml";
        File f1 = IOUtil.createUniqueFile(directory, fileName, extension);
        assertNotNull(f1);
    }

    @Test
    public void saveURLasFile() throws Exception {
        File f = testFolder.newFile();
        URL url = new URL("http://www.google.com");
        IOUtil.saveURLasFile(url, f);
        assertTrue(f.exists());
    }

}