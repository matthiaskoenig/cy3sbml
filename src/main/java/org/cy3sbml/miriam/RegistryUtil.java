package org.cy3sbml.miriam;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;



import org.identifiers.registry.RegistryUtilities.*;
import org.cy3sbml.util.IOUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

//import org.xml.sax.SAXException;
//
//import javax.xml.parsers.DocumentBuilder;
//import javax.xml.parsers.DocumentBuilderFactory;
//import javax.xml.parsers.ParserConfigurationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONParserConfiguration;

/**
 * Tools for working with Miriam registry.
 * Here the MIRIAM xml file is loaded or updated.
 * http://www.ebi.ac.uk/miriam/main/export/
 */
public class RegistryUtil {
    private static final Logger logger = LoggerFactory.getLogger(RegistryUtil.class);
    public static final String URL_MIRIAM_JSON = "https://registry.api.identifiers.org/resolutionApi/getResolverDataset";
    public static final String FILENAME_MIRIAM = "getResolverDataset.json";

    /**
     * Load the registry from the resources.
     * @param file MIRIAM json file
     */
    public static void loadRegistry(File file) {
        if (file != null && file.exists()){
            try {

                RegistryDatabase.loadFromFile(file);
                logger.info("Load MIRIAM: " + file.getAbsolutePath());
                return;
            } catch (FileNotFoundException e) {
                logger.error("Problems loading the downloaded MIRIAM JSON.", e);
                e.printStackTrace();

            }
        }


    }

    /**
     * Load the registry from the resources.
     */

    /**
     * Only update MIRIAM if newer version is available.
     * Check last modified and use for update.
     */
    public static void updateMiriamJSONWithNewer(File file) throws MalformedURLException {

        Date fileDate = null;

        // check if file exists on harddisk and get date
        if (file!=null && file.exists()){
            // Get data-version of current file
            List<String> fileDates = getModifiedTimestampsFromFile(FILENAME_MIRIAM);
            logger.debug("data-version file: " + fileDate);
        } else {
            logger.warn("MIRIAM registry file does not exist locally");
        }

        // Get data-version of online resource
        List<String> urlDates = getURLModifiedTimestamps(new URL(URL_MIRIAM_JSON));


        // online version is newer
        for (int i = 0; i < urlDates.size(); i++) {
            if (urlDates.get(i).equals(fileDate.toString()) == false){
                updateMiriamJSON(file);
            }
        }
    }

    /**
     * Updates the MIRIAM registry file.
     * Downloads json from MIRIAM and saves in file.
     *
     * @param file MIRIAM json file
     */
    public static void updateMiriamJSON(File file){
        try {
            URL miriamURL = new URL(URL_MIRIAM_JSON);
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
    public static List<String> getURLModifiedTimestamps(URL url) {
        List<String> modifiedList = new ArrayList<>();
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("Accept", "application/json");
            conn.connect();

            InputStream input = conn.getInputStream();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(input);

            if (root.isArray()) {
                for (JsonNode entry : root) {
                    JsonNode modified = entry.get("modified");
                    if (modified != null) {
                        modifiedList.add(modified.asText());
                    }
                }
            }

            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return modifiedList;
    }

    /**
     * Retrieve the MIRIAM data-version from the given file.
     */
    public static List<String> getModifiedTimestampsFromFile(String fileName) {
        List<String> modifiedList = new ArrayList<>();
        try {
            // Load file from resources
            InputStream input = IOUtil.readResource("/miriam/" + fileName);
            if (input == null) {
                throw new IllegalArgumentException("File not found: " + fileName);
            }

            // Parse JSON
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(input);

            // Extract "modified" fields
            if (root.isArray()) {
                for (JsonNode entry : root) {
                    JsonNode modified = entry.get("modified");
                    if (modified != null) {
                        modifiedList.add(modified.asText());
                    }
                }
            }

            input.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return modifiedList;
    }





    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Script for updating the packaged MIRIAM XML file in src/main/resources.
     */
    public static void main(String[] args) throws FileNotFoundException, MalformedURLException {
        File miriamFile = new File("/home/mkoenig/git/cy3sbml/src/main/resources/miriam/" + FILENAME_MIRIAM);
        //updateMiriamXML(miriamFile);
        updateMiriamJSONWithNewer(miriamFile);
    }

}
