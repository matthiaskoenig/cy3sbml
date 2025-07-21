package org.cy3sbml.biomodelrest;

import org.json.*;

import java.io.IOException;
import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.cy3sbml.biomodelrest.rest.Biomodel;
import org.cy3sbml.biomodelrest.rest.BiomodelsQuery;

/**
 * Result of the given web service query.
 */
public class BiomodelsQueryResult {

	final private String query;
	final private Integer status;
	final private String json;
	
	public BiomodelsQueryResult(final String query, Integer status, String json){
		this.query = query;
		this.status = status;
		this.json = json;
	}
	
	/** Returns true if the request was successful. */
	public boolean success(){
		return (status == 200);
	}
	
	public String getQuery(){
		return query;
	}

	public Integer getStatus(){
		return status;
	}

	public String getJSON(){
	    return json;
    }

    private JSONObject getJSONObject(){
	    if (json == null){
	        return null;
        } else {
            JSONObject obj = new JSONObject(json);
            // System.out.println(obj.toString(2));
            return obj;
        }
    }


    /**
     * Parses the Biomodel information from a search query.
     * @return
     */
    public List<String> getBiomodelIdsFromSearch(){
        JSONObject jsonObject = getJSONObject();
        List<String> biomodelIds = new ArrayList<>();
        if (jsonObject != null){

            // get biomodel identifiers
            JSONArray array = jsonObject.getJSONArray("models");
            for (int i = 0; i < array.length(); i++) {
                JSONObject model = (JSONObject) array.get(i);
                String biomodelId = (String) model.get("id");
                biomodelIds.add(biomodelId);
            }
        }
        return biomodelIds;
    }


    /**
     * Returns biomodel information for given biomodel ids
     * @return
     */
    public static ArrayList<Biomodel> getBiomodelsFromIds(Iterable<String> biomodelIds) throws IOException, InterruptedException, ExecutionException {

        ArrayList<Biomodel> biomodels = new ArrayList<>();
        List<CompletableFuture<Biomodel>> futures = new ArrayList<>();
        for (String biomodelId: biomodelIds){
            CompletableFuture<Biomodel> future = BiomodelsQuery.performBiomodelQuery(biomodelId);
            futures.add(future);

        }
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        biomodels= allFutures.thenApply(v ->
                futures.stream()
                        .map(future -> {
                            try {
                                return future.join(); // Get each Biomodel
                            } catch (Exception e) {
                                System.err.println("Skipping failed model: " + e.getMessage());
                                return null; // or handle errors differently
                            }
                        })
                        .filter(Objects::nonNull) // Remove nulls (failed requests)
                        .collect(Collectors.toCollection(ArrayList::new))
        ).get();
        return biomodels;
    }




}
