package org.cy3sbml.util;

import java.io.*;

/**
 * Helper functions for input and output.
 */
public class IOUtil {
    private static final int BUFFER_SIZE = 16384;

    /** Read String from InputStream. */
    public static String readString(InputStream source) throws IOException {
        StringWriter writer = new StringWriter();
        BufferedReader reader = new BufferedReader(new InputStreamReader(source));
        try {
            char[] buffer = new char[BUFFER_SIZE];
            int charactersRead = reader.read(buffer, 0, buffer.length);
            while (charactersRead != -1) {
                writer.write(buffer, 0, charactersRead);
                charactersRead = reader.read(buffer, 0, buffer.length);
            }
        } finally {
            reader.close();
        }
        return writer.toString();
    }

}
