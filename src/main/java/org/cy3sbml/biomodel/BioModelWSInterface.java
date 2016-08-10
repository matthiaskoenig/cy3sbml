package org.cy3sbml.biomodel;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.cy3sbml.ConnectionProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.biomodels.ws.BioModelsWSClient;
import uk.ac.ebi.biomodels.ws.BioModelsWSException;
import uk.ac.ebi.biomodels.ws.SimpleModel;


/** 
 * Main class for interaction with the BioModels via web services. 
 */
public class BioModelWSInterface {
	private static final Logger logger = LoggerFactory.getLogger(BioModelWSInterface.class);
	
	private String proxyHost;
	private String proxyPort;
	
	public BioModelWSInterface(String pHost, String pPort){
		proxyHost = pHost;
		proxyPort = pPort;
	}
	
	public BioModelWSInterface(){
		this(null, null);
	}
	
	public BioModelWSInterface(ConnectionProxy connectionProxy){
		this(null, null);
		if (!"direct".equals(connectionProxy.getProxyType())){
			proxyHost = connectionProxy.getProxyHost();
			proxyPort = connectionProxy.getProxyPort();
		}
	}
		
	private BioModelsWSClient createBioModelsWSClient(){
		BioModelsWSClient client = new BioModelsWSClient();
		setProxyForClient(client);
		return client;
	}
	
	private void setProxyForClient(BioModelsWSClient client){
		if (proxyHost != null && proxyPort != null){
	 		client.setProperty("http.proxyHost", proxyHost);
	 		client.setProperty("http.proxyPort", proxyPort);
	 		client.setProperty("socks.proxyHost", proxyHost);
	 		client.setProperty("socks.proxyPort", proxyPort);
	 	}
	}
	
    /** Web service queries. */
	public List<String> getBioModelIdsByName(String name){
		BioModelsWSClient client = createBioModelsWSClient();
		String[] ids = null;
		try {
			ids = client.getModelsIdByName(name);
		} catch (BioModelsWSException e) {
			logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		if (ids == null){ return new LinkedList<String>(); }
		return Arrays.asList(ids);
	}
	
	public List<String> getBioModelIdsByPerson(String person){
		BioModelsWSClient client = createBioModelsWSClient();
		String[] ids = null;
		try {
			ids = client.getModelsIdByPerson(person);
		} catch (BioModelsWSException e) {
			logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		if (ids == null){ return new LinkedList<String>(); }
		return Arrays.asList(ids);
	}
	
	public List<String> getBioModelIdsByPublication(String publication){
		BioModelsWSClient client = createBioModelsWSClient();
		String[] ids = null;
		try {
			ids = client.getModelsIdByPublication(publication);
		} catch (BioModelsWSException e) {
			logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		if (ids == null){ return new LinkedList<String>(); }
		return Arrays.asList(ids);
	}
	
	public List<String> getBioModelIdsByTaxonomy(String taxonomy){
		BioModelsWSClient client = createBioModelsWSClient();
		String[] ids = null;
		try {
			ids = client.getModelsIdByTaxonomy(taxonomy);
		} catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		if (ids == null){ return new LinkedList<String>(); }
		return Arrays.asList(ids);
	}
	
	public List<String> getBioModelIdsByTaxonomyId(String taxonomyId){
		BioModelsWSClient client = createBioModelsWSClient();
		String[] ids = null;
		try {
			ids = client.getModelsIdByTaxonomyId(taxonomyId);
		} catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		if (ids == null){ return new LinkedList<String>(); }
		return Arrays.asList(ids);
	}
	
	public List<String> getBioModelIdsByChebi(String chebi){
		BioModelsWSClient client = createBioModelsWSClient();
		String[] ids = null;
		try {
			ids = client.getModelsIdByChEBI(chebi);
		} catch (BioModelsWSException e) {
			logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		if (ids == null){ return new LinkedList<String>(); }
		return Arrays.asList(ids);
	}
	
	public List<String> getBioModelIdsByChebiId(String chebiId){
		BioModelsWSClient client = createBioModelsWSClient();
		String[] ids = null;
		try {
			ids = client.getModelsIdByChEBIId(chebiId);
		} catch (BioModelsWSException e) {
			logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		if (ids == null){ return new LinkedList<String>(); }
		return Arrays.asList(ids);
	}
	
	public List<String> getBioModelIdsByUniprot(String uniprot){
		BioModelsWSClient client = createBioModelsWSClient();
		String[] ids = null;
		try {
			ids = client.getModelsIdByUniprot(uniprot);
		} catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		if (ids == null){ return new LinkedList<String>(); }
		return Arrays.asList(ids);
	}
	
	public List<String> getBioModelIdsByUniprotId(String uniprotId){
		BioModelsWSClient client = createBioModelsWSClient();
		String[] ids = null;
		try {
			ids = client.getModelsIdByUniprotId(uniprotId);
		} catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		if (ids == null){ return new LinkedList<String>(); }
		return Arrays.asList(ids);
	}
	
	public String getBioModelNameById(String id){
		BioModelsWSClient client = createBioModelsWSClient();
		String name = "";
		try {
			name = client.getModelNameById(id);
		} catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		return name;
	}
	
	public String getDateLastModifiedByModelId(String id){		
		BioModelsWSClient client = createBioModelsWSClient();
		String date = "";
		try {
			// Date expressed according to ISO 8601
			date = client.getLastModifiedDateByModelId(id);
		} catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		return date;
	}
	
	public List<String> getAuthorsByModelId(String id){		
		BioModelsWSClient client = createBioModelsWSClient();
		String[] authors = null;
		try {
			authors = client.getAuthorsByModelId(id);
		} catch (BioModelsWSException e) {
			e.printStackTrace();
		}
		if (authors == null){ return new LinkedList<String>(); }
		return Arrays.asList(authors);
	}
	
	public List<String> getEncodersByModelId(String id){
	    BioModelsWSClient client = createBioModelsWSClient();
	    String[] encoders = null;
	    try {
			encoders = client.getEncodersByModelId(id);
		} catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
	    if (encoders == null){ return new LinkedList<String>(); }
		return Arrays.asList(encoders);
	}
	
	public String getBioModelSBMLById(String id){
    	BioModelsWSClient client = createBioModelsWSClient();
    	String sbml = "";
    	try {
			sbml = client.getModelSBMLById(id);
			if (sbml == null){
				sbml = "";
			}
		} catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
    	return sbml;
	}
	
	public SimpleModel getSimpleModelById(String id){
		BioModelsWSClient client = createBioModelsWSClient();
		SimpleModel model = null;
		try {
			model = client.getSimpleModelById(id);
		} catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		return model;
	}
	
	public LinkedHashMap<String, SimpleModel> getSimpleModelsByIds(String[] ids){
		BioModelsWSClient client = createBioModelsWSClient();
		LinkedHashMap<String, SimpleModel> simpleModels = null;
		try {
			List<SimpleModel> simpleModelsList = client.getSimpleModelsByIds(ids);
			simpleModels = new LinkedHashMap<String, SimpleModel>();
			for(int k=0; k<simpleModelsList.size(); ++k){
				simpleModels.put(ids[k], simpleModelsList.get(k));
			}
		} catch (BioModelsWSException e) {
            logger.error("BioModelsWSException", e);
			e.printStackTrace();
		}
		return simpleModels;
	}
	
	/** Connection test. */
	public static boolean testBioModelConnection(String proxyHost, String proxyPort) {
		BioModelWSInterface gbm = new BioModelWSInterface(proxyHost, proxyPort);
	    boolean connected = gbm.testBioModel();
	    return connected;
	}
	
	private boolean testBioModel(){
	 	BioModelsWSClient client = createBioModelsWSClient();
		String test = null;
		try {
			test = client.getModelNameById("BIOMD0000000070");
		} catch (BioModelsWSException e) {
			logger.warn("BioModelsWSException accessing BioModels");
			return false;
		}
	 	return (test != null);
	}
}