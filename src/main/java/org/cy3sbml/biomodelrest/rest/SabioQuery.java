package org.cy3sbml.biomodelrest.rest;

import org.cy3sbml.biomodelrest.SabioQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

public abstract class SabioQuery{
	private static Logger logger = LoggerFactory.getLogger(SabioQuery.class);
	public static final String SABIORK_RESTFUL_URL = "http://sabiork.h-its.org/sabioRestWebServices";
	
	public static final String PREFIX_KINETIC_LAW_INFO = "http://sabiork.h-its.org/kineticLawEntry.jsp?viewData=true&kinlawid=";
	public static final String PREFIX_QUERY = "searchKineticLaws/sbml?q=";
	public static final String PREFIX_COUNT = "searchKineticLaws/count?q=";
	public static final String PREFIX_LAW = "kineticLaws/";
	public static final String PREFIX_LAWS = "kineticLaws?kinlawids=";
	public static final String CONNECTOR_AND = " AND ";

	public static final String STATUS_UP = "UP";
	public static final String STATUS_DOWN = "DOWN";
	
	
	public abstract SabioQueryResult performQuery(String query);
	public abstract Integer performCountQuery(String query);
	public abstract String getSabioStatus();
	
	
	/** 
	 * Create URI from query String.
     *
	 * Performs necessary replacements and sanitation of query, for 
	 * instance fixing issues with encoding. Handles the url
	 */
	public static URI uriFromQuery(String query) throws URISyntaxException{

        // the following is only a bug fix due to the encoding issues with web service
		query = query.replace(":", "%3A");
		query = query.replace(" ", "%20");
        query = query.replace("\"", "%22");
        query = query.replace(">", "%3E");
        query = query.replace("<", "%3C");

		URI uri = new URI(SabioQuery.SABIORK_RESTFUL_URL + "/" + query);
		return uri;
	}
	
	/**
	 * Convert sbml to count query if necessary.
	 */
	protected static String convertToCountQuery(String query){
		if (query.startsWith(SabioQuery.PREFIX_QUERY)){
			query = query.replace(SabioQuery.PREFIX_QUERY, SabioQuery.PREFIX_COUNT);
		}
		return query;
	}
	
	
	/** Generate query from ids. */
	public static String queryStringFromIds(Collection<Integer> ids){
		
		if (ids.size() == 1){
			return (SabioQuery.PREFIX_LAW + ids.iterator().next());	
		} else {
			String idText = null;
			for (Integer kid: ids){
				if (idText == null){
					idText = kid.toString();
				} else {
					idText += "," + kid.toString();
				}
			}
			return (SabioQuery.PREFIX_LAWS + idText);
	    }
	}
	
	
}
