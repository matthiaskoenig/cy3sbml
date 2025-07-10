package org.cy3sbml.biomodel;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cy3sbml.ConnectionProxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.cy3sbml.biomodel.SimpleModel;


/**
 * Main class for interaction with the BioModels via web services.
 */
public class BioModelWSInterface {
    private static final Logger logger = LoggerFactory.getLogger(BioModelWSInterface.class);
    private static final String BIOMODEL_BASE = "https://www.ebi.ac.uk/biomodels/";
    private String proxyHost;
    private String proxyPort;

    public BioModelWSInterface(String pHost, String pPort) {
        proxyHost = pHost;
        proxyPort = pPort;
    }


    public BioModelWSInterface(ConnectionProxy connectionProxy) {
        this(null, null);
        if (!"direct".equals(connectionProxy.getProxyType())) {
            proxyHost = connectionProxy.getProxyHost();
            proxyPort = connectionProxy.getProxyPort();
        }
    }


    /**
     * Web service queries.
     */
    public List<String> getBioModelIdsByName(String name) throws IOException, InterruptedException {
        JsonNode modelsList = getModelsfromJson(name);
        List<String> modelIds = new LinkedList<>();
        for (int i = 0; i < modelsList.size(); i++) {
            JsonNode model = modelsList.get(i);
            String modelId = model.get("id").asText();
            assert false;
            modelIds.add(modelId);
        }
        return modelIds;
    }

    public Map<String, SimpleModel> getModelsfromJson(String name) throws IOException, InterruptedException {
        HttpResponse<String> response = getResultUrl(BIOMODEL_BASE, "search", name);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode modelsNode = mapper.readTree(response.body()).findValue("models");
        Map<String, SimpleModel> simpleModels = new HashMap<>();
        for (JsonNode modelNode : modelsNode) {
            String modelId = modelNode.get("id").asText();
            if (modelId.isEmpty()) continue;
            Map<String, String> simpleModelData = mapper.convertValue(modelNode, new TypeReference<Map<String, String>>() {
            });
            simpleModels.put(modelId, new SimpleModel(simpleModelData));
        }
        return simpleModels;
    }

    public HttpResponse<String> getResultUrl(String base, String operation, String name) throws IOException, InterruptedException {
        String url = String.format("%s?query=%s&format=json",
                base+operation,
                URLEncoder.encode(name, StandardCharsets.UTF_8));

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .GET()
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<String> getSBMLResponse(String base, String operation, String id) throws IOException, InterruptedException {
        String downloadUrl = base + operation + id + "?filename=" + id + "_url.xml";
        System.out.println("downloadUrl: " + downloadUrl);
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .GET()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
    public String getBioModelNameById(String id) {
        BioModelsWSClient client = createBioModelsWSClient();
        String name = "";
        try {
            name = client.getModelNameById(id);
        } catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
            e.printStackTrace();
        }
        return name;
    }

    public String getDateLastModifiedByModelId(String id) {
        BioModelsWSClient client = createBioModelsWSClient();
        String date = "";
        try {
            // Date expressed according to ISO 8601
            date = client.getLastModifiedDateByModelId(id);
        } catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
            e.printStackTrace();
        }
        return date;
    }

    public List<String> getAuthorsByModelId(String id) {
        BioModelsWSClient client = createBioModelsWSClient();
        String[] authors = null;
        try {
            authors = client.getAuthorsByModelId(id);
        } catch (BioModelsWSException e) {
            e.printStackTrace();
        }
        if (authors == null) {
            return new LinkedList<String>();
        }
        return Arrays.asList(authors);
    }

    public List<String> getEncodersByModelId(String id) {
        BioModelsWSClient client = createBioModelsWSClient();
        String[] encoders = null;
        try {
            encoders = client.getEncodersByModelId(id);
        } catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
            e.printStackTrace();
        }
        if (encoders == null) {
            return new LinkedList<String>();
        }
        return Arrays.asList(encoders);
    }

    public String getBioModelSBMLById(String id) throws IOException, InterruptedException {

        String sbml = "";
        HttpResponse<String> sbmlResponse = getSBMLResponse(BIOMODEL_BASE,"model/download/",id);
        if (sbmlResponse.statusCode() == 200) {
            // The response body contains the SBML XML content
            sbml = sbmlResponse.body();


            // You can save it to a file if needed
            // Files.writeString(Path.of(modelId + ".xml"), sbmlContent);
        } else {
            System.err.println("Failed to download SBML. Status code: " + sbmlResponse.statusCode());
        }

        return sbml;
    }

    public SimpleModel getSimpleModelById(String id) {
        BioModelsWSClient client = createBioModelsWSClient();
        SimpleModel model = null;
        try {
            model = client.getSimpleModelById(id);
            model.
        } catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
            e.printStackTrace();
        }
        return model;
    }

    public LinkedHashMap<String, SimpleModel> getSimpleModelsByIds(String[] ids) {

        LinkedHashMap<String, SimpleModel> simpleModels = null;
        try {
            List<SimpleModel> simpleModelsList = client.getSimpleModelsByIds(ids);
            simpleModels = new LinkedHashMap<String, SimpleModel>();
            for (int k = 0; k < simpleModelsList.size(); ++k) {
                simpleModels.put(ids[k], simpleModelsList.get(k));
            }
        } catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
            e.printStackTrace();
        }
        return simpleModels;
    }

    /**
     * Connection test.
     */
    public static boolean testBioModelConnection(String proxyHost, String proxyPort) {
        BioModelWSInterface gbm = new BioModelWSInterface(proxyHost, proxyPort);
        boolean connected = gbm.testBioModel();
        return connected;
    }

    private boolean testBioModel() {
        BioModelsWSClient client = createBioModelsWSClient();
        String test = null;
        try {
            test = client.getModelNameById("BIOMD0000000070");
        } catch (BioModelsWSException e) {
            logger.warn("BioModelsWSException accessing BioModels");
            return false;
        }
        return (test != null);
    }
}