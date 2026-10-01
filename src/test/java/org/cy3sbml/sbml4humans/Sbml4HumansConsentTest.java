package org.cy3sbml.sbml4humans;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;
import org.cytoscape.property.CyProperty;
import org.junit.jupiter.api.Test;

class Sbml4HumansConsentTest {

    @SuppressWarnings("unchecked")
    private static CyProperty<Properties> property(Properties properties) {
        CyProperty<Properties> property = mock(CyProperty.class);
        when(property.getProperties()).thenReturn(properties);
        return property;
    }

    @Test
    void notConfirmedWithoutProperty() {
        assertFalse(new Sbml4HumansConsent(property(new Properties())).isConfirmed());
    }

    @Test
    void confirmStoresTheProperty() {
        Properties properties = new Properties();
        Sbml4HumansConsent consent = new Sbml4HumansConsent(property(properties));

        consent.confirm();

        assertTrue(consent.isConfirmed());
        assertEquals("true", properties.getProperty(Sbml4HumansConsent.PROPERTY_CONFIRMED));
    }

    @Test
    void otherValuesAreNoConsent() {
        Properties properties = new Properties();
        properties.setProperty(Sbml4HumansConsent.PROPERTY_CONFIRMED, "yes");
        assertFalse(new Sbml4HumansConsent(property(properties)).isConfirmed());
    }
}
