package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompSBasePlugin;
import org.sbml.jsbml.ext.comp.ReplacedBy;
import org.sbml.jsbml.ext.comp.ReplacedElement;

class MappingUtilTest {

    @Test
    void uniqueMetaIdGetsTheNextFreeNumber() {
        SBMLDocument document = new SBMLDocument(3, 1);
        Model model = document.createModel("m");
        model.createParameter("x").setMetaId("x");
        model.createParameter("x_1").setMetaId("x0");

        assertEquals("x1", MappingUtil.createUniqueMetaId(document, "x"));
        assertEquals("y", MappingUtil.createUniqueMetaId(document, "y"));
    }

    /** Elements without id, like replaced elements, get a metaid from their parent and type. */
    @Test
    void replacementsGetMetaIdFromTheirParent() {
        SBMLDocument document = new SBMLDocument(3, 1);
        document.enablePackage(CompConstants.shortLabel);
        Model model = document.createModel("m");
        Parameter parameter = model.createParameter("p");
        CompSBasePlugin plugin = (CompSBasePlugin) parameter.getPlugin(CompConstants.shortLabel);
        ReplacedElement first = plugin.createReplacedElement();
        ReplacedElement second = plugin.createReplacedElement();
        ReplacedBy replacedBy = plugin.createReplacedBy();

        MappingUtil.setSBaseMetaId(document, first);
        MappingUtil.setSBaseMetaId(document, second);
        MappingUtil.setSBaseMetaId(document, replacedBy);

        assertEquals("p_replacedElement", first.getMetaId());
        assertEquals("p_replacedElement0", second.getMetaId());
        assertEquals("p_replacedBy", replacedBy.getMetaId());
    }
}
