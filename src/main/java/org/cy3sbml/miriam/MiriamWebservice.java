package org.cy3sbml.miriam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.miriam.lib.MiriamLink;

/** Set up the Miriam Link to get data via the webservice. */
public class MiriamWebservice {
	private static final Logger logger = LoggerFactory.getLogger(MiriamWebservice.class);
	public static final String MIRIAM_URL = "http://www.ebi.ac.uk/miriamws/main/MiriamWebServices";
	
	public static MiriamLink getMiriamLink(){
		logger.info("MiriamLink created");
		MiriamLink link = new MiriamLink();
        link.setAddress(MIRIAM_URL);
        return link;
	}
	
	public static boolean test(){
		MiriamLink link = getMiriamLink();
		String test = link.getDataTypeURI("ChEBI");
		return (test != null);
	}
}
