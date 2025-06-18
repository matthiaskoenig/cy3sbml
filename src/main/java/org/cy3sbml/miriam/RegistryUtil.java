package org.cy3sbml.miriam;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.type.TypeReference;
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
    private static final ObjectMapper mapper = new ObjectMapper();
    /**
     * Load the registry from the resources.
     * @param file MIRIAM json file
     */
    public static Map<String, Namespace> loadRegistry(File file) throws IOException {

        Map<String, Map<String, String>> data = null;
        if (file != null && file.exists()) {
            updateMiriamJSON(file);
            try {

                data = readJsonCache(file);

                logger.info("Load MIRIAM: " + file.getAbsolutePath());
                if (data == null) {
                    throw new IllegalArgumentException("Registry could not be loaded from cache");
                }


            } catch (FileNotFoundException e) {
                logger.error("Problems loading the downloaded MIRIAM JSON.", e);
                e.printStackTrace();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        JsonNode namespacesData = mapper.readTree(file).findValue("namespaces");

        mapper.setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL);
        mapper.configOverride(String.class)
                .setSetterInfo(JsonSetter.Value.forValueNulls(Nulls.SKIP));
        // 3. Log raw input (debug level)
        logger.info("Raw registry data loaded: " + namespacesData);

        // 4. Convert to Namespace objects
        Map<String, Namespace> result = new HashMap<>();
        Map<String, Resource> resourceResult = new HashMap<>();
        for (JsonNode nsNode : namespacesData) {
            try {
                String prefix = nsNode.path("prefix").asText();
                if (prefix.isEmpty()) continue;

                Map<Object, Object> nsData = mapper.convertValue(nsNode,
                        new TypeReference<Map<Object, Object>>() {});

                result.put(prefix, new Namespace(nsData));
                JsonNode resourcesNode = nsNode.findValue("resources");
                for (int j=0; j<resourcesNode.size(); j++) {
                    String mirId = resourcesNode.get(j).get("mirId").asText();
                    Map<Object, Object> rsData = mapper.convertValue(resourcesNode.get(j),
                            new TypeReference<Map<Object, Object>>() {});
                    resourceResult.put(mirId, new Resource(rsData));
                }
                //System.out.println("DEBUG: Added " + prefix);  // Verify each addition
            } catch (Exception e) {
                System.err.println("Failed to parse namespace: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return result;
    }

    /**
     * Load the registry from the resources.
     */

    /**
     * Only update MIRIAM if newer version is available.
     * Check last modified and use for update.
     */


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



    @SuppressWarnings("unchecked")
    public static Map<String, Map<String, String>> readJsonCache(File cacheFile) throws IOException {
        try {
            if (cacheFile.length() == 0) {
                return null;
            }
            return mapper.readValue(cacheFile, Map.class);

        } catch (IOException e) {
            // Handle file read or JSON parse errors
            return null;
        }
    }





    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Script for updating the packaged MIRIAM XML file in src/main/resources.
     */
    public static void main(String[] args) throws FileNotFoundException, MalformedURLException {
        File miriamFile = new File("/home/mkoenig/git/cy3sbml/src/main/resources/miriam/" + FILENAME_MIRIAM);
        //updateMiriamXML(miriamFile);

    }

}
