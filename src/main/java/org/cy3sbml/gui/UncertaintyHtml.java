package org.cy3sbml.gui;

import java.util.List;
import org.cy3sbml.comp.CompTargets;
import org.cy3sbml.util.DistribUtil;
import org.cy3sbml.util.HtmlUtil;
import org.cy3sbml.util.SBMLUtil;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.ext.distrib.UncertParameter;
import org.sbml.jsbml.ext.distrib.UncertSpan;
import org.sbml.jsbml.ext.distrib.Uncertainty;

/**
 * The HTML of the uncertainties of an SBase (distrib package): per uncertainty its id
 * and name and a table of its uncertainty parameters and spans, with the nested
 * parameters after their parent. A var is linked to the node of the element it
 * references.
 */
public final class UncertaintyHtml {
    private static final String TABLE_START = "<table class=\"table table-striped table-condensed table-hover\">\n"
            + "<tr><th>type</th><th>value</th><th>units</th><th>definition</th></tr>\n";
    private static final String UNSET_BOUND = "?";
    private static final String NESTED_PREFIX = "&nbsp;&nbsp;&#8627;&nbsp;";

    private UncertaintyHtml() {}

    /**
     * The HTML of the uncertainties of the SBase, empty if it has none.
     *
     * @param targets the network collections of the open documents, for the links of the
     *     vars, may be null
     */
    public static String create(SBase sbase, CompTargets targets) {
        List<Uncertainty> uncertainties = DistribUtil.uncertainties(sbase);
        if (uncertainties.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<h3>Uncertainties</h3>\n");
        for (Uncertainty uncertainty : uncertainties) {
            html.append(header(uncertainty));
            html.append(TABLE_START);
            for (UncertParameter parameter : uncertainty.getListOfUncertParameters()) {
                rows(parameter, 0, sbase.getModel(), targets, html);
            }
            html.append("</table>\n");
        }
        return html.toString();
    }

    private static String header(Uncertainty uncertainty) {
        if (!uncertainty.isSetId() && !uncertainty.isSetName()) {
            return "";
        }
        String id = uncertainty.isSetId() ? "<b>" + HtmlUtil.escape(uncertainty.getId()) + "</b>" : "";
        String name = uncertainty.isSetName() ? HtmlUtil.escape(uncertainty.getName()) : "";
        return "<p class=\"cvterm\">" + String.join(" ", id, name).strip() + "</p>\n";
    }

    private static void rows(
            UncertParameter parameter, int depth, Model model, CompTargets targets, StringBuilder html) {
        String label = HtmlUtil.escape(DistribUtil.label(parameter));
        html.append(depth == 0 ? "<tr>" : "<tr class=\"distrib-nested\">")
                .append("<td>")
                .append(NESTED_PREFIX.repeat(depth))
                .append(label)
                .append("</td><td>")
                .append(value(parameter, model, targets))
                .append("</td><td>")
                .append(parameter.isSetUnits() ? HtmlUtil.escape(parameter.getUnits()) : "")
                .append("</td><td>")
                .append(definition(parameter))
                .append("</td></tr>\n");
        if (parameter.isSetListOfUncertParameters()) {
            for (UncertParameter nested : parameter.getListOfUncertParameters()) {
                rows(nested, depth + 1, model, targets, html);
            }
        }
    }

    /** The value, else the var, the bounds of a span, else the formula of the math. */
    private static String value(UncertParameter parameter, Model model, CompTargets targets) {
        if (parameter instanceof UncertSpan span) {
            String lower = span.isSetValueLower()
                    ? String.valueOf(span.getValueLower())
                    : span.isSetVarLower() ? var(span.getVarLower(), model, targets) : UNSET_BOUND;
            String upper = span.isSetValueUpper()
                    ? String.valueOf(span.getValueUpper())
                    : span.isSetVarUpper() ? var(span.getVarUpper(), model, targets) : UNSET_BOUND;
            return "[" + lower + ", " + upper + "]";
        }
        if (parameter.isSetValue()) {
            return String.valueOf(parameter.getValue());
        }
        if (parameter.isSetVar()) {
            return var(parameter.getVar(), model, targets);
        }
        return parameter.isSetMath() ? HtmlUtil.escape(parameter.getMath().toFormula()) : "";
    }

    /** The id with a link to the node of the element it references, if there is one. */
    private static String var(String id, Model model, CompTargets targets) {
        String html = HtmlUtil.escape(id);
        NamedSBase element = model == null ? null : model.findNamedSBase(id);
        if (element == null || !element.isSetMetaId()) {
            return html;
        }
        return SBMLUtil.nodeLink(model, element.getMetaId(), targets)
                .map(link -> html + link)
                .orElse(html);
    }

    private static String definition(UncertParameter parameter) {
        if (!parameter.isSetDefinitionURL()) {
            return "";
        }
        String url = HtmlUtil.escape(parameter.getDefinitionURL());
        return "<a href=\"" + url + "\">" + url + "</a>";
    }
}
