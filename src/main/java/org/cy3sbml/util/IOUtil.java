package org.cy3sbml.util;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper functions for input and output.
 */
public class IOUtil {
    private static final Logger logger = LoggerFactory.getLogger(IOUtil.class);
    private static final int BUFFER_SIZE = 16384;

    /**
     * Read resource to InputStream
     */
    public static InputStream readResource(String resource) {
        return IOUtil.class.getResourceAsStream(resource);
    }

    /**
     * Read String from InputStream.
     */
    public static String inputStream2String(InputStream source) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(source));
             StringWriter writer = new StringWriter()) {
            char[] buffer = new char[BUFFER_SIZE];
            int charactersRead;
            while ((charactersRead = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, charactersRead);
            }
            return writer.toString();
        }
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

    /**
     * Get URL as file.
     * Use to download files
     *
     * @param file
     */
    public static void saveURLasFile(URL url, File file) {
        HttpURLConnection sourceConnection = null;
        try {
            sourceConnection = (HttpURLConnection) url.openConnection();
            sourceConnection.setFollowRedirects(true);
            sourceConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");

            String encoding = sourceConnection.getContentEncoding();
            InputStream rawInputStream = sourceConnection.getInputStream();

            InputStream inputStream;
            if ("gzip".equalsIgnoreCase(encoding)) {
                inputStream = new GZIPInputStream(rawInputStream);
            } else if ("deflate".equalsIgnoreCase(encoding)) {
                inputStream = new InflaterInputStream(rawInputStream, new Inflater(true));
            } else {
                inputStream = rawInputStream;
            }

            // Copy decompressed input stream directly to file
            try (InputStream in = inputStream;
                 FileOutputStream fos = new FileOutputStream(file)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }

        } catch (IOException e) {
            logger.error("URL could not be saved.", e);
            e.printStackTrace();
        } finally {
            if (sourceConnection != null) {
                sourceConnection.disconnect();
            }
        }
    }


    /**
     * Returns the Last-Modified Http Response Header field.
     * @param url
     * @return
     */


}
