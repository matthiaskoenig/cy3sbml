package org.cy3sbml.ols;

import org.springframework.web.client.HttpClientErrorException;
import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfig;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Identifier;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import org.cy3sbml.IdentifiersConstants;
import org.cy3sbml.miriam.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Information for given OLSAccess.
 * This represents the information for a term.
 */
public class OLSAccess {
    private static final Logger logger = LoggerFactory.getLogger(OLSAccess.class);
    public static OLSClient olsClient = new OLSClient(new OLSWsConfig());

    /**
     * Gets the OLS term for a given identifier.
     * Example: "GO:0042752"
     * <p>
     * Returns NULL if not an ontology term, or no term.
     */
    public static Term getTerm(String identifier) {
        try {
            String[] tokens = identifier.split(":");
            if (tokens.length == 2) {
                Identifier id = new Identifier(identifier, Identifier.IdentifierType.OBO);
                return olsClient.getTermById(id, tokens[0]);
            }

            tokens = identifier.split("_");
            if (tokens.length == 2) {
                Identifier id = new Identifier(identifier, Identifier.IdentifierType.OWL);
                return olsClient.getTermById(id, tokens[0]);
            }

            // none of the strategies worked
            logger.warn("Identifier is not an ontology identifier: {}", identifier);
            return null;

        } catch (HttpClientErrorException e) {
            logger.warn("OLS term not found <{}>", identifier);
            return null;
        } catch (Throwable e) {
            logger.error("Error retrieving OLS term for: {}", identifier, e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create string representation of term.
     */
    public static String termToString(Term term) {
        if (term == null) {
            return null;
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
    public static boolean isPhysicalLocationOLS(Resource resource) {
        return resource.getResourceHomeUrl().contains(IdentifiersConstants.OLS_BASE_URL);
    }

    public static void main(String[] args) {
        Term term = OLSAccess.getTerm("GO:0042752");
        System.out.println(termToString(term));
    }

}
