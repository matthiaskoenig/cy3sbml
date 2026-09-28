package org.cy3sbml.biomodel;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;
import org.cy3sbml.util.HtmlUtil;

/**
 * The HTML of the BioModels dialog for a search result: one entry per model with the
 * information of the search response, and the details of the models that were looked up.
 * Does not access the web service.
 */
public final class BiomodelsHtml {
    private static final Pattern PUBMED_ID = Pattern.compile("\\d+");
    private static final Pattern ISO_DATE = Pattern.compile("\\d{4}-\\d{2}-\\d{2}.*");

    private BiomodelsHtml() {}

    /**
     * Returns the HTML of the given search result. Every model has an anchor named by its
     * id, to scroll to it.
     *
     * @param details the details of the models looked up so far, by id
     * @param loading the ids of the models whose details are being looked up
     * @param selected the ids of the models selected in the list, highlighted
     */
    public static String searchResult(
            SearchBioModel.Result result,
            Map<String, Biomodel> details,
            Collection<String> loading,
            Collection<String> selected) {
        StringBuilder html = new StringBuilder();
        int count = result.modelIds().size();
        if (result.isParsed()) {
            html.append(String.format("<h2>%d BioModel%s</h2>", count, count == 1 ? "" : "s"));
        } else {
            html.append(
                    String.format("<h2>%d BioModel%s found</h2>", result.matches(), result.matches() == 1 ? "" : "s"));
            if (count < result.matches()) {
                html.append(String.format(
                        "<p>The first %d models are listed, refine the search to find the others.</p>", count));
            }
            html.append(result.searchContent().toHTML());
        }
        if (!result.isParsed() && count > 0) {
            html.append("<p>Select models in the list to see their details.</p>");
        }
        html.append("<hr>");
        for (String id : result.modelIds()) {
            html.append(model(
                    id,
                    result.summaries().get(id),
                    details.get(id),
                    loading.contains(id),
                    result.isParsed(),
                    selected.contains(id)));
        }
        return BioModelDialogText.getString(html.toString());
    }

    /**
     * Returns the HTML entry of a model: the information of the search response and, if
     * looked up, its details.
     */
    static String model(
            String id,
            BiomodelSummary summary,
            Biomodel details,
            boolean loading,
            boolean requested,
            boolean selected) {
        StringBuilder rows = new StringBuilder();
        rows.append(row("ID", link("https://www.biomodels.org/" + id, id)));
        String name = details != null ? details.name() : summary != null ? summary.name() : "";
        rows.append(row("Name", HtmlUtil.escape(name)));
        if (summary != null) {
            rows.append(row("Submitted", HtmlUtil.escape(date(summary.submissionDate()))));
            rows.append(row("Last modified", HtmlUtil.escape(date(summary.lastModified()))));
        }
        if (details != null) {
            if (!details.submissionIdentifier().equals(id)) {
                rows.append(row("Submission ID", HtmlUtil.escape(details.submissionIdentifier())));
            }
            // the description is HTML
            rows.append(row("Description", details.description()));
            rows.append(row("Authors", HtmlUtil.escape(details.authors())));
            rows.append(row("Publication", publication(details.publicationIdentifier())));
        } else if (loading) {
            rows.append(row("", "<i>Loading the details ...</i>"));
        } else if (selected || requested) {
            rows.append(row("", "<i>The details could not be loaded.</i>"));
        }
        String marker = selected ? " bgcolor=\"#339933\"" : "";
        return "<a name=\"" + HtmlUtil.escape(id) + "\"></a>"
                + "<table width=\"100%\"><tr><td width=\"6\"" + marker + "></td><td>"
                + "<table border=\"0\">" + rows + "</table>"
                + "</td></tr></table><hr>";
    }

    /** Returns the date of an ISO 8601 date-time, the text as is otherwise. */
    static String date(String dateTime) {
        if (dateTime == null) {
            return "";
        }
        return ISO_DATE.matcher(dateTime).matches() ? dateTime.substring(0, 10) : dateTime;
    }

    /**
     * Returns the link of a publication identifier: a PubMed id or a DOI, the escaped text
     * for any other identifier.
     */
    static String publication(String identifier) {
        if (identifier == null || identifier.isEmpty()) {
            return "";
        }
        if (PUBMED_ID.matcher(identifier).matches()) {
            return link("https://pubmed.ncbi.nlm.nih.gov/" + identifier + "/", identifier);
        }
        if (identifier.startsWith("10.")) {
            return link("https://doi.org/" + identifier, identifier);
        }
        return HtmlUtil.escape(identifier);
    }

    private static String link(String url, String text) {
        return "<a href=\"" + HtmlUtil.escape(url) + "\">" + HtmlUtil.escape(text) + "</a>";
    }

    /** Returns the table row of the given attribute, none for an empty value. */
    private static String row(String attribute, String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return "<tr><td valign=\"top\" width=\"110\" nowrap><b><font size=\"-1\">" + attribute + "</font></b></td>"
                + "<td><font size=\"-1\">" + value + "</font></td></tr>";
    }
}
