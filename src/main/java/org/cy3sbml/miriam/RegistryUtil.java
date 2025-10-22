package org.cy3sbml.miriam;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONArray;

import org.cy3sbml.util.IOUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tools for working with Miriam registry.
 */
public class RegistryUtil {

    private static final String URL_MIRIAM_JSON = "https://registry.api.identifiers.org/resolutionApi/getResolverDataset";
    private static final Logger logger = LoggerFactory.getLogger(RegistryUtil.class);
    private static final Map<String, Namespace> namespaceMap = loadMiriamNamespaceMap();

    /**
     * Script for updating the packaged MIRIAM XML file in src/main/resources.
     */
    public static Map<String, Namespace> loadMiriamNamespaceMap() {
        Map<String, Namespace> namespaceMap = null;
        try {
            File f = File.createTempFile("MiriamRegistry", ".json");
            RegistryUtil.updateMiriamJSON(f);

            namespaceMap = RegistryUtil.loadRegistry(f);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return namespaceMap;
    }

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
            logger.info("Updated MIRIAM: {}", file.getAbsolutePath());
        } catch (MalformedURLException e) {
            logger.error("MalformedURLException", e);
        }
    }

    /**
     * Load registry from JSON file.
     *
     * @param fJSON MIRIAM json file
     */
    public static Map<String, Namespace> loadRegistry(File fJSON) throws IOException {

        byte[] jsonBytes = Files.readAllBytes(fJSON.toPath());
        JSONObject root = JSON.parseObject(jsonBytes);
        JSONObject payload = root.getJSONObject("payload");
        if (payload == null) {
            throw new IllegalArgumentException("Missing 'payload' object");
        }
        JSONArray namespaces = payload.getJSONArray("namespaces");
        if (namespaces == null) {
            throw new IllegalArgumentException("Missing 'namespaces' array");
        }
        Map<String, Namespace> map = new HashMap<>();
        for (int i = 0; i < namespaces.size(); i++) {
            JSONObject nsNode = namespaces.getJSONObject(i);
            String prefix = nsNode.getString("prefix");
            if (prefix == null || prefix.isEmpty()) continue;
            Map<Object, Object> nsData = new LinkedHashMap<>(nsNode);
            map.put(prefix, new Namespace(nsData));
        }

        return map;
    }

    /**
     * Convert resourceURI to compact identifier.
     *
     * https://identifiers.org/CHEBI:12345
     * http://identifiers.org/go/GO:0006114
     * http://identifiers.org/GO:0006114
     * @param resourceURI
     * @return
     */
    public static String compactIdFromResourceURI(String resourceURI) {
        String prefix = RegistryUtil.prefixFromResourceURI(resourceURI);
        if (prefix == null){
            return null;
        }

        // remove http
        String token = resourceURI.replace("https://identifiers.org/", "");
        token = token.replace("http://identifiers.org/", "");

        String compactId = null;
        String[] tokens = token.split("/");
        if (tokens.length == 1 && tokens[0].contains(":")) {
            compactId = tokens[0];
        } else if (tokens.length > 1){
            String id = token.replace(prefix + "/", "");
            if (id.startsWith(prefix.toUpperCase() + ":")){
                compactId = id;
            } else {
                compactId = prefix.toUpperCase() + ":" + id;
            }
        }
        return compactId;
    }

    public static String prefixFromResourceURI(String resourceURI) {
        String prefix = null;
        if (!resourceURI.contains("identifiers.org/")) {
            return null;
        }

        // remove prefix
        String token = resourceURI.replace("https://identifiers.org/", "");
        token = token.replace("http://identifiers.org/", "");

        String[] tokens = token.split("/");
        if (tokens.length == 1 && tokens[0].contains(":")) {
            String[] tokens2 = tokens[0].split(":");
            prefix = tokens2[0].toLowerCase();
        } else if (tokens.length > 1) {
            prefix = tokens[0].toLowerCase();
        }
        return prefix;
    }

    public static Namespace namespaceFromCompactId(String compactId) {
        Namespace namespace = null;
        if (compactId != null){
            String prefix = compactId.split(":")[0].toLowerCase();
            namespace = namespaceMap.get(prefix);
        }
        return namespace;
    }

    public static String idFromCompactId(String compactId) {
        String id = null;
        if (compactId != null){
            String prefix = compactId.split(":")[0];
            id = compactId.replace(prefix + ":", "");
        }
        return id;
    }

}
