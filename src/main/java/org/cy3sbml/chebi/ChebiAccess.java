package org.cy3sbml.chebi;

import uk.ac.ebi.chebi.webapps.chebiWS.client.ChebiWebServiceClient;
import uk.ac.ebi.chebi.webapps.chebiWS.model.ChebiWebServiceFault_Exception;
import uk.ac.ebi.chebi.webapps.chebiWS.model.DataItem;
import uk.ac.ebi.chebi.webapps.chebiWS.model.Entity;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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

        String text = "";
        String formula = "";
        List<DataItem> items = entity.getFormulae();
        if (items != null && items.size() > 0){
            formula = items.get(0).getData();
        }
        text += "Formula:" + formula + "\n";
        text += "Net charge:" + entity.getCharge() + "\n";
        text += "Average mass:" + entity.getMass() + "\n";
        text += "Stars:" + entity.getEntityStar() + "\n";

        System.out.println(text);
    }


}
