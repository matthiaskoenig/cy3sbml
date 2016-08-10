package org.cy3sbml.uniprot;


import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.client.Client;
import uk.ac.ebi.uniprot.dataservice.client.QueryResult;
import uk.ac.ebi.uniprot.dataservice.client.ServiceFactory;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtQueryBuilder;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtService;
import uk.ac.ebi.uniprot.dataservice.query.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Access UniProt information.
 */
public class UniprotAccess {
    private static final Logger logger = LoggerFactory.getLogger(UniprotAccess.class);

    /**
     * Retrieve UniProt Entry by accession id.
     *
     * @param accession UniProt accession id, e.g. "P10415"
     * @return
     */
    public static UniProtEntry getUniProtEntry(String accession){
        UniProtEntry entry = null;
        ServiceFactory serviceFactoryInstance = Client.getServiceFactoryInstance();
        UniProtService uniProtService = serviceFactoryInstance.getUniProtQueryService();
        try {
            // start the service
            uniProtService.start();

            // fetch entry
            entry = uniProtService.getEntry(accession);

            if (entry == null){
                // is secondary accession, get first result
                logger.debug("Querying any accession: " + accession);
                Query query = UniProtQueryBuilder.anyAccession(accession);
                QueryResult<UniProtEntry> result = uniProtService.getEntries(query);
                entry = result.getFirstResult();
            }
            if (entry == null) {
                logger.warn("UniProt Entry " + accession + " could not be retrieved");
            } else {
                logger.debug("Retrieved UniProtEntry " + accession);
            }
        } catch (Exception e) {
            logger.error("Problems retrieving uniprot entry.", e);
            e.printStackTrace();
        } finally {
            // always remember to stop service
            uniProtService.stop();
        }
        return entry;
    }

}
