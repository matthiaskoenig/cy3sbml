package org.cy3sbml.biomodelrest.rest;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import org.cy3sbml.biomodelrest.SabioQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * UniRest based Sabio Queries.
 */
public class SabioQueryUniRest extends SabioQuery{
	private static Logger logger = LoggerFactory.getLogger(SabioQueryUniRest.class);

	@Override
	public SabioQueryResult performQuery(String query){
		HttpResponse<InputStream> response = executeQuery(query);
		if (response != null){
			Integer status = response.getStatus();
			String xml = null;
			if (status == 200){
				// xml = response.getBody();
				xml = getStringBody(response);
			}
			return new SabioQueryResult(query, status, xml);	
		}
		return null;
	}

	/**
	 * Performs query and returns the XML string.
	 *
	 * Returns null if problem.
	 * @param query
	 * @return
	 */
	public String performQueryXML(String query){
		HttpResponse<InputStream> response = executeQuery(query);
		if (response != null){
			if (response.getStatus() == 200){
				return getStringBody(response);
			}
		}
		return null;
	}

	@Override
	public Integer performCountQuery(String query) {
		query = convertToCountQuery(query);
		
		HttpResponse<InputStream> response = executeQuery(query);
		Integer count = -1;
		// success
		if (response != null && response.getStatus() == 200){
			String countString = getStringBody(response);
			try {
				count = Integer.parseInt(countString);
			} catch (NumberFormatException e){
				count = 0;
			}
		}
		return count;
	}

	@Override
	public String getSabioStatus() {
		String status = SabioQuery.STATUS_DOWN;
		HttpResponse<InputStream> response = executeQuery("status");
		if (response != null){
			status = getStringBody(response);
		}
		return status;
	}
	
	private HttpResponse<InputStream> executeQuery(String query){
		try {
			URI uri = uriFromQuery(query);
			logger.info(uri.toString());
			// HttpResponse<String> response = Unirest.get(uri.toString())
			//									   .asString();
			
			HttpResponse<InputStream> ioResponse = Unirest.get(uri.toString())
														  .header("Accept", "text/xml;charset=UTF-8")
														  .header("Content-Type", "text/xml;charset=UTF-8")
														   .asBinary();
			return ioResponse;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private String getStringBody(HttpResponse<InputStream> ioResponse){ 
		InputStream inputStream = ioResponse.getRawBody();

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		String content = bufferedReader.lines().collect(Collectors.joining("\n"));
		
		return content;
	}

}
