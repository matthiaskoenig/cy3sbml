package org.cy3sbml.ols;



import uk.ac.ebi.pride.utilities.ols.web.service.client.OLSClient;
import uk.ac.ebi.pride.utilities.ols.web.service.config.OLSWsConfigProd;


public class OLSResource {

    private static OLSClient olsClient = new OLSClient(new OLSWsConfigProd());


}
