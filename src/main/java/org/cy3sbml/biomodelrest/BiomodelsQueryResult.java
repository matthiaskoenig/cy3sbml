package org.cy3sbml.biomodelrest;

import org.json.*;
import java.util.*;

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
    public HashSet<String> getBiomodelIdsFromSearch(){
        JSONObject jsonObject = getJSONObject();
        HashSet<String> biomodelIds = new HashSet<String>();
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
    public ArrayList<Biomodel> getBiomodelsFromIds(Iterable<String> biomodelIds){

        ArrayList<Biomodel> biomodels = new ArrayList<>();
        for (String biomodelId: biomodelIds){
            Biomodel biomodel = BiomodelsQuery.performBiomodelQuery(biomodelId);
            biomodels.add(biomodel);
        }
        return biomodels;
    }




}
