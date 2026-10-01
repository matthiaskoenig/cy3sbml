package org.cy3sbml.sbml4humans;

import java.util.Properties;
import org.cytoscape.property.CyProperty;

/**
 * Whether the user agreed to upload models to sbml4humans without being asked: the upload is
 * public (anyone with the link can open the report for 24 hours), so cy3sbml asks before the
 * first upload, and the answer "Don't ask again" is stored in the cy3sbml properties as
 * {@value #PROPERTY_CONFIRMED}.
 */
public final class Sbml4HumansConsent {
    /** The property of the consent, {@code true} after "Don't ask again". */
    public static final String PROPERTY_CONFIRMED = "cy3sbml.sbml4humans.confirmed";

    private final CyProperty<Properties> properties;

    public Sbml4HumansConsent(CyProperty<Properties> properties) {
        this.properties = properties;
    }

    /** Whether uploads are confirmed, so no dialog is shown. */
    public boolean isConfirmed() {
        return "true".equals(properties.getProperties().getProperty(PROPERTY_CONFIRMED));
    }

    /** Stores that uploads are confirmed. */
    public void confirm() {
        properties.getProperties().setProperty(PROPERTY_CONFIRMED, "true");
    }
}
