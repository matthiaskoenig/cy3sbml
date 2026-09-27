package org.cy3sbml.util;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Helper functions for input and output.
 */
public class IOUtil {

    /**
     * Read resource to InputStream
     */
    public static InputStream readResource(String resource) {
        return IOUtil.class.getResourceAsStream(resource);
    }

    /**
     * Create InputStream from String.
     */
    public static InputStream string2InputStream(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Copy InputStream.
     */
    public static InputStream copyInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream copy = new ByteArrayOutputStream();
        int chunk = 0;
        byte[] data = new byte[1024 * 1024];
        while ((-1 != (chunk = is.read(data)))) {
            copy.write(data, 0, chunk);
        }
        is.close();
        return new ByteArrayInputStream(copy.toByteArray());
    }

    /**
     * Creates a unique file with a given filename and a given extension in a given directory.
     * If the file already exists, suffixes will be added.
     *
     * @param fileName  - Filename of the Temporary file
     * @param extension - File extension of the temporary file (with dot).
     * @return The unique File Object.
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
