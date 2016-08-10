package org.cy3sbml.miriam;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import org.identifiers.registry.RegistryDatabase;
import org.cy3sbml.util.IOUtil;

import org.identifiers.registry.data.DataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                e.printStackTrace();
                logger.warn("Problems loading the downloaded MIRIAM XML, using packed resource.");
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
     * Updates the MiriamFile.
     * Downloads xml from MIRIAM and saves in file.
     *
     * FIXME: The HTTP header Last-Modified: will avoid that you download data more than once per release.
     *  This is currently not supported by miriam.
     *
     * @param file MIRIAM xml file
     */
    public static void updateMiriamXML(File file){
        try {
            URL miriamURL = new URL(URL_MIRIAM_XML);
            IOUtil.saveURLasFile(miriamURL, file);
            logger.info("Updated MIRIAM: " + file.getAbsolutePath());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Script for updating the MIRIAM XML file in resources.
     */
    public static void main(String[] args){
        File miriamFile = new File("/home/mkoenig/git/cy3sbml/src/main/resources/miriam/" + FILENAME_MIRIAM);
        updateMiriamXML(miriamFile);
    }

}
