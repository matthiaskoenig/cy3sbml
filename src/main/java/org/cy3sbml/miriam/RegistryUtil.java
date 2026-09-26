package org.cy3sbml.miriam;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.cy3sbml.util.IOUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.cy3sbml.miriam.Fields.*;

/**
 * Tools for working with Miriam registry.
 * Here the MIRIAM xml file is loaded or updated.
 * http://www.ebi.ac.uk/miriam/main/export/
 */
public class RegistryUtil {
    public static final String PAYLOAD = "payload";
    public static final String NAMESPACES = "namespaces";
    public static final String PREFIX = "prefix";
    private static final Logger logger = LoggerFactory.getLogger(RegistryUtil.class);
    public static final String URL_MIRIAM_JSON = "https://registry.api.identifiers.org/resolutionApi/getResolverDataset";
    private static final ObjectMapper MAPPER = new ObjectMapper();


    /**
     * Load the registry from the resources.
     *
     * @param file MIRIAM json file
     */

    public static Map<String, Namespace> loadRegistry(File file) throws IOException {

        byte[] jsonBytes = Files.readAllBytes(file.toPath());
        JsonNode root = MAPPER.readTree(jsonBytes);
        JsonNode payload = root.path(PAYLOAD);
        if (payload.isMissingNode()) {
            throw new IllegalArgumentException("Missing 'payload' object");
        }
        JsonNode namespaces = payload.path(NAMESPACES);
        if (!namespaces.isArray()) {
            throw new IllegalArgumentException("Missing 'namespaces' array");
        }
        Map<String, Namespace> result = new HashMap<>();
        for (JsonNode nsNode : namespaces) {
            String prefix = nsNode.path(PREFIX).asText(null);
            if (prefix == null || prefix.isEmpty()) continue;
            Map<Object, Object> nsData = MAPPER.convertValue(nsNode, new TypeReference<Map<Object, Object>>() {
            });

            result.put(prefix, new Namespace(nsData));
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
    public static void updateMiriamJSON(File file) {
        try {
            URL miriamURL = new URL(URL_MIRIAM_JSON);
            IOUtil.saveURLasFile(miriamURL, file);
            logger.info("Updated MIRIAM: " + file.getAbsolutePath());
        } catch (MalformedURLException e) {
            logger.error("MalformedURLException", e);
            e.printStackTrace();
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Script for updating the packaged MIRIAM XML file in src/main/resources.
     */
    public static Map<String, Namespace> getMiriamContent() {
        File f = null;
        Map<String, Namespace> result = null;
        try {
            f = File.createTempFile("MiriamRegistry", ".json");
            RegistryUtil.updateMiriamJSON(f);


            result = RegistryUtil.loadRegistry(f);
        } catch (IOException e) {

        }
        return result;
    }

    public static void main(String[] args) throws FileNotFoundException, MalformedURLException {


    }

}
