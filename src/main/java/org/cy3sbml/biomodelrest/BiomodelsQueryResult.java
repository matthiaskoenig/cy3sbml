package org.cy3sbml.biomodelrest;

import org.sbml.jsbml.SBMLDocument;
import java.util.ArrayList;
import org.json.*;

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

    public JSONObject getJSONObject(){
        JSONObject obj = new JSONObject(json);
        System.out.println(obj);
        return obj;
    }
	


}