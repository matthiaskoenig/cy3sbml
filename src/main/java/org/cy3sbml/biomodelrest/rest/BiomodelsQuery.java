package org.cy3sbml.biomodelrest.rest;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import org.cy3sbml.biomodelrest.BiomodelsQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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


    /*
     * TODO:
     * Separate functionality into searching models (which will return number of biomodel ids.
     * And lookup of models which requires single query for every model.
     */


	public static BiomodelsQueryResult performSearchQuery(String query){
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

    public static BiomodelsQueryResult performModelQuery(String biomodelId){
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


    /**
     * Web service queries.
     */
    public List<String> getBioModelIdsFromSearch(String name) {
        // TODO: implement
        String[] ids = null;
        return Arrays.asList(ids);
    }

    public List<Biomodel> getBiomodelsFromIds(){
        //TODO: implement
        LinkedList<Biomodel> biomodels = new LinkedList<>();
        return biomodels;
    }


    /* Test the Restful API. */
    public static void main(String[] args) throws URISyntaxException {

        BiomodelsQueryResult result = BiomodelsQuery.performSearchQuery("/BIOMD0000000012?format=json");
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
