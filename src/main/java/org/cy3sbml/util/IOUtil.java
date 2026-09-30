package org.cy3sbml.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Helper functions for input and output.
 */
public class IOUtil {

    /**
     * The classpath resource of the app.
     *
     * @param resource the absolute resource path
     * @return the stream, null if there is no such resource
     */
    public static InputStream readResource(String resource) {
        return IOUtil.class.getResourceAsStream(resource);
    }

    /**
     * The UTF-8 bytes of the string as stream.
     *
     * @param s the string
     * @return the stream
     */
    public static InputStream string2InputStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Reads the stream into memory and closes it, e.g. to read a stream twice.
     *
     * @param is the stream, closed afterwards
     * @return a stream of the bytes read
     * @throws IOException if the stream cannot be read
     */
    public static InputStream copyInputStream(InputStream is) throws IOException {
        try (is) {
            return new ByteArrayInputStream(is.readAllBytes());
        }
    }

    /**
     * A file in the directory with the name and extension that does not exist yet: the name
     * with a suffix {@code _0}, {@code _1}, ... if the file exists.
     *
     * @param directory the directory
     * @param fileName the file name without extension
     * @param extension the file extension, with the dot
     * @return the file, not created
     */
    public static File createUniqueFile(File directory, String fileName, String extension) {
        File target = new File(directory, fileName + extension);
        int suffix = 0;
        while (target.exists()) {
            target = new File(directory, fileName + "_" + suffix + extension);
            suffix++;
        }
        return target;
    }
}
