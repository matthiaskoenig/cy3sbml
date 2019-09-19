package org.cy3sbml.oven;


import org.sbml.jsbml.*;
import org.sbml.jsbml.text.parser.ParseException;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;


public class InlineUnitsDropped {

    public static void main(String[] args) throws IOException, XMLStreamException, ParseException {

        // [1] To formula with inline units not working
        SBMLDocument doc1 = JSBML.readSBML("/home/mkoenig/Desktop/inline_units_py.xml");
        Model m1 = doc1.getModel();
        AssignmentRule r1 = m1.getAssignmentRuleByVariable("p");
        ASTNode a1 = r1.getMath();
        String formula = JSBML.formulaToString(a1);
        System.out.println(formula);


        // [2] Parsing formulas with inline units not working
        SBMLDocument doc = new SBMLDocument(3, 2);
        Model model = doc.createModel();
        model.setId("test_inline_unit");

        UnitDefinition ud = model.createUnitDefinition();
        ud.setId("m");
        Unit u = new Unit();
        u.setKind(Unit.Kind.METRE);
        u.setExponent(1.0);
        u.setScale(1);
        u.setMultiplier(1.0);

        ud.addUnit(u);


        Parameter p = model.createParameter("p");
        p.setConstant(false);
        p.setUnits("m");

        AssignmentRule rule = model.createAssignmentRule();
        rule.setVariable("p");
        ASTNode ast = JSBML.parseFormula("5.0 m");
        rule.setMath(ast);

        System.out.println(ast);

        JSBML.writeSBML(doc, "/home/mkoenig/Desktop/inline_units.xml");
    }
}
