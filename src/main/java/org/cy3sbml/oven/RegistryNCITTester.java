package org.cy3sbml.oven;

import org.cy3sbml.miriam.RegistryUtil;
import org.identifiers.registry.RegistryDatabase;
import org.identifiers.registry.RegistryUtilities;
import org.identifiers.registry.data.DataType;

import java.io.File;
import java.io.FileNotFoundException;

public class RegistryNCITTester {


    public static void main(String[] args) throws FileNotFoundException {
        File miriamFile = new File("/home/mkoenig/git/cy3sbml/src/main/resources/miriam/IdentifiersOrg-Registry.xml");
        //updateMiriamXML(miriamFile);
        RegistryUtil.loadRegistry(miriamFile);
        System.out.println("Hello world");

        String resourceURI = "http://identifiers.org/ncit/C94552";

        String identifier = RegistryUtilities.getIdentifierFromURI(resourceURI);
        String dataCollection = RegistryUtilities.getDataCollectionPartFromURI(resourceURI);
        DataType dataType = RegistryDatabase.getInstance().getDataTypeByURI(dataCollection);
        System.out.println(dataType);
    }

}
