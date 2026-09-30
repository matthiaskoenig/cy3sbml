package org.cy3sbml.miriam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the identifiers.org resource URI helpers in {@link RegistryUtil}. These replace
 * org.identifiers.registry.RegistryUtilities (registry-lib), which is only published on the
 * EBI Maven repository.
 * <p>
 * Covers the identifiers.org URI forms actually seen in SBML annotations:
 * <ul>
 *     <li>legacy namespace/id form: {@code http://identifiers.org/<namespace>/<accession>},
 *         where the accession may contain slashes</li>
 *     <li>compact form: {@code https://identifiers.org/<namespace>:<accession>}, where
 *         {@code <namespace>} is lowercase and the accession may contain slashes</li>
 *     <li>a provider-less compact URI whose single path segment is itself the accession, and
 *         that accession happens to embed a colon (e.g. GO's own accessions look like
 *         {@code GO:0042752}); since the segment's prefix before the colon is not lowercase,
 *         this is not treated as a genuine {@code <namespace>:<accession>} split</li>
 *     <li>{@code urn:miriam:<namespace>:<accession>} URNs</li>
 * </ul>
 * No registry lookup is used or needed: a namespace prefix is recognized purely by position
 * (before the first path separator, or before the first colon in a single-segment URI) and,
 * for the single-segment case, by being lowercase (the identifiers.org convention).
 */
public class RegistryUtilUriTest {

    // -- getIdentifierFromURI ------------------------------------------------------------------

    @Test
    public void getIdentifierFromURI_legacyForm() {
        // namespace/accession, two path segments
        assertEquals("36927", RegistryUtil.getIdentifierFromURI("http://identifiers.org/chebi/36927"));
    }

    @Test
    public void getIdentifierFromURI_legacyFormWithColonInAccession() {
        // the accession itself contains a colon (GO's own accession syntax)
        assertEquals("GO:0042752", RegistryUtil.getIdentifierFromURI("http://identifiers.org/go/GO:0042752"));
    }

    @Test
    public void getIdentifierFromURI_compactForm() {
        // single path segment "chebi:CHEBI:36927": lowercase "chebi" is the namespace,
        // "CHEBI:36927" (itself containing a colon) is the accession
        assertEquals("CHEBI:36927", RegistryUtil.getIdentifierFromURI("https://identifiers.org/chebi:CHEBI:36927"));
    }

    @Test
    public void getIdentifierFromURI_embeddedPrefixForm() {
        // single path segment "GO:0042752": "GO" is not lowercase, so this is not a genuine
        // namespace:accession split -- the whole segment is the accession
        assertEquals("GO:0042752", RegistryUtil.getIdentifierFromURI("https://identifiers.org/GO:0042752"));
    }

    @Test
    public void getIdentifierFromURI_legacyFormWithSlashInAccession() {
        assertEquals(
                "10.1016/j.jtbi.2004.04.039",
                RegistryUtil.getIdentifierFromURI("https://identifiers.org/doi/10.1016/j.jtbi.2004.04.039"));
    }

    @Test
    public void getIdentifierFromURI_compactFormWithSlashInAccession() {
        assertEquals(
                "10.1016/j.jtbi.2004.04.039",
                RegistryUtil.getIdentifierFromURI("https://identifiers.org/doi:10.1016/j.jtbi.2004.04.039"));
        assertEquals(
                "DOI:10.1016/j.jtbi.2004.04.039",
                RegistryUtil.getIdentifierFromURI("https://identifiers.org/DOI:10.1016/j.jtbi.2004.04.039"));
    }

    @Test
    public void getIdentifierFromURI_urnForm() {
        assertEquals("CHEBI:36927", RegistryUtil.getIdentifierFromURI("urn:miriam:chebi:CHEBI:36927"));
    }

    @Test
    public void getIdentifierFromURI_urnFormWithColonInAccession() {
        assertEquals("GO:0042752", RegistryUtil.getIdentifierFromURI("urn:miriam:go:GO:0042752"));
    }

    @Test
    public void getIdentifierFromURI_httpVsHttps() {
        String viaHttp = RegistryUtil.getIdentifierFromURI("http://identifiers.org/chebi:CHEBI:36927");
        String viaHttps = RegistryUtil.getIdentifierFromURI("https://identifiers.org/chebi:CHEBI:36927");
        assertEquals("CHEBI:36927", viaHttp);
        assertEquals(viaHttp, viaHttps);
    }

    @Test
    public void getIdentifierFromURI_trailingSlash() {
        String withoutSlash = RegistryUtil.getIdentifierFromURI("https://identifiers.org/chebi/CHEBI:36927");
        String withSlash = RegistryUtil.getIdentifierFromURI("https://identifiers.org/chebi/CHEBI:36927/");
        assertEquals("CHEBI:36927", withoutSlash);
        assertEquals(withoutSlash, withSlash);
    }

    @Test
    public void getIdentifierFromURI_compactFormTrailingSlash() {
        String withoutSlash = RegistryUtil.getIdentifierFromURI("https://identifiers.org/chebi:CHEBI:36927");
        String withSlash = RegistryUtil.getIdentifierFromURI("https://identifiers.org/chebi:CHEBI:36927/");
        assertEquals("CHEBI:36927", withoutSlash);
        assertEquals(withoutSlash, withSlash);
    }

    @Test
    public void getIdentifierFromURI_withQueryString() {
        assertEquals("36927", RegistryUtil.getIdentifierFromURI("http://identifiers.org/chebi/36927?redirect=true"));
    }

    @Test
    public void getIdentifierFromURI_null() {
        assertNull(RegistryUtil.getIdentifierFromURI(null));
    }

    // -- getNamespaceFromURI -------------------------------------------------------------------

    @Test
    public void getNamespaceFromURI_legacyForm() {
        assertEquals("chebi", RegistryUtil.getNamespaceFromURI("http://identifiers.org/chebi/CHEBI:36927"));
    }

    @Test
    public void getNamespaceFromURI_legacyFormWithColonInAccession() {
        assertEquals("go", RegistryUtil.getNamespaceFromURI("http://identifiers.org/go/GO:0042752"));
    }

    @Test
    public void getNamespaceFromURI_compactForm() {
        assertEquals("chebi", RegistryUtil.getNamespaceFromURI("https://identifiers.org/chebi:CHEBI:36927"));
    }

    @Test
    public void getNamespaceFromURI_embeddedPrefixForm() {
        assertEquals("GO", RegistryUtil.getNamespaceFromURI("https://identifiers.org/GO:0042752"));
    }

    @Test
    public void getNamespaceFromURI_compactFormWithSlashInAccession() {
        assertEquals("DOI", RegistryUtil.getNamespaceFromURI("https://identifiers.org/DOI:10.1016/j.jtbi.2004.04.039"));
    }

    @Test
    public void getNamespaceFromURI_urnForm() {
        assertEquals("chebi", RegistryUtil.getNamespaceFromURI("urn:miriam:chebi:CHEBI:36927"));
    }

    @Test
    public void getNamespaceFromURI_httpVsHttps() {
        assertEquals(
                RegistryUtil.getNamespaceFromURI("http://identifiers.org/chebi:CHEBI:36927"),
                RegistryUtil.getNamespaceFromURI("https://identifiers.org/chebi:CHEBI:36927"));
    }

    @Test
    public void getNamespaceFromURI_trailingSlash() {
        assertEquals(
                RegistryUtil.getNamespaceFromURI("https://identifiers.org/chebi/CHEBI:36927"),
                RegistryUtil.getNamespaceFromURI("https://identifiers.org/chebi/CHEBI:36927/"));
    }

    // -- getDataCollectionPartFromURI ----------------------------------------------------------

    @Test
    public void getDataCollectionPartFromURI_legacyForm() {
        // registry-lib kept the trailing slash on the data collection part
        assertEquals(
                "http://identifiers.org/chebi/",
                RegistryUtil.getDataCollectionPartFromURI("http://identifiers.org/chebi/CHEBI:36927"));
    }

    @Test
    public void getDataCollectionPartFromURI_legacyFormWithColonInAccession() {
        assertEquals(
                "http://identifiers.org/go/",
                RegistryUtil.getDataCollectionPartFromURI("http://identifiers.org/go/GO:0042752"));
    }

    @Test
    public void getDataCollectionPartFromURI_compactForm() {
        assertEquals(
                "https://identifiers.org/chebi/",
                RegistryUtil.getDataCollectionPartFromURI("https://identifiers.org/chebi:CHEBI:36927"));
    }

    @Test
    public void getDataCollectionPartFromURI_embeddedPrefixForm() {
        assertEquals(
                "https://identifiers.org/GO/",
                RegistryUtil.getDataCollectionPartFromURI("https://identifiers.org/GO:0042752"));
    }

    @Test
    public void getDataCollectionPartFromURI_urnForm() {
        assertEquals("urn:miriam:chebi", RegistryUtil.getDataCollectionPartFromURI("urn:miriam:chebi:CHEBI:36927"));
    }

    @Test
    public void getDataCollectionPartFromURI_httpVsHttps() {
        // the data collection part is a link back to the resource, so it keeps the scheme of
        // the URI it was extracted from (unlike the identifier and namespace parts)
        assertEquals(
                "http://identifiers.org/chebi/",
                RegistryUtil.getDataCollectionPartFromURI("http://identifiers.org/chebi/CHEBI:36927"));
        assertEquals(
                "https://identifiers.org/chebi/",
                RegistryUtil.getDataCollectionPartFromURI("https://identifiers.org/chebi/CHEBI:36927"));
    }

    @Test
    public void getDataCollectionPartFromURI_trailingSlash() {
        assertEquals(
                RegistryUtil.getDataCollectionPartFromURI("https://identifiers.org/chebi/CHEBI:36927"),
                RegistryUtil.getDataCollectionPartFromURI("https://identifiers.org/chebi/CHEBI:36927/"));
    }

    // -- checkRegexp ----------------------------------------------------------------------------

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

    @Test
    public void isIdentifiersURI() {
        assertTrue(RegistryUtil.isIdentifiersURI("http://identifiers.org/chebi/CHEBI:36927"));
        assertTrue(RegistryUtil.isIdentifiersURI("https://identifiers.org/chebi:CHEBI:36927"));
        assertTrue(RegistryUtil.isIdentifiersURI("urn:miriam:obo.chebi:CHEBI%3A36927"));
        assertFalse(RegistryUtil.isIdentifiersURI("https://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI:36927"));
        assertFalse(RegistryUtil.isIdentifiersURI(null));
    }

    @Test
    public void isIdentifiersURI_checksTheHost() {
        assertTrue(RegistryUtil.isIdentifiersURI("HTTPS://Identifiers.org/chebi/CHEBI:36927"));
        assertTrue(RegistryUtil.isIdentifiersURI("https://www.identifiers.org/chebi/CHEBI:36927"));
        assertTrue(RegistryUtil.isIdentifiersURI("http://identifiers.org:80/chebi/CHEBI:36927"));
        assertFalse(RegistryUtil.isIdentifiersURI("https://example.org/identifiers.org/chebi/CHEBI:36927"));
        assertFalse(RegistryUtil.isIdentifiersURI("https://notidentifiers.org/chebi/CHEBI:36927"));
        assertFalse(RegistryUtil.isIdentifiersURI("https://identifiers.org.example.org/chebi/CHEBI:36927"));
        assertFalse(RegistryUtil.isIdentifiersURI("https://example.org/?see=identifiers.org"));
        assertFalse(RegistryUtil.isIdentifiersURI("ftp://identifiers.org/chebi/CHEBI:36927"));
    }

    /** A plus sign is a literal character of the identifier, not an encoded space. */
    @Test
    public void getIdentifierFromURI_keepsAPlusSign() {
        assertEquals("CA+2", RegistryUtil.getIdentifierFromURI("http://identifiers.org/biocyc/CA+2"));
        assertEquals(
                "InChI=1S/Na/q+1", RegistryUtil.getIdentifierFromURI("https://identifiers.org/inchi:InChI=1S/Na/q+1"));
        assertEquals("InChI=1S/Na/q+1", RegistryUtil.getIdentifierFromURI("urn:miriam:inchi:InChI=1S/Na/q+1"));
        assertEquals("InChI=1S/Na/q+1", RegistryUtil.getIdentifierFromURI("urn:miriam:inchi:InChI%3D1S%2FNa%2Fq%2B1"));
    }
}
