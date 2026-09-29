package org.cy3sbml.util;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.distrib.DistribConstants;
import org.sbml.jsbml.ext.distrib.DistribSBasePlugin;
import org.sbml.jsbml.ext.distrib.UncertParameter;
import org.sbml.jsbml.ext.distrib.UncertSpan;
import org.sbml.jsbml.ext.distrib.Uncertainty;

/**
 * The uncertainties of the SBML distrib package (version 1): every SBase can have a
 * list of uncertainties, each with uncertainty parameters and spans (mean, standard
 * deviation, confidence interval, a distribution, ...).
 */
public final class DistribUtil {
    private static final String UNSET_BOUND = "?";

    private DistribUtil() {}

    /**
     * The uncertainties of the SBase, empty if it has none. The SBase is not changed:
     * JSBML creates the distrib plugin and the list on access, this does not.
     */
    public static List<Uncertainty> uncertainties(SBase sbase) {
        if (sbase.getExtension(DistribConstants.shortLabel) instanceof DistribSBasePlugin plugin
                && plugin.isSetListOfUncertainties()) {
            return new ArrayList<>(plugin.getListOfUncertainties());
        }
        return List.of();
    }

    /**
     * One line text of the uncertainties: the parameters of an uncertainty are
     * separated by {@code "; "}, the uncertainties by {@code " | "}, each prefixed by
     * its id if set, e.g. {@code "u1: mean=4.2; confidenceInterval=[3.5, 4.9] | u2: ..."}.
     * Empty for no uncertainties.
     */
    public static String summary(List<Uncertainty> uncertainties) {
        return uncertainties.stream().map(DistribUtil::summary).collect(Collectors.joining(" | "));
    }

    private static String summary(Uncertainty uncertainty) {
        String parameters = parameters(uncertainty.getListOfUncertParameters());
        return uncertainty.isSetId() ? uncertainty.getId() + ": " + parameters : parameters;
    }

    private static String parameters(List<UncertParameter> parameters) {
        return parameters.stream().map(DistribUtil::parameter).collect(Collectors.joining("; "));
    }

    /**
     * The uncertainty parameter as {@code label=value}, e.g. {@code standardDeviation=0.3 mole},
     * {@code mean=p2}, {@code confidenceInterval=[1.0, 5.0]} or {@code distribution=normal(3, 0.5)},
     * followed by its nested parameters in parentheses.
     */
    public static String parameter(UncertParameter parameter) {
        String text = label(parameter);
        String value = value(parameter);
        if (value != null) {
            text += "=" + value;
        }
        if (parameter.isSetListOfUncertParameters() && parameter.getUncertParameterCount() > 0) {
            text += " (" + parameters(parameter.getListOfUncertParameters()) + ")";
        }
        return text;
    }

    /** The type, for an external parameter its name if it is set. */
    public static String label(UncertParameter parameter) {
        if (parameter.isSetType()
                && parameter.getType() == UncertParameter.Type.externalParameter
                && parameter.isSetName()) {
            return parameter.getName();
        }
        return parameter.isSetType() ? parameter.getType().toString() : parameter.getElementName();
    }

    /**
     * The value of the parameter: the value, else the var, followed by the units if set;
     * the bounds of a span; else the formula of the math, else the definition URL; null
     * if none is set.
     */
    public static String value(UncertParameter parameter) {
        if (parameter instanceof UncertSpan span) {
            return "[" + lower(span) + ", " + upper(span) + "]";
        }
        String value = null;
        if (parameter.isSetValue()) {
            value = String.valueOf(parameter.getValue());
        } else if (parameter.isSetVar()) {
            value = parameter.getVar();
        }
        if (value != null) {
            return parameter.isSetUnits() ? value + " " + parameter.getUnits() : value;
        }
        if (parameter.isSetMath()) {
            return parameter.getMath().toFormula();
        }
        return parameter.isSetDefinitionURL() ? parameter.getDefinitionURL() : null;
    }

    private static String lower(UncertSpan span) {
        if (span.isSetValueLower()) {
            return String.valueOf(span.getValueLower());
        }
        return span.isSetVarLower() ? span.getVarLower() : UNSET_BOUND;
    }

    private static String upper(UncertSpan span) {
        if (span.isSetValueUpper()) {
            return String.valueOf(span.getValueUpper());
        }
        return span.isSetVarUpper() ? span.getVarUpper() : UNSET_BOUND;
    }
}
