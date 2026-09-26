package org.cy3sbml.miriam;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the identifiers.org resource URI helpers in {@link RegistryUtil}. These replace
 * org.identifiers.registry.RegistryUtilities (registry-lib), which is only published on the
 * EBI Maven repository.
 */
public class RegistryUtilUriTest {

    @Test
    public void getIdentifierFromURI_legacyUrl() {
        assertEquals("CHEBI:36927",
                RegistryUtil.getIdentifierFromURI("http://identifiers.org/chebi/CHEBI:36927"));
    }

    @Test
    public void getIdentifierFromURI_urn() {
        assertEquals("CHEBI:36927",
                RegistryUtil.getIdentifierFromURI("urn:miriam:chebi:CHEBI:36927"));
    }

    @Test
    public void getIdentifierFromURI_withQueryString() {
        assertEquals("36927",
                RegistryUtil.getIdentifierFromURI("http://identifiers.org/chebi/36927?redirect=true"));
    }

    @Test
    public void getIdentifierFromURI_null() {
        assertNull(RegistryUtil.getIdentifierFromURI(null));
    }

    @Test
    public void getDataCollectionPartFromURI_legacyUrl() {
        assertEquals("http://identifiers.org/chebi",
                RegistryUtil.getDataCollectionPartFromURI("http://identifiers.org/chebi/CHEBI:36927"));
    }

    @Test
    public void getDataCollectionPartFromURI_urn() {
        assertEquals("urn:miriam:chebi",
                RegistryUtil.getDataCollectionPartFromURI("urn:miriam:chebi:CHEBI:36927"));
    }

    @Test
    public void getNamespaceFromURI_legacyUrl() {
        assertEquals("chebi",
                RegistryUtil.getNamespaceFromURI("http://identifiers.org/chebi/CHEBI:36927"));
    }

    @Test
    public void getNamespaceFromURI_compactUrl() {
        assertEquals("chebi",
                RegistryUtil.getNamespaceFromURI("https://identifiers.org/chebi:CHEBI:36927"));
    }

    @Test
    public void getNamespaceFromURI_urn() {
        assertEquals("chebi",
                RegistryUtil.getNamespaceFromURI("urn:miriam:chebi:CHEBI:36927"));
    }

    @Test
    public void checkRegexp_matches() {
        assertTrue(RegistryUtil.checkRegexp("CHEBI:36927", "^CHEBI:\\d+$"));
    }

    @Test
    public void checkRegexp_doesNotMatch() {
        assertFalse(RegistryUtil.checkRegexp("not-an-id", "^CHEBI:\\d+$"));
    }

    @Test
    public void checkRegexp_nullArguments() {
        assertFalse(RegistryUtil.checkRegexp(null, "^CHEBI:\\d+$"));
        assertFalse(RegistryUtil.checkRegexp("CHEBI:36927", null));
    }
}
