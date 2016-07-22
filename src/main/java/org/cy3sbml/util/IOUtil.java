package org.cy3sbml.util;

import java.io.*;

/**
 * Helper functions for input and output.
 */
public class IOUtil {
    private static final int BUFFER_SIZE = 16384;

    /** Read resource to InputStream */
    public static InputStream readResource(String resource){
        return IOUtil.class.getResourceAsStream(resource);
    }

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

    /** Copy InputStream. */
    public static InputStream copyInputStream(InputStream is) throws IOException {
        ByteArrayOutputStream copy = new ByteArrayOutputStream();
        int chunk = 0;
        byte[] data = new byte[1024*1024];
        while((-1 != (chunk = is.read(data)))) {
            copy.write(data, 0, chunk);
        }
        is.close();
        return new ByteArrayInputStream( copy.toByteArray() );
    }

}
