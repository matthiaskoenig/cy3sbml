package org.cy3sbml.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sbml.jsbml.ASTNode;
import org.sbml.jsbml.InitialAssignment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;

class ASTNodeUtilTest {

    private InitialAssignment assignment;

    @BeforeEach
    void setUp() {
        Model model = new SBMLDocument(3, 1).createModel("m1");
        Parameter parameter = model.createParameter("p1");
        parameter.setConstant(true);
        assignment = model.createInitialAssignment();
        assignment.setVariable(parameter);
    }

    private ASTNode math(String formula) throws Exception {
        ASTNode astNode = ASTNode.parseFormula(formula);
        astNode.setParentSBMLObject(assignment);
        return astNode;
    }

    @Test
    void toFormulaWithoutUnits() throws Exception {
        ASTNode astNode = math("p1 * (1 + 0.2) / 3e-2");
        assertEquals(astNode.toFormula(), ASTNodeUtil.toFormula(astNode));
    }

    @Test
    void toFormulaIntegerUnits() throws Exception {
        ASTNode astNode = new ASTNode(10, "dimensionless", assignment);
        assertEquals("10 dimensionless", ASTNodeUtil.toFormula(astNode));
    }

    @Test
    void toFormulaRealUnits() throws Exception {
        ASTNode astNode = new ASTNode(0.2, assignment);
        astNode.setUnits("mole");
        assertEquals("0.2 mole", ASTNodeUtil.toFormula(astNode));
    }

    @Test
    void toFormulaExponentUnits() throws Exception {
        ASTNode astNode = new ASTNode(1.5, -3, assignment);
        astNode.setUnits("litre");
        assertEquals("1.5E-3 litre", ASTNodeUtil.toFormula(astNode));
    }

    @Test
    void toFormulaNestedUnits() throws Exception {
        ASTNode astNode = math("p1 / (1 + p1)");
        astNode.getRightChild().getLeftChild().setUnits("dimensionless");
        assertEquals("p1/(1 dimensionless+p1)", ASTNodeUtil.toFormula(astNode));
    }

    @Test
    void toFormulaReadUnits() throws Exception {
        String sbml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <sbml xmlns="http://www.sbml.org/sbml/level3/version1/core" level="3" version="1">
                  <model id="m1">
                    <listOfParameters>
                      <parameter id="p1" constant="true"/>
                    </listOfParameters>
                    <listOfInitialAssignments>
                      <initialAssignment symbol="p1">
                        <math xmlns="http://www.w3.org/1998/Math/MathML"
                              xmlns:sbml="http://www.sbml.org/sbml/level3/version1/core">
                          <apply>
                            <times/>
                            <cn sbml:units="mole" type="integer"> 2 </cn>
                            <cn sbml:units="dimensionless"> 0.5 </cn>
                          </apply>
                        </math>
                      </initialAssignment>
                    </listOfInitialAssignments>
                  </model>
                </sbml>
                """;
        ASTNode astNode =
                SBMLReader.read(sbml).getModel().getInitialAssignment(0).getMath();
        assertEquals("2 mole*0.5 dimensionless", ASTNodeUtil.toFormula(astNode));
    }
}
