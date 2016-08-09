package org.cy3sbml.miriam;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

import org.identifiers.registry.RegistryDatabase;
import org.cy3sbml.util.IOUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tools for working with Miriam registry.
 *
 * http://www.ebi.ac.uk/miriam/main/export/
 */
public class RegistryUtil {
    private static final Logger logger = LoggerFactory.getLogger(RegistryUtil.class);
    public static final String URL_MIRIAM_XML = "http://www.ebi.ac.uk/miriam/main/export/xml";
    public static final String FILENAME_MIRIAM = "IdentifiersOrg-Registry.xml";

    /** Load the registry from the resources. */
    public static void loadRegistry(File file) {

        if (file.exists()){
            try {
                RegistryDatabase.loadFromFile(file);
                logger.info("Miriam loaded from file:" + file.getAbsolutePath());
                return;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                logger.warn("Problems loading the downloaded MIRIAM XML, using packed resource.");
            }
        }
        // something went wrong, using fallback
        loadRegistry();
    }

    /** Load the registry from the resources. */
    public static void loadRegistry() {
        InputStream miriamStream = IOUtil.readResource("/miriam/" + FILENAME_MIRIAM);
        RegistryDatabase.loadFromInputStream(miriamStream);
    }

    /**
     * Updates the MiriamFile.
     * The HTTP header Last-Modified: will avoid that you download data more than once per release,
     * if you use a download tool that makes use of this information,
     * e.g. the unix commands lwp-mirror or curl with the -z option.
     * Here are examples of how to do this in Perl:
     *
     */
    public static void updateMiriamXML(File file){
        try {
            URL miriamURL = new URL(URL_MIRIAM_XML);
            IOUtil.saveURLasFile(miriamURL, file);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        File miriamFile = new File("/home/mkoenig/git/cy3sbml/src/main/resources/miriam/" + FILENAME_MIRIAM);
        updateMiriamXML(miriamFile);
    }

}
