package org.cy3sbml.oven;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class ExampleUniRest {
	/**
	 * Create client and perform query.
	 * @throws UnirestException
	 */
	public static JsonNode newQuery(String uri) {
		JsonNode jsonNode = null;
        try {
			/**
			HttpResponse<JsonNode> jsonResponse = Unirest.post("http://httpbin.org/post")
					  .header("accept", "application/json")
					  .queryString("apiKey", "123")
					  .field("parameter", "value")
					  .field("foo", "bar")
					  .asJson();
			JsonNode json = jsonResponse.getBody();
			String output = json.toString();
			*/

			HttpResponse<JsonNode> response = Unirest.get(uri).asJson();
			System.out.println(response.getStatus());

			jsonNode = response.getBody();
			System.out.println("--------------------------------------------");
			System.out.println(jsonNode.toString());
			System.out.println("--------------------------------------------");
		} catch (UnirestException e){
			e.printStackTrace();
		}
        return jsonNode;
	}
	

	/*
	 * Test the Restful API.
	 *      <rdf:li rdf:resource="http://identifiers.org/biomodels.sbo/SBO:0000247" />
     *      <rdf:li rdf:resource="http://identifiers.org/chebi/CHEBI:25858" />
     *      <rdf:li rdf:resource="http://identifiers.org/kegg.compound/C13747" />
	 */
	public static void main(String[] args){

		// olsQuery("http://www.ebi.ac.uk/ols/api/ontologies?page=1&size=1");
        JsonNode jsonNode = newQuery("http://www.ebi.ac.uk/ols/api/ontologies?size=500");
        JSONObject jsonResponse = jsonNode.getObject();
        JSONArray ontologies = jsonResponse.getJSONObject("_embedded").getJSONArray("ontologies");

        for (int i=0; i<ontologies.length(); i++){
            String id = ontologies.getJSONObject(i).getString("ontologyId");
            System.out.println(id);
        }

        try {
            Unirest.shutdown();
        }catch (IOException e){
            e.printStackTrace();
        }
	}
}
