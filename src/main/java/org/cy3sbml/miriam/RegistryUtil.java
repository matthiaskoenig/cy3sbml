package org.cy3sbml.miriam;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.identifiers.registry.RegistryDatabase;
import org.cy3sbml.util.IOUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Tools for working with Miriam registry.
 * Here the MIRIAM xml file is loaded or updated.
 * http://www.ebi.ac.uk/miriam/main/export/
 */
public class RegistryUtil {
    private static final Logger logger = LoggerFactory.getLogger(RegistryUtil.class);
    public static final String URL_MIRIAM_XML = "http://www.ebi.ac.uk/miriam/main/export/xml";
    public static final String FILENAME_MIRIAM = "IdentifiersOrg-Registry.xml";

    /**
     * Load the registry from the resources.
     * @param file MIRIAM xml file
     */
    public static void loadRegistry(File file) {
        if (file != null && file.exists()){
            try {
                RegistryDatabase.loadFromFile(file);
                logger.info("Load MIRIAM: " + file.getAbsolutePath());
                return;
            } catch (FileNotFoundException e) {
                logger.error("Problems loading the downloaded MIRIAM XML.", e);
                e.printStackTrace();

            }
        }
        // something went wrong, using fallback, i.e. the packed MIRIAM xml
        loadRegistry();
    }

    /**
     * Load the registry from the resources.
     */
    public static void loadRegistry() {
        InputStream miriamStream = IOUtil.readResource("/miriam/" + FILENAME_MIRIAM);
        RegistryDatabase.loadFromInputStream(miriamStream);
    }

    /**
     * Only update MIRIAM if newer version is available.
     * Check last modified and use for update.
     */
    public static void updateMiriamXMLWithNewer(File file){

        // Get data-version of current file
        Date fileDate = getDataVersionDate(file);
        System.out.println("data-version file: " + fileDate);

        // Get data-version of online resource
        Date miriamDate = getLatestDataVersionDate();
        System.out.println("data-version miriam: " + miriamDate);

        // online version is newer
        if (miriamDate==null || miriamDate.compareTo(fileDate)>0){
            logger.info("New MIRIAM available: " + miriamDate);
            updateMiriamXML(file);
        }else {
            logger.info(String.format("MIRIAM is current version (%s)", fileDate));
        }
    }

    /**
     * Updates the MIRIAM registry file.
     * Downloads xml from MIRIAM and saves in file.
     *
     * @param file MIRIAM xml file
     */
    public static void updateMiriamXML(File file){
        try {
            URL miriamURL = new URL(URL_MIRIAM_XML);
            IOUtil.saveURLasFile(miriamURL, file);
            logger.info("Updated MIRIAM: " + file.getAbsolutePath());
        } catch (MalformedURLException e) {
            logger.error("MalformedURLException", e);
            e.printStackTrace();
        }
    }

    /**
     * Get date of latest data-version from MIRIAM online.
     *
     * @return
     */
    private static Date getLatestDataVersionDate(){
        try {
            // Get data-version of online file
            URL miriamURL = new URL(URL_MIRIAM_XML);
            String lastModified = IOUtil.getLastModified(miriamURL);
            // System.out.println("lastModified: " + lastModified);

            // 2016/08/03 15:21:34
            // SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            // Wed, 17 Aug 2016 14:57:52 GMT
            SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
            try {
                Date date = format.parse(lastModified);
                return date;
            } catch (ParseException e){
                logger.error("Last-Modified could not be parsed", e);
                e.printStackTrace();
                return null;
            }
        } catch (MalformedURLException e) {
            logger.error("MalformedURLException", e);
            return null;
        }
    }

    /**
     * Retrieve the MIRIAM data-version from the given file.
     */
    private static Date getDataVersionDate(File file){
        try {
            InputStream inputStream = new FileInputStream(file);
            BufferedInputStream stream = new BufferedInputStream(inputStream);

            try {
                Document document = create(stream, false);
                NodeList miriamNodes = document.getElementsByTagName("miriam");
                if (miriamNodes.getLength() != 1){
                    logger.error("More than one, or zero <miriam> elements in MIRIAM xml.");
                    return null;
                }
                Element miriamElement = (Element) miriamNodes.item(0);
                String dataVersion = miriamElement.getAttribute("data-version");

                // "2016-08-03T15:21:34+01:00"
                // SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                // dataVersion = dataVersion.replaceAll("\\+0([0-9]){1}\\:00", "+0$100");

                // Wed, 17 Aug 2016 14:57:52 GMT
                SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

                try {
                    Date date = format.parse(dataVersion);
                    return date;
                } catch (ParseException e){
                    logger.error("file data-version could not be parsed", e);
                    e.printStackTrace();
                    return null;
                }

            } catch (RuntimeException e) {
                throw e;
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
                }
            }
        } catch (FileNotFoundException e){
            e.printStackTrace();
        }
        return null;
    }


    /**
     *
     * Creates an XML DOM document by parsing the content of the specified byte
     * stream as XML, using a <i>nonvalidating</i> parser.
     *
     * @param byteStream The byte stream which content is parsed as XML to
     * create the XML DOM document.
     * @param namespaceAware A flag to indicate whether the parser should know
     * about namespaces or not.
     * @return The <code>org.w3c.dom.Document</code> instance representing
     * the XML DOM document created from the <code>byteStream</code>
     * XML content.
     * @throws NullPointerException If <code>byteStream</code> is
     * <code>null</code>.
     * @throws RuntimeException If any error occurs (parser configuration
     * errors, I/O errors, SAX parsing errors).
     *
     */
    private static Document create(InputStream byteStream,
                                   boolean namespaceAware) {
        if (byteStream == null) {
            throw new NullPointerException();
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(namespaceAware);

            factory.setValidating(false);

            try {
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            } catch (AbstractMethodError e) {
                // do nothing
            } catch (ParserConfigurationException e) {
                // do nothing
            }

            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(byteStream);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Script for updating the MIRIAM XML file in resources.
     */
    public static void main(String[] args){
        File miriamFile = new File("/home/mkoenig/git/cy3sbml/src/main/resources/miriam/" + FILENAME_MIRIAM);
        //updateMiriamXML(miriamFile);
        updateMiriamXMLWithNewer(miriamFile);
    }

}
