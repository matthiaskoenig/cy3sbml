package org.cy3sbml.archive;

import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Archive Filter class.
 * Which files and uris are accepted as archive files.
 */
public class ArchiveFileFilter extends BasicCyFileFilter {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveFileFilter.class);
    public static byte[] MAGIC = { 'P', 'K', 0x3, 0x4 };

    /**
     * Constructor.
     */
    public ArchiveFileFilter(StreamUtil streamUtil) {
        super(
                new String[]{"ro", "omex", "sedx", "sbex", "cmex", "sbox", "neux", "phex", "zip", ""},
                new String[]{"application/zip", "application/octet-stream", "application/vnd.wf4ever.robundle+zipPK"},
                "Archive network reader (cy3robundle)",
                DataCategory.NETWORK,
                streamUtil
        );
        logger.info("new " + getClass() + "()");
    }

    /**
     * Indicates which URI the FileFilter accepts.
     *
     * @param uri URI to check
     * @param category
     * @return
     */
    @Override
    public boolean accepts(URI uri, DataCategory category) {
        try {
            logger.info("public boolean accepts(URI uri, DataCategory category)");
            logger.info(uri.toURL().toString());

            // Not working because streamUtil extracts the zipped content
            // InputStream inputStream = streamUtil.getInputStream(uri.toURL());
            InputStream inputStream = streamUtil.getURLConnection(uri.toURL()).getInputStream();

            return accepts(inputStream, category);
        } catch (IOException e) {
            logger.error("Error while creating stream from uri", e);
            return false;
        }
    }

    /**
     * Indicates which streams the FileFilter accepts.
     *
     * @param stream
     * @param category
     * @return
     */
    @Override
    public boolean accepts(InputStream stream, DataCategory category) {
        logger.info("public boolean accepts(InputStream stream, DataCategory category)");
        if (!category.equals(DataCategory.NETWORK)) {
            return false;
        }
        return isZipStream(stream);
    }

    /**
     * Method to test if a input stream is a zip archive.
     *
     * This method is based on the MAGIC bytes of the zip archive,
     * but not very reliable.
     *
     * The better way for testing is
     *      boolean isZipped = new ZipInputStream(stream).getNextEntry() != null;
     *
     * But does not work in the Cytoscape reader context because
     * only first kbs of stream are send for validation
     * from GenericReaderManager.getReader()
     *
     * Because we don't know who will provide the file filter or
     * what they might do with the InputStream, we provide a copy
     * of the first 2KB rather than the stream itself.
     *
     *  if (cff.accepts(CopyInputStream.copyKBytes(stream, 1), category)) {
     *      // logger.debug("successfully matched READER " + factory);
     *      return (R) factory.createTaskIterator(stream, inputName).next();
     *  }
     *
     * @param in    the input stream to test.
     * @return
     */
    public static boolean isZipStream(InputStream in) {
        // boolean isZipped = new ZipInputStream(stream).getNextEntry() != null;

        /*
        in.read():

        Reads the next byte of data from the input stream. The value byte is
        returned as an <code>int</code> in the range <code>0</code> to
        <code>255</code>. If no byte is available because the end of the stream
        has been reached, the value <code>-1</code> is returned. This method
        blocks until input data is available, the end of the stream is detected,
        or an exception is thrown.
        */

        if (!in.markSupported()) {
            in = new BufferedInputStream(in);
        }
        boolean isZip = true;
        try {
            in.mark(MAGIC.length);
            for (int i = 0; i < MAGIC.length; i++) {
                byte b = (byte) in.read();
                logger.debug("byte[" + i + "]: '" + b +"'");
                if (MAGIC[i] != b) {
                    isZip = false;
                    break;
                }
            }
            in.reset();
        } catch (IOException e) {
            isZip = false;
        }
        logger.debug("isZipStream: " + isZip);
        return isZip;

    }

}
