package org.cy3sbml.chebi;

import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.ChebiWebServiceFault_Exception;
import uk.ac.ebi.chebi.webapps.chebiWS.model.Entity;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Testing ChEBI access.
 */
public class ChebiAccess {
    private static final Logger logger = LoggerFactory.getLogger(ChebiAccess.class);

    /**
     * Retrieve ChEBI Entity by accession id.
     * Example: "CHEBI:15377"
     */

    public static Entity getEntityByAccession(String accession) {
        Entity entity = null;
        try {
            // Create client
            ChebiWebServiceClient client = new ChebiWebServiceClient();

            // Get entity by accession number
            entity = client.getCompleteEntity(accession);
            logger.info("CHEBI ID: " + entity.getChebiId());

        } catch (ChebiWebServiceFault_Exception e) {
            e.printStackTrace();
        }
        return entity;
    }

    public static void main(String[] args){
        Entity entity = getEntityByAccession("CHEBI:456216");
        System.out.println(entity);
    }


}
