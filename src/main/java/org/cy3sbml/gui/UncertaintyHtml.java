package org.cy3sbml.gui;

import java.util.List;
import java.util.Locale;
import org.cy3sbml.comp.CompTargets;
import org.cy3sbml.util.DistribUtil;
import org.cy3sbml.util.HtmlUtil;
import org.cy3sbml.util.SBMLUtil;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.NamedSBase;
import org.sbml.jsbml.SBase;
import org.sbml.jsbml.SimpleSpeciesReference;
import org.sbml.jsbml.ext.distrib.UncertParameter;
import org.sbml.jsbml.ext.distrib.UncertSpan;
import org.sbml.jsbml.ext.distrib.Uncertainty;

/**
 * The HTML of the uncertainties of an SBase (distrib package): per uncertainty its id
 * and name and a table of its uncertainty parameters and spans (type and value, like the
 * attribute table), with the nested
 * parameters after their parent. A var is linked to the node of the element it
 * references.
 */
public final class UncertaintyHtml {
    private static final String TABLE_START = "<table class=\"table table-striped table-condensed table-hover\">\n";
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
        StringBuilder html = new StringBuilder();
        for (Uncertainty uncertainty : uncertainties) {
            html.append(header(uncertainty));
            html.append(TABLE_START);
            for (UncertParameter parameter : DistribUtil.uncertParameters(uncertainty)) {
                rows(parameter, 0, sbase.getModel(), targets, html);
            }
            html.append("</table>\n");
        }
        return html.toString();
    }

    /** The label of the uncertainty with its id and name, like the qualifier of a CVTerm. */
    private static String header(Uncertainty uncertainty) {
        String id = uncertainty.isSetId() ? "<b>" + HtmlUtil.escape(uncertainty.getId()) + "</b>" : "";
        String name = uncertainty.isSetName() ? HtmlUtil.escape(uncertainty.getName()) : "";
        return "<p class=\"cvterm\"><span class=\"qualifier\">uncertainty</span> "
                + String.join(" ", id, name).strip() + "</p>\n";
    }

    private static void rows(
            UncertParameter parameter, int depth, Model model, CompTargets targets, StringBuilder html) {
        String label = HtmlUtil.escape(DistribUtil.label(parameter));
        html.append(depth == 0 ? "<tr>" : "<tr class=\"distrib-nested\">")
                .append("<td>")
                .append(NESTED_PREFIX.repeat(depth))
                .append(label)
                .append("</td><td>")
                .append(cell(parameter, model, targets))
                .append("</td></tr>\n");
        if (parameter.isSetListOfUncertParameters()) {
            for (UncertParameter nested : parameter.getListOfUncertParameters()) {
                rows(nested, depth + 1, model, targets, html);
            }
        }
    }

    /** The value with its units, followed by the link to the definition. */
    private static String cell(UncertParameter parameter, Model model, CompTargets targets) {
        String cell = value(parameter, model, targets);
        if (parameter.isSetUnits()) {
            cell += " " + HtmlUtil.escape(parameter.getUnits());
        }
        String definition = definition(parameter);
        if (!definition.isEmpty()) {
            cell = cell.isEmpty() ? definition : cell + " <small>(" + definition + ")</small>";
        }
        return cell;
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
        // a species reference is an edge, not a node
        if (element == null || !element.isSetMetaId() || element instanceof SimpleSpeciesReference) {
            return html;
        }
        return SBMLUtil.nodeLink(model, element.getMetaId(), targets)
                .map(link -> html + link)
                .orElse(html);
    }

    /** The definition URL as a link named by its last part; only http and https URLs are links. */
    private static String definition(UncertParameter parameter) {
        if (!parameter.isSetDefinitionURL()) {
            return "";
        }
        String url = parameter.getDefinitionURL();
        String scheme = url.toLowerCase(Locale.ROOT);
        if (!scheme.startsWith("http://") && !scheme.startsWith("https://")) {
            return HtmlUtil.escape(url);
        }
        // the last part of the URL names the definition, e.g. "normal" or "PROB_k0000225"
        String name = url.substring(Math.max(url.lastIndexOf('/'), url.lastIndexOf('#')) + 1);
        if (name.isEmpty()) {
            name = url;
        }
        String escaped = HtmlUtil.escape(url);
        return "<a href=\"" + escaped + "\" title=\"" + escaped + "\">" + HtmlUtil.escape(name) + "</a>";
    }
}
