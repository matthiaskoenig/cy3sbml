package org.cy3sbml.util;

import java.util.Properties;
import org.cy3sbml.miriam.RegistryUtil;
import org.sbml.jsbml.Annotation;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.SBase;

/**
 * Tools for working with annotations.
 */
public class AnnotationUtil {

    /**
     * The identifiers of the resources of the CV terms by the namespace of their data
     * collection, e.g. {@code chebi -> CHEBI:17234}; of several resources of a collection
     * the last one.
     *
     * @param sbase the SBase
     * @return the identifiers by namespace, empty without annotation
     */
    public static Properties parseCVTerms(SBase sbase) {
        Properties props = new Properties();

        if (sbase.isSetAnnotation()) {
            Annotation annotation = sbase.getAnnotation();
            for (CVTerm cvterm : annotation.getListOfCVTerms()) {
                // add property for every cvterm
                for (String resourceURI : cvterm.getResources()) {
                    String namespace = RegistryUtil.getNamespaceFromURI(resourceURI);
                    String identifier = RegistryUtil.getIdentifierFromURI(resourceURI);

                    // Store under the namespace of the data collection
                    if (namespace != null && identifier != null) {
                        props.setProperty(namespace, identifier);
                    }
                }
            }
        }

        return props;
    }
}
