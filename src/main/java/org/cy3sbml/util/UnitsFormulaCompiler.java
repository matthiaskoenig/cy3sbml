package org.cy3sbml.util;

import org.sbml.jsbml.util.compilers.ASTNodeValue;
import org.sbml.jsbml.util.compilers.FormulaCompilerLibSBML;

/**
 * Infix formula of the math with the inline units of numbers (SBML L3 {@code sbml:units} on {@code cn}),
 * written after the number like libSBML's {@code formulaToL3String}, e.g. {@code 1 dimensionless}.
 *
 * <p>JSBML's {@link FormulaCompilerLibSBML} (used by {@code ASTNode.toFormula}) drops the units (#262).
 * Rational numbers keep no units, since JSBML compiles them without (<code>frac(int, int)</code>).
 */
public class UnitsFormulaCompiler extends FormulaCompilerLibSBML {

    @Override
    public ASTNodeValue compile(double mantissa, int exponent, String units) {
        return withUnits(super.compile(mantissa, exponent, units), units);
    }

    @Override
    public ASTNodeValue compile(double real, String units) {
        return withUnits(super.compile(real, units), units);
    }

    @Override
    public ASTNodeValue compile(int integer, String units) {
        return withUnits(super.compile(integer, units), units);
    }

    private ASTNodeValue withUnits(ASTNodeValue value, String units) {
        if (units == null || units.isEmpty()) {
            return value;
        }
        return new ASTNodeValue(value + " " + units, this);
    }
}
