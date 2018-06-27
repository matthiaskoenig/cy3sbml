package org.cy3sbml.biomodelrest.rest;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class TestUniRest {
	/** Create client and perform query. 
	 * @throws UnirestException */
	public static void newQuery(String query) {
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
			
			URI uri = BiomodelsQuery.uriFromQuery(query);
			System.out.println(uri.toString());
			HttpResponse<String> response = Unirest.get(uri.toString()).asString();
			System.out.println(response.getStatus());
			String output = response.getBody();
			System.out.println("--------------------------------------------");
			System.out.println(output);
			System.out.println("--------------------------------------------");
			
			
			HttpResponse<InputStream> ioResponse = Unirest.get(uri.toString()).asBinary();
			InputStream inputStream = ioResponse.getRawBody();
			
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
			String content = bufferedReader.lines().collect(Collectors.joining("\n"));
			
			System.out.println("--------------------------------------------");
			System.out.println(content);
			System.out.println("--------------------------------------------");
			
			
			Unirest.shutdown();
		} catch (UnirestException | URISyntaxException | IOException e){
			e.printStackTrace();
		}
	}
	

	/* Test the Restful API. */
	public static void main(String[] args){				
		newQuery("/BIOMD0000000012?format=json");
		// newQuery("searchKineticLaws/sbml?q=Tissue:spleen AND Organism:\"Homo sapiens\"");
		// newQuery("searchKineticLaws/sbml?q=Tissue:spleen%20AND%20Organism:%22homo%20sapiens%22");
	}
}
