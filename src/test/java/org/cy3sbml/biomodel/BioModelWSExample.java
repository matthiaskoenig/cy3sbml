package org.cy3sbml.biomodel;

import java.util.List;
import uk.ac.ebi.biomodels.ws.BioModelsWSClient;
import uk.ac.ebi.biomodels.ws.BioModelsWSException;
import uk.ac.ebi.biomodels.ws.SimpleModel;

public class BioModelWSExample {

	
	private static String getTextReportForSimpleModel(SimpleModel simpleModel){
		String smId = simpleModel.getId();
		String smName = simpleModel.getName();
		String smPublicationId = simpleModel.getPublicationId();
		String smDateModified = simpleModel.getLastModificationDateStr();
		
		// List<String> smAuthors = simpleModel.getAuthors();
		// List<String> smEncoders = simpleModel.getEncoders();
		
		String report = String.format(
					"ID:\t %s \n" +
					"NAME:\t %s \n" +
					"PUBID:\t %s \n" +
					"MODIFIED:\t %s \n",
						smId, smName, smPublicationId, smDateModified
				);
		return report;
	}
	
	private static String getTextReportForIds(String[] modelIds){
		String report;
		report = String.format("********* modelIds ********\n");
		for (String id: modelIds){
			report += String.format("\t%s\n", id);
		}
		report += "************************";
		return report;
	}
	
	
	
	@SuppressWarnings("unused")
	private static void test() throws BioModelsWSException{
		BioModelsWSClient client = new BioModelsWSClient();
		/* Proxy necessary for Charite */
		String proxyHost = "proxy.charite.de";
		String proxyPort = "8080";
 		client.setProperty("http.proxyHost", proxyHost);
 		client.setProperty("http.proxyPort", proxyPort);
 		client.setProperty("socks.proxyHost", proxyHost);
 		client.setProperty("socks.proxyPort", proxyPort);
		
 		String searchString = "glycolysis";
		String[] modelIds = client.getModelsIdByName(searchString); 
		System.out.println(getTextReportForIds(modelIds));
		
		// get simple model for first returned id
		if (modelIds.length > 0){
			
			String id = modelIds[0];
			SimpleModel simpleModel = client.getSimpleModelById(id);
			
			String textReport = getTextReportForSimpleModel(simpleModel);
			System.out.println(textReport);
		}
		
		// get SimpleModels for all the search results
		List<SimpleModel> simpleModels = client.getSimpleModelsByIds(modelIds);	
		
		// Test special case
		String id = "MODEL8293171637";
		SimpleModel simpleModel = client.getSimpleModelById(id);
		System.out.println(id);
		System.out.println(simpleModel.getId());
	}
	
	public static void main(String[] args) throws BioModelsWSException{
		System.out.println("Run tests");
		test();
	}
}
