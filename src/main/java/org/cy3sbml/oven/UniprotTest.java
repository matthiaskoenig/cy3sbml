package org.cy3sbml.oven;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.client.Client;
import uk.ac.ebi.uniprot.dataservice.client.ServiceFactory;
import uk.ac.ebi.uniprot.dataservice.client.exception.ServiceException;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtService;

public class UniprotTest {

    public static void main(String[] args) throws ServiceException {
        ServiceFactory serviceFactoryInstance = Client.getServiceFactoryInstance();
        UniProtService uniProtService = serviceFactoryInstance.getUniProtQueryService();
        UniProtEntry entry = null;

            // start the service
            uniProtService.start();

            // fetch entry
            entry = uniProtService.getEntry("P19367");
    }
}
