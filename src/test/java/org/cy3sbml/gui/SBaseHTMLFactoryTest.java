package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.cy3sbml.chebi.ChebiAccess;
import org.cy3sbml.miriam.MiriamRegistry;
import org.cy3sbml.ols.OlsClient;
import org.cy3sbml.uniprot.UniprotAccess;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Creator;
import org.sbml.jsbml.History;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.util.filters.Filter;
import org.sbml.jsbml.xml.XMLAttributes;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.jsbml.xml.XMLTriple;

class SBaseHTMLFactoryTest {

    @Test
    void htmlTextHasBaseDirAndTitle() {
        SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory("file:///app/gui/", null, null, null, null);

        String html = htmlFactory.createHTMLText("<p>text</p>", "Title");

        assertTrue(html.contains("<base href=\"file:///app/gui/\" />"), html);
        assertTrue(html.contains("<title>Title</title>"), html);
        assertTrue(html.contains("<p>text</p>"), html);
    }

    @Test
    void renderingShowsTheSboTermWithoutChangingTheDocument() throws Exception {
        SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory(
                "file:///app/gui/",
                MiriamRegistry.bundled(),
                mock(OlsClient.class),
                mock(UniprotAccess.class),
                mock(ChebiAccess.class));
        Species species = new Species("s1", 3, 1);
        species.setSBOTerm(247);

        String html = htmlFactory.createInfo(species);

        // the SBO term is shown as an annotation, but not added to the model
        assertEquals(0, species.getCVTermCount());
        assertTrue(html.contains("SBO:0000247"), html);
    }

    /**
     * Every row of the attribute table has two cells, the name and the value: a third (empty)
     * cell takes a share of the fixed table layout and squeezes the values.
     */
    @Test
    void attributeRowsHaveTwoCells() throws Exception {
        SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory(
                "file:///app/gui/",
                MiriamRegistry.bundled(),
                mock(OlsClient.class),
                mock(UniprotAccess.class),
                mock(ChebiAccess.class));
        Species species = new Species("s1", "species 1", 3, 1);

        String html = htmlFactory.createInfo(species);

        assertFalse(html.contains("<td/>"), html);
        String[] rows = html.split("<tr>", -1);
        assertTrue(rows.length > 1, html);
        for (int k = 1; k < rows.length; k++) {
            String row = rows[k].substring(0, rows[k].indexOf("</tr>"));
            assertEquals(2, row.split("<td>", -1).length - 1, row);
        }
    }

    /**
     * CVTerms with urn:miriam resource URNs (e.g. "urn:miriam:obo.chebi:CHEBI%3A15422") are
     * shown like identifiers.org URIs, linked to the resources of their data collection.
     */
    @Test
    void urnMiriamResourcesAreShown() throws Exception {
        SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory(
                "file:///app/gui/",
                MiriamRegistry.bundled(),
                mock(OlsClient.class),
                mock(UniprotAccess.class),
                mock(ChebiAccess.class));
        Species species = new Species("atp", 3, 1);
        species.setMetaId("meta_atp");
        species.addCVTerm(new CVTerm(
                CVTerm.Qualifier.BQB_IS, "urn:miriam:kegg.compound:C00002", "urn:miriam:obo.chebi:CHEBI%3A15422"));

        String html = htmlFactory.createInfo(species);

        assertTrue(html.contains("https://www.kegg.jp/entry/C00002"), html);
        assertTrue(html.contains("https://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI:15422"), html);
        assertFalse(html.contains("Unknown data collection"), html);
    }

    /**
     * Links go to a resource that is not deprecated, preferring the official one: the first
     * SBO resource in the registry is deprecated and only leads to a deprecation page.
     */
    @Test
    void linksSkipDeprecatedResources() throws Exception {
        SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory(
                "file:///app/gui/",
                MiriamRegistry.bundled(),
                mock(OlsClient.class),
                mock(UniprotAccess.class),
                mock(ChebiAccess.class));
        Species species = new Species("s1", 3, 1);
        species.setSBOTerm(247);

        String html = htmlFactory.createInfo(species);

        assertFalse(html.contains("registry.identifiers.org/deprecation"), html);
        assertFalse(html.contains("https://www.ebi.ac.uk/sbo/"), html);
        assertTrue(html.contains("https://www.ebi.ac.uk/ols4/ontologies/sbo/terms?obo_id=SBO:0000247"), html);
    }

    /**
     * The compact identifiers of the example model (e.g. "BAO:0000362", "DOI:10.1016/...",
     * "GO:0007049") resolve to their data collections and match their patterns (#394).
     */
    @Test
    void compactIdentifiersResolveToTheirDataCollections() throws Exception {
        SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory(
                "file:///app/gui/",
                MiriamRegistry.bundled(),
                mock(OlsClient.class),
                mock(UniprotAccess.class),
                mock(ChebiAccess.class));
        SBMLDocument document;
        try (InputStream in = getClass().getResourceAsStream("/models/Faure2006_MammalianCellCycle.sbml")) {
            document = SBMLReader.read(in);
        }
        List<SBase> annotated = new ArrayList<>();
        Filter hasCVTerms = o -> o instanceof SBase sbase && sbase.getCVTermCount() > 0;
        for (Object o : document.filter(hasCVTerms)) {
            annotated.add((SBase) o);
        }
        assertFalse(annotated.isEmpty());

        StringBuilder html = new StringBuilder();
        for (SBase sbase : annotated) {
            html.append(htmlFactory.createInfo(sbase));
        }

        assertFalse(html.toString().contains("does not match pattern"), html.toString());
        assertFalse(html.toString().contains("Unknown data collection"), html.toString());
        assertTrue(html.toString().contains("https://doi.org/10.1016/j.jtbi.2004.04.039"), html.toString());
    }

    @Test
    void nonRdfAnnotationShowsNonRdfElements() throws Exception {
        Species species = new Species("s1", 3, 1);
        species.getAnnotation()
                .appendNonRDFAnnotation("<sabio xmlns=\"http://sabiork.h-its.org\">kinetic data</sabio>");

        String html = SBaseHTMLFactory.createNonRDFAnnotation(species);

        assertTrue(html.contains("kinetic data"), html);
    }

    @Test
    void nonRdfAnnotationSkipsRdfElement() throws Exception {
        // element names are compared by value, not by reference: the name is a new String
        XMLNode rdf = new XMLNode(
                new XMLTriple(new String("RDF"), "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf"),
                new XMLAttributes());
        rdf.addChild(new XMLNode("rdf content"));
        XMLNode annotation = new XMLNode(new XMLTriple("annotation", "", ""), new XMLAttributes());
        annotation.addChild(rdf);
        Species species = new Species("s1", 3, 1);
        species.getAnnotation().setNonRDFAnnotation(annotation);

        String html = SBaseHTMLFactory.createNonRDFAnnotation(species);

        assertFalse(html.contains("rdf content"), html);
    }

    @Test
    void ontologyTextKeepsInlineFormatting() {
        assertEquals(
                "<small>D</small>-galactose, H<sub>2</sub>O",
                SBaseHTMLFactory.ontologyTextHTML("<small>D</small>-galactose, H<sub>2</sub>O"));
    }

    @Test
    void ontologyTextEscapesOtherMarkup() {
        assertEquals(
                "&lt;script&gt;x&lt;/script&gt; a &amp; b &lt;sub onclick=&quot;x&quot;&gt;",
                SBaseHTMLFactory.ontologyTextHTML("<script>x</script> a & b <sub onclick=\"x\">"));
    }

    @Test
    void ontologyTextKeepsNestedPairs() {
        assertEquals("<b><i>x</i></b> y", SBaseHTMLFactory.ontologyTextHTML("<b><i>x</i></b> y"));
    }

    @Test
    void ontologyTextEscapesUnbalancedTags() {
        assertEquals("H&lt;sub&gt;2 O", SBaseHTMLFactory.ontologyTextHTML("H<sub>2 O"));
        assertEquals("x&lt;/i&gt; y", SBaseHTMLFactory.ontologyTextHTML("x</i> y"));
        assertEquals("&lt;b&gt;&lt;i&gt;x&lt;/b&gt;&lt;/i&gt;", SBaseHTMLFactory.ontologyTextHTML("<b><i>x</b></i>"));
    }

    /**
     * A creator with only an organisation is shown without the separator of a missing name.
     */
    @Test
    void historyShowsCreatorWithOnlyOrganisation() throws Exception {
        SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory(
                "file:///app/gui/",
                MiriamRegistry.bundled(),
                mock(OlsClient.class),
                mock(UniprotAccess.class),
                mock(ChebiAccess.class));
        Model model = new Model("m", 3, 1);
        model.setMetaId("meta_m");
        History history = new History();
        Creator creator = new Creator();
        creator.setOrganisation("ZBIT");
        history.addCreator(creator);
        model.setHistory(history);

        String html = htmlFactory.createInfo(model);

        assertTrue(html.contains("<p class=\"cvterm\">ZBIT<br />"), html);
    }

    /**
     * The creators of a model history in vCard4 (written e.g. by sbmlutils) are shown (#397).
     */
    @Test
    void historyShowsVCard4Creators() throws Exception {
        SBaseHTMLFactory htmlFactory = new SBaseHTMLFactory(
                "file:///app/gui/",
                MiriamRegistry.bundled(),
                mock(OlsClient.class),
                mock(UniprotAccess.class),
                mock(ChebiAccess.class));
        String sbml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <sbml xmlns="http://www.sbml.org/sbml/level3/version2/core" level="3" version="2">
                  <model metaid="meta_m" id="m">
                    <annotation>
                      <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                          xmlns:dcterms="http://purl.org/dc/terms/"
                          xmlns:vCard4="http://www.w3.org/2006/vcard/ns#">
                        <rdf:Description rdf:about="#meta_m">
                          <dcterms:creator>
                            <rdf:Bag>
                              <rdf:li rdf:parseType="Resource">
                                <vCard4:hasName rdf:parseType="Resource">
                                  <vCard4:family-name>König</vCard4:family-name>
                                  <vCard4:given-name>Matthias</vCard4:given-name>
                                </vCard4:hasName>
                                <vCard4:hasEmail>koenigmx@hu-berlin.de</vCard4:hasEmail>
                                <vCard4:organization-name>Humboldt &amp; University</vCard4:organization-name>
                              </rdf:li>
                            </rdf:Bag>
                          </dcterms:creator>
                          <dcterms:created rdf:parseType="Resource">
                            <dcterms:W3CDTF>2024-05-06T07:08:09Z</dcterms:W3CDTF>
                          </dcterms:created>
                          <dcterms:modified rdf:parseType="Resource">
                            <dcterms:W3CDTF>2025-01-02T03:04:05Z</dcterms:W3CDTF>
                          </dcterms:modified>
                        </rdf:Description>
                      </rdf:RDF>
                    </annotation>
                  </model>
                </sbml>
                """;
        SBMLDocument document = SBMLReader.read(sbml);

        String html = htmlFactory.createInfo(document);

        assertTrue(
                html.contains("Matthias König (<a href=\"mailto:koenigmx@hu-berlin.de\">koenigmx@hu-berlin.de</a>),"
                        + " Humboldt &amp; University<br />"),
                html);
        assertTrue(html.contains("created: 2024-05-06T07:08:09Z"), html);
        assertTrue(html.contains("modified: 2025-01-02T03:04:05Z"), html);
    }

    /**
     * Booleans are shown as a green check and a red cross, inline SVG icons which need no
     * icon font (#440).
     */
    @Test
    void booleansAreInlineSvgIcons() {
        String trueHtml = SBaseHTMLFactory.booleanHTML(true);
        String falseHtml = SBaseHTMLFactory.booleanHTML(false);

        assertTrue(trueHtml.startsWith("<svg class=\"icon icon-true\""), trueHtml);
        assertTrue(trueHtml.contains("<title>true</title>"), trueHtml);
        assertTrue(falseHtml.startsWith("<svg class=\"icon icon-false\""), falseHtml);
        assertTrue(falseHtml.contains("<title>false</title>"), falseHtml);
    }
}
