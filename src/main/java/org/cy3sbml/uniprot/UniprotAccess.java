package org.cy3sbml.uniprot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.client.Client;
import uk.ac.ebi.uniprot.dataservice.client.ServiceFactory;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtService;

/**
 * Testing UniProt Access
 */
public class UniprotAccess {
    private static final Logger logger = LoggerFactory.getLogger(UniprotAccess.class);


    /**
     * Retrieve UniProt Entry by accession id.
     * Example: "P10415"
     */
    public static UniProtEntry getEntryByAccession(String accession){
        UniProtEntry entry = null;
        ServiceFactory serviceFactoryInstance = Client.getServiceFactoryInstance();
        UniProtService uniProtService = serviceFactoryInstance.getUniProtQueryService();
        try {
            // start the service
            uniProtService.start();

            // fetch emtry
            entry = uniProtService.getEntry(accession);
            if (entry == null) {
                logger.warn("UniProt Entry " + accession + " could not be retrieved");
            } else {
                logger.info("Retrieved UniProtEntry object");
                logger.info(entry.getUniProtId().getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // always remember to stop the service
            uniProtService.stop();
            System.out.println("service now stopped.");
        }
        return entry;
    }



    public static void main(String[] args){


    }

}
