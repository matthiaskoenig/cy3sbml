package org.cy3sbml.util;

import java.util.Properties;

import org.identifiers.registry.RegistryUtilities;
import org.identifiers.registry.data.DataType;

import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.SBase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tools for working with annotations.
 */
public class AnnotationUtil {
	private static final Logger logger = LoggerFactory.getLogger(AnnotationUtil.class);
	
	/**
     * Parses the CV-terms into properties.
     */
	public static Properties parseCVTerms(SBase sbase) {
	    Properties props = new Properties();
	    
	    if (sbase.isSetAnnotation()){
	    	Annotation annotation = sbase.getAnnotation();
	    	for (CVTerm cvterm :annotation.getListOfCVTerms()){
	    	    // add property for every cvterm
                for (String resourceURI : cvterm.getResources()){
                    String dataCollection = RegistryUtilities.getDataCollectionPartFromURI(resourceURI);
                    DataType dataType = RegistryUtilities.getDataType(dataCollection);
                    String identifier = RegistryUtilities.getIdentifierFromURI(resourceURI);

                    // Store under the namespace of the data collection
                    if (dataType != null && identifier != null){
                        props.setProperty(dataType.getNamespace(), identifier);
                    }
	    		}
	    	}
	    }

	    return props;
	  }

}
