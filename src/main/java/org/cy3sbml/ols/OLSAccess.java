package org.cy3sbml.ols;


import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfigProd;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Identifier;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import org.cy3sbml.miriam.RegistryUtil;
import org.identifiers.registry.RegistryUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Information for given OLSAccess.
 * This represent the information for a term.
 * FIXME: probably not a good idea to have one static client (multi-threading)
 */
public class OLSAccess {
    private static final Logger logger = LoggerFactory.getLogger(OLSAccess.class);
    public final static String OLS_BASE_URL = "http://www.ebi.ac.uk/ols/ontologies/";
    private static OLSClient olsClient = new OLSClient(new OLSWsConfigProd());

    /**
     * Gets the OLS term for a given MIRIAM resourceURI.
     * Example: "GO:0042752"
     *
     * Returns NULL if not an ontology term, or no term.
     */
    public static Term getTerm(String identifier){
        try {
            String[] tokens = identifier.split(":");
            if (tokens.length != 2) {
                logger.warn(String.format("Identifier is not an ontology identifier: %s", identifier));
                return null;
            }
            String ontologyId = tokens[0];
            Identifier id = new Identifier(identifier, Identifier.IdentifierType.OBO);
            Term term = olsClient.getTermById(id, ontologyId);
            return term;
        } catch (Throwable e){
            logger.error(String.format("Error retrieving OLS term for: %s", identifier));
            e.printStackTrace();
            return null;
        }
    }

    /** Create string representation of term. */
    public static String termToString(Term term){
        if (term == null){
            return term.toString();
        }
        return String.format(
            "iri:           %s\n" +
            "label:         %s\n" +
            "description:   %s\n" +
            "shortForm:     %s\n" +
            "oboId:         %s\n" +
            "ontologyName:  %s\n" +
            "oboDefinitionCitations:  %s\n",
                term.getIri(),
                term.getLabel(),
                term.getDescription(),
                term.getShortForm(),
                term.getTermOBOId(),
                term.getOntologyName(),
                term.getOboDefinitionCitation()
        );
    }


    public static void main(String[] args){
        // prepare miriam registry support
        RegistryUtil.loadRegistry();

        // Exists on OLS
        String resourceURI = "http://identifiers.org/go/GO:0042752";
        String identifier = RegistryUtilities.getIdentifierFromURI(resourceURI);


        Term term = getTerm(identifier);
        // Term term = olsClient.getTermById(new Identifier("GO:0042752", Identifier.IdentifierType.OBO), "GO");
        System.out.println(termToString(term));
    }

}
