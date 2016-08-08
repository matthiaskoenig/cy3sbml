package org.cy3sbml.uniprot;

import org.junit.Test;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.client.Client;
import uk.ac.ebi.uniprot.dataservice.client.ServiceFactory;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtQueryBuilder;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtService;
import uk.ac.ebi.uniprot.dataservice.query.Query;

import static org.junit.Assert.*;
import static uk.ac.ebi.uniprot.dataservice.client.examples.UniProtRetrievalExamples.*;

/**
 * Test access to UniProt via JAPI.
 */
public class UniprotAccessTest {

    @Test
    public void getEntryByAccession(){
        String accession = "P10415";
        UniProtEntry entry = UniprotAccess.getUniProtEntry(accession);
        assertNotNull(entry);
        assertEquals("BCL2_HUMAN", entry.getUniProtId().toString());
    }

    @Test
    public void uniprotAccess(){
        ServiceFactory serviceFactoryInstance = Client.getServiceFactoryInstance();
        UniProtService uniProtService = serviceFactoryInstance.getUniProtQueryService();
        try {
            // start the service
            uniProtService.start();

            // the accession we're interested in
            String accession = "P10415";

            // use the service directly to fetch the UniProtEntry
            accessSingleFullUniProtEntry(uniProtService, accession);

            // create a query that will satisfy the 1 document result,
            // i.e., the one for entry P10144
            Query query = UniProtQueryBuilder.accession(accession);

            // use the service with the query to entries
            accessMultiFullUniProtEntry(uniProtService, query);

            // use the service with the query to access only its comments
            accessCommentsOnly(uniProtService, query);

            // use the service with the query to access only its features
            accessFeaturesOnly(uniProtService, query);

            // use the service with the query to access only its protein names
            accessProteinNamesOnly(uniProtService, query);

            // use the service with the query to access only its EC numbers
            accessECsOnly(uniProtService, query);

            // use the service with the query to access only its genes
            accessGenesOnly(uniProtService, query);

            // use the service with the query to access only its database cross-references
            accessXrefsOnly(uniProtService, query);

            // use the service with the query to access results with all components
            accessResults(uniProtService, query);

            // use the service simply find out numbers of hits of a query
            showResultHits(uniProtService);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // always remember to stop the service
            uniProtService.stop();
            System.out.println("service now stopped.");
        }
    }

}