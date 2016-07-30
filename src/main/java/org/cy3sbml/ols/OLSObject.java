package org.cy3sbml.ols;


import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;



/**
 * Information for given OLSObject.
 * This represent the information for a term.
 */
public class OLSObject {
    public final static String OLS_BASE_URL = "http://www.ebi.ac.uk/ols/ontologies/";


    private String uri;
    private Term term;

    public OLSObject(String resourceURI){
        uri = resourceURI;

        // TODO: lookup the information via OLS
        term = null;
    }
}
