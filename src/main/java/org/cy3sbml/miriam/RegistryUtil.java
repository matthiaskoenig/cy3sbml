package org.cy3sbml.miriam;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.*;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;


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



    /**
     * Load the registry from the resources.
     * @param file MIRIAM json file
     */

    public static Map<String, Namespace> loadRegistry(File file) throws IOException {

        byte[] jsonBytes = Files.readAllBytes(file.toPath());
        JSONObject root = JSON.parseObject(jsonBytes);
        JSONObject payload = root.getJSONObject(PAYLOAD);
        if (payload == null) {
            throw new IllegalArgumentException("Missing 'payload' object");
        }
        JSONArray namespaces = payload.getJSONArray(NAMESPACES);
        if (namespaces == null) {
            throw new IllegalArgumentException("Missing 'namespaces' array");
        }
        Map<String, Namespace> result = new HashMap<>();
        for (int i = 0; i < namespaces.size(); i++) {
            JSONObject nsNode = namespaces.getJSONObject(i);
            String prefix = nsNode.getString(PREFIX);
            if (prefix == null || prefix.isEmpty()) continue;
            Map<Object, Object> nsData = new LinkedHashMap<>(nsNode);

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



    //////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Script for updating the packaged MIRIAM XML file in src/main/resources.
     */
    public static void main(String[] args) throws FileNotFoundException, MalformedURLException {


    }

}
