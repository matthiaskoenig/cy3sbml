package org.cy3sbml.util;


import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.SBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnnotationUtil {
	private static final String SEPARATOR = ",";
	
	private static final Logger logger = LoggerFactory.getLogger(AnnotationUtil.class);
	
	 /**
	   * parses the CV-terms.
	   */
	  public static Properties parseCVTerms(SBase sbase) {
	    Properties props = new Properties();
	    
	    if (sbase.isSetAnnotation()){
	    	Annotation annotation = sbase.getAnnotation();
	    	for (CVTerm cvterm :annotation.getListOfCVTerms()){
	    		// add a property for every cvterm
	    		String qualifier = cvterm.getQualifier().getElementNameEquivalent();
	    		for (String resource : cvterm.getResources()){
	    			
	    			// TODO: qualifier multiplicity not handeled, i.e. can be IS and VERSION_OF, ...
	    			// to same collection
	    			
	    			// get id and collection from resource
	    			Map<String, String> map = getIdCollectionMapForURI(resource);
	    			
	    			String key = map.get("collection");
		    		String value = (String) props.getOrDefault(key, null);
		    		if (value == null){
		    			value = map.get("id");
		    		} else {
		    			value = value + SEPARATOR + map.get("id");
		    		}
		    		props.setProperty(key, value);
	    		}
	    	}
	    }
	    return props;
	  }
	  
		/**
		 * Split the information in url, resource, id.
		 * Examples are:
		 * 		<rdf:li rdf:resource="http://identifiers.org/chebi/CHEBI:17234"/>
		 * 		<rdf:li rdf:resource="http://identifiers.org/kegg.compound/C00293"/>
		 * 		"urn:miriam:kegg.compound:C00197" 
		 */
		public static Map<String, String> getIdCollectionMapForURI(final String rURI) {
			Map<String, String> map = new HashMap<String, String>();
			if (rURI.startsWith("http")){
				String[] items = rURI.split("/");
				map.put("id", items[items.length - 1]);
				// map.put("key", StringUtils.join(ArrayUtils.subarray(items, 0, items.length-1), "/"));
				map.put("collection", items[items.length - 2]);
			} else if (rURI.startsWith("urn")){
				String[] items = rURI.split(":");
				map.put("id", items[items.length - 1]);
				// map.put("collection", StringUtils.join(ArrayUtils.subarray(items, 0, items.length-1), ":"));
				map.put("collection", items[items.length - 2]);
			} else {
				logger.warn("rURI neither 'urn' nor 'http':" + rURI);
			}
			return map;
		}
	  
	  
}
