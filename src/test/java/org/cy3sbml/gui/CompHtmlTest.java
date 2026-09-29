package org.cy3sbml.gui;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.util.Optional;
import org.cy3sbml.comp.CompModels;
import org.cy3sbml.comp.CompTargets;
import org.cy3sbml.comp.SBaseRefResolver;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompModelPlugin;
import org.sbml.jsbml.ext.comp.CompSBasePlugin;
import org.sbml.jsbml.ext.comp.Submodel;

/** The info panel HTML of the comp elements. */
class CompHtmlTest {

    private static SBMLDocument read(String resource) throws Exception {
        URL url = CompHtmlTest.class.getResource(resource);
        SBMLDocument document = SBMLReader.read(url.openStream());
        document.setLocationURI(url.toURI().toString());
        return document;
    }

    /** The network collection of the model definition mdA has the root SUID 42, other models have none. */
    private static final long MDA_ROOT = 42L;

    private static String info(SBMLDocument document, SBase sbase) throws Exception {
        SBaseRefResolver resolver = new SBaseRefResolver(new CompModels(document));
        CompTargets targets = new CompTargets() {
            @Override
            public SBaseRefResolver resolver(SBase s) {
                return resolver;
            }

            @Override
            public Optional<Long> rootNetwork(Model model) {
                return "mdA".equals(model.getId()) ? Optional.of(MDA_ROOT) : Optional.empty();
            }
        };
        return new SBaseHTMLFactory("file:///app/gui/", null, null, null, null, targets).createInfo(sbase);
    }

    private static Submodel submodel(SBMLDocument document, String id) {
        return ((CompModelPlugin) document.getModel().getExtension(CompConstants.shortLabel)).getSubmodel(id);
    }

    @Test
    void replacedElementShowsItsAttributesAndLinksItsTarget() throws Exception {
        SBMLDocument document = read("/models/comp/unit/references.xml");
        CompSBasePlugin plugin =
                (CompSBasePlugin) document.getModel().getSpecies("S2").getExtension(CompConstants.shortLabel);

        String html = info(document, plugin.getReplacedElement(0));

        assertTrue(html.contains("submodelRef=A &gt; idRef=x2"), html);
        assertTrue(html.contains(">cf<"), html);
        assertTrue(html.contains(BrowserHyperlinkListener.URL_SELECT_TARGET + MDA_ROOT + "/x2"), html);
    }

    @Test
    void submodelLinksTheNetworkOfItsModel() throws Exception {
        SBMLDocument document = read("/models/comp/unit/references.xml");

        String html = info(document, submodel(document, "A"));

        assertTrue(html.contains(">mdA<") || html.contains("mdA"), html);
        assertTrue(html.contains(BrowserHyperlinkListener.URL_SELECT_TARGET + MDA_ROOT + "/\""), html);
    }

    /** A target in a model without network is shown without link. */
    @Test
    void targetWithoutNetworkHasNoLink() throws Exception {
        SBMLDocument document = read("/models/comp/unit/references.xml");
        CompSBasePlugin plugin =
                (CompSBasePlugin) document.getModel().getParameter("q").getExtension(CompConstants.shortLabel);

        String html = info(document, plugin.getReplacedBy());

        assertTrue(html.contains("mdB"), html);
        assertTrue(!html.contains(BrowserHyperlinkListener.URL_SELECT_TARGET), html);
    }

    @Test
    void unresolvedDeletionShowsTheReason() throws Exception {
        SBMLDocument document = read("/models/comp/unit/references.xml");

        String html =
                info(document, submodel(document, "A").getListOfDeletions().get("del_missing"));

        assertTrue(
                html.contains("There is no element &#39;missing&#39;")
                        || html.contains("There is no element 'missing'"),
                html);
    }

    @Test
    void documentListsTheExternalModelDefinitionsWithTheirStatus() throws Exception {
        SBMLDocument document = read("/models/comp/unit/top.xml");

        String html = info(document, document);

        assertTrue(html.contains("sub/ext2.xml"), html);
        assertTrue(html.contains("missing.xml does not exist"), html);
    }
}
