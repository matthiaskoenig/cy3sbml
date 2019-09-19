package org.cy3sbml.biomodelrest.rest;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import org.json.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cy3sbml.biomodelrest.BiomodelsQueryResult;


/**
 * UniRest based REST queries for biomodels.
 */
public class BiomodelsQuery {
	private static Logger logger = LoggerFactory.getLogger(BiomodelsQuery.class);
	public static final String BIOMODELS_RESTFUL_URL = "https://wwwdev.ebi.ac.uk/biomodels";

	/**
	 * Create URI from query String.
	 *
	 * Performs necessary replacements and sanitation of query.
	 */
	public static URI uriFromQuery(String query) throws URISyntaxException {
        // FIXME: Necessary to url escape
        // https://stackoverflow.com/questions/724043/http-url-address-encoding-in-java#724764
	    query = query.replace(":", "%3A");
        query = query.replace(" ", "%20");
        query = query.replace("\"", "%22");
        query = query.replace(">", "%3E");
        query = query.replace("<", "%3C");
		URI uri = new URI(BIOMODELS_RESTFUL_URL + query);
		return uri;
	}

    /**
     * Run a biomodels query.
     * @param query
     * @return
     */
	public static BiomodelsQueryResult performSearchQuery(String query){
	    // TODO: handle the more complex cases, i.e. if there is pagination, than
        // FIXME: pagination - &offset=0&numResults=10
        // perform all the individual queries and combine the results.

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


    /**
     * Get information for given biomodel.
     * @param biomodelId
     * @return
     */
    public static Biomodel performBiomodelQuery(String biomodelId){
	    String query = "/" + biomodelId + "?format=json";
        HttpResponse<InputStream> response = executeQuery(query);
        if (response != null){
            Integer status = response.getStatus();
            String json = null;
            if (status == 200){
                json = getStringBody(response);
                JSONObject jsonObject = new JSONObject(json);
                return new Biomodel(jsonObject);
            }
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


    /* Test the Restful API. */
    public static void main(String[] args) throws URISyntaxException {

        BiomodelsQueryResult result = BiomodelsQuery.performSearchQuery("/BIOMD0000000012?format=json");
        System.out.println(result.getJSON());

        Biomodel biomodel = BiomodelsQuery.performBiomodelQuery("BIOMD0000000012");
        System.out.println(biomodel.getInfo());
        System.out.println(biomodel);



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
