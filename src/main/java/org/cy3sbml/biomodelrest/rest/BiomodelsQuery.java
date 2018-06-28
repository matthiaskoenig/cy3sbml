package org.cy3sbml.biomodelrest.rest;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import org.cy3sbml.biomodelrest.BiomodelsQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biomodels.ws.BioModelsWSClient;
import uk.ac.ebi.biomodels.ws.BioModelsWSException;
import uk.ac.ebi.biomodels.ws.SimpleModel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.*;


/**
 * UniRest based Sabio Queries.
 */
public class BiomodelsQuery {
	private static Logger logger = LoggerFactory.getLogger(BiomodelsQuery.class);
	public static final String BIOMODELS_RESTFUL_URL = "https://wwwdev.ebi.ac.uk/biomodels";
	public static final String CONNECTOR_AND = " AND ";

	/**
	 * Create URI from query String.
	 *
	 * Performs necessary replacements and sanitation of query.
	 */
	public static URI uriFromQuery(String query) throws URISyntaxException {
		URI uri = new URI(BIOMODELS_RESTFUL_URL + query);
		return uri;
	}

	public static BiomodelsQueryResult performQuery(String query){
		HttpResponse<InputStream> response = executeQuery(query);
		if (response != null){
			Integer status = response.getStatus();
			String json = null;
			if (status == 200){
				json = getStringBody(response);
			}
			return new BiomodelsQueryResult(query, status, json);
		}
		return null;
	}

	private static HttpResponse<InputStream> executeQuery(String query){
		try {
			URI uri = uriFromQuery(query);
			logger.info(uri.toString());
			HttpResponse<InputStream> ioResponse = Unirest.get(uri.toString())
														  .header("Accept", "text/json;charset=UTF-8")
														  .header("Content-Type", "text/json;charset=UTF-8")
														   .asBinary();
			return ioResponse;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static String getStringBody(HttpResponse<InputStream> ioResponse){
		InputStream inputStream = ioResponse.getRawBody();

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		String content = bufferedReader.lines().collect(Collectors.joining("\n"));
		
		return content;
	}




//
//
//    /**
//     * Web service queries.
//     */
//    public List<String> getBioModelIdsByName(String name) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByName(name);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByPerson(String person) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByPerson(person);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByPublication(String publication) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByPublication(publication);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByTaxonomy(String taxonomy) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByTaxonomy(taxonomy);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByTaxonomyId(String taxonomyId) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByTaxonomyId(taxonomyId);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByChebi(String chebi) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByChEBI(chebi);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByChebiId(String chebiId) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByChEBIId(chebiId);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByUniprot(String uniprot) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByUniprot(uniprot);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByUniprotId(String uniprotId) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByUniprotId(uniprotId);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public String getBioModelNameById(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String name = "";
//        try {
//            name = client.getModelNameById(id);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        return name;
//    }
//
//    public String getDateLastModifiedByModelId(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String date = "";
//        try {
//            // Date expressed according to ISO 8601
//            date = client.getLastModifiedDateByModelId(id);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        return date;
//    }
//
//    public List<String> getAuthorsByModelId(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] authors = null;
//        try {
//            authors = client.getAuthorsByModelId(id);
//        } catch (BioModelsWSException e) {
//            e.printStackTrace();
//        }
//        if (authors == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(authors);
//    }
//
//    public List<String> getEncodersByModelId(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] encoders = null;
//        try {
//            encoders = client.getEncodersByModelId(id);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (encoders == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(encoders);
//    }
//
//    public String getBioModelSBMLById(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String sbml = "";
//        try {
//            sbml = client.getModelSBMLById(id);
//            if (sbml == null) {
//                sbml = "";
//            }
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        return sbml;
//    }
//
//    public SimpleModel getSimpleModelById(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        SimpleModel model = null;
//        try {
//            model = client.getSimpleModelById(id);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        return model;
//    }
//
//    public LinkedHashMap<String, SimpleModel> getSimpleModelsByIds(String[] ids) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        LinkedHashMap<String, SimpleModel> simpleModels = null;
//        try {
//            List<SimpleModel> simpleModelsList = client.getSimpleModelsByIds(ids);
//            simpleModels = new LinkedHashMap<String, SimpleModel>();
//            for (int k = 0; k < simpleModelsList.size(); ++k) {
//                simpleModels.put(ids[k], simpleModelsList.get(k));
//            }
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        return simpleModels;
//    }
//


    /* Test the Restful API. */
    public static void main(String[] args) throws URISyntaxException {

        BiomodelsQueryResult result = BiomodelsQuery.performQuery("/BIOMD0000000012?format=json");
        JSONObject json = result.getJSONObject();
        Biomodel model = new Biomodel(json);
        System.out.println(model);


        // Download the OMEX archive
        // https://www.ebi.ac.uk/biomodels/model/download/BIOMD0000000012

        // Download single model file
        // https://www.ebi.ac.uk/biomodels/model/download/BIOMD0000000012?filename=BIOMD0000000012_url.xml

        // Search for models
        // https://www.ebi.ac.uk/biomodels/search?query=repressilator&format=json


        // newQuery("searchKineticLaws/sbml?q=Tissue:spleen AND Organism:\"Homo sapiens\"");
        // newQuery("searchKineticLaws/sbml?q=Tissue:spleen%20AND%20Organism:%22homo%20sapiens%22");
    }


}
