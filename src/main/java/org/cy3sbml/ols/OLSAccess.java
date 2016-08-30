package org.cy3sbml.ols;


import org.identifiers.registry.data.PhysicalLocation;
import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfigProd;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Identifier;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

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
     * Gets the OLS term for a given identifier.
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
        } catch (HttpClientErrorException e) {
            logger.warn(String.format("OLS term not found <%s>", identifier));
            return null;
        } catch (Throwable e){
            logger.error(String.format("Error retrieving OLS term for: %s", identifier), e);
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

    /**
     * Is a given location a OLS location, i.e. an ontology in OLS.
     */
    public static boolean isPhysicalLocationOLS(PhysicalLocation location){
        return location.getUrlRoot().startsWith(OLS_BASE_URL);
    }

    public static void main(String[] args){
        // Exists on OLS
        String resourceURI = "http://identifiers.org/go/GO:0042752";
        String identifier = RegistryUtilities.getIdentifierFromURI(resourceURI);

        Term term = OLSAccess.getTerm(identifier);
        System.out.println(OLSAccess.termToString(term));
    }

}
