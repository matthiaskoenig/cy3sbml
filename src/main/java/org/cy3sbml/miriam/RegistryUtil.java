package org.cy3sbml.miriam;

import org.cy3sbml.ols.OLSAccess;
import org.cy3sbml.util.IOUtil;
import org.identifiers.registry.RegistryDatabase;
import org.identifiers.registry.data.PhysicalLocation;

import java.io.InputStream;

/**
 * Tools for working with Miriam registry.
 * http://www.ebi.ac.uk/miriam/main/export/
 *
 * TODO: implement update of registry file
 */
public class RegistryUtil {

    /** Load the registry from the resources. */
    public static void loadRegistry() {
        InputStream miriamStream = IOUtil.readResource("/miriam/IdentifiersOrg-Registry.xml");
        RegistryDatabase.loadFromInputStream(miriamStream);
    }

    /**
     * Is a given location a OLS location, i.e. an ontology in OLS.
     */
    public static boolean isPhysicalLocationOLS(PhysicalLocation location){
        return location.getUrlRoot().startsWith(OLSAccess.OLS_BASE_URL);
    }


}
