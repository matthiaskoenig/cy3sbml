package org.cy3sbml.miriam;

import org.cy3sbml.util.IOUtil;
import org.identifiers.registry.RegistryDatabase;

import java.io.InputStream;

/**
 * Tools for working with Miriam registry.
 */
public class RegistryUtil {

    /** Load the registry from the resources. */
    public static void loadRegistry() {
        InputStream miriamStream = IOUtil.readResource("/miriam/IdentifiersOrg-Registry.xml");
        RegistryDatabase.loadFromInputStream(miriamStream);
    }


}
