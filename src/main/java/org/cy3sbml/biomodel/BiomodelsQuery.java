package org.cy3sbml.biomodel;

import java.net.http.HttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.json.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * UniRest based REST queries for biomodels.
 */
public class BiomodelsQuery {
	private static Logger logger = LoggerFactory.getLogger(BiomodelsQuery.class);
	public static final String BIOMODELS_RESTFUL_URL = "https://www.ebi.ac.uk/biomodels/";
	public static final String BIOMODELS_SEARCH = "search";
	public static final String BIOMODELS_BIOMODEL = "?format=json";

	/**
	 * Create URI from query String.
	 *
	 * Performs necessary replacements and sanitation of query.
	 */
	public static URI uriFromQuery(String query) throws URISyntaxException {
        // FIXME: Necessary to url escape
        // https://stackoverflow.com/questions/724043/http-url-address-encoding-in-java#724764
//	    query = query.replace(":", "%3A");
//        query = query.replace(" ", "%20");
//        query = query.replace("\"", "%22");
//        query = query.replace(">", "%3E");
//        query = query.replace("<", "%3C");

		URI uri = new URI(BIOMODELS_RESTFUL_URL + query);
		return uri;
	}

    /**
     * Run a biomodels query.
     * @param query
     * @return
     */
	public static BiomodelsQueryResult performSearchQuery(String query) throws IOException, InterruptedException {
	    // TODO: handle the more complex cases, i.e. if there is pagination, than
        // FIXME: pagination - &offset=0&numResults=10
        // perform all the individual queries and combine the results.

		String url = String.format("%s?query=%s&format=json",
				BIOMODELS_RESTFUL_URL + BIOMODELS_SEARCH,
				URLEncoder.encode(query, StandardCharsets.UTF_8));
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Accept", "application/json")
				.GET()
				.build();
		HttpResponse<InputStream> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

		if (response != null){
			Integer status = response.statusCode();
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
    public static CompletableFuture<Biomodel> performBiomodelQuery(String biomodelId) throws IOException, InterruptedException {
	    String query = biomodelId + "?format=json";
		String url = BIOMODELS_RESTFUL_URL + query;
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Accept", "application/json")
				.GET()
				.build();
		CompletableFuture<Biomodel> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
				.thenApply(response -> {
					if (response.statusCode() == 200) {
						try {
							String json = getStringBody(response); // Reuse your method
							JSONObject jsonObject = new JSONObject(json);
							return new Biomodel(jsonObject);
						} catch (Exception e) {
							throw new RuntimeException("Failed to parse biomodel: " + biomodelId, e);
						}
					} else {
						throw new RuntimeException("HTTP error for " + biomodelId + ": " + response.statusCode());
					}
				});

       return future;
    }


	public static String getBioModelSBMLById(String id) throws IOException, InterruptedException {

		String sbml = "";
		long start = System.currentTimeMillis();
		HttpResponse<String> sbmlResponse = getSBMLResponse(BIOMODELS_RESTFUL_URL,"model/download/",id);
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
	public static HttpResponse<String> getSBMLResponse(String base, String operation, String id) throws IOException, InterruptedException {
		String downloadUrl = base + operation + id + "?filename=" + id + "_url.xml";

		HttpClient client = HttpClient.newBuilder()
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(downloadUrl))
				.GET()
				.build();

		return client.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private static String getStringBody(HttpResponse<InputStream> ioResponse){
		InputStream inputStream = ioResponse.body();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		String content = bufferedReader.lines().collect(Collectors.joining("\n"));
		
		return content;
	}


    /* Test the Restful API. */
    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {

        BiomodelsQueryResult result = BiomodelsQuery.performSearchQuery("/BIOMD0000000012?format=json");
        System.out.println(result.getJSON());

        CompletableFuture<Biomodel> biomodel = BiomodelsQuery.performBiomodelQuery("BIOMD0000000012");

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
