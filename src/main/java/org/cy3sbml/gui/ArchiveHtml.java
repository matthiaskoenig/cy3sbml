package org.cy3sbml.gui;

import java.util.ArrayList;
import java.util.List;
import org.cy3sbml.archive.ArchiveImport;
import org.cy3sbml.archive.ArchiveInfo;
import org.cy3sbml.util.HtmlUtil;

/**
 * The HTML of the COMBINE archive a document was imported from: the archive name and
 * title, its description and creators, and its files (location, format, master), the
 * imported SBML file in bold.
 */
public final class ArchiveHtml {
    private static final String TABLE_START = "<table class=\"table table-striped table-condensed table-hover\">\n";

    private ArchiveHtml() {}

    public static String create(ArchiveImport archive) {
        ArchiveInfo info = archive.info();
        StringBuilder html = new StringBuilder("<p class=\"cvterm\"><span class=\"qualifier\">archive</span> <b>")
                .append(HtmlUtil.escape(info.name()))
                .append("</b>");
        if (!info.title().isEmpty()) {
            html.append(" ").append(HtmlUtil.escape(info.title()));
        }
        html.append("</p>\n");
        if (!info.description().isEmpty()) {
            html.append("<p>").append(HtmlUtil.escape(info.description())).append("</p>\n");
        }
        for (ArchiveInfo.Creator creator : info.creators()) {
            html.append("<p>creator: ")
                    .append(HtmlUtil.escape(creator(creator)))
                    .append("</p>\n");
        }
        html.append(TABLE_START);
        for (ArchiveInfo.Entry entry : info.entries()) {
            String location = HtmlUtil.escape(entry.location());
            if (entry.location().equals(archive.location())) {
                location = "<b>" + location + "</b>";
            }
            String format = HtmlUtil.escape(ArchiveInfo.formatLabel(entry.format()));
            if (entry.master()) {
                format += ", master";
            }
            html.append("<tr><td>")
                    .append(location)
                    .append("</td><td>")
                    .append(format)
                    .append("</td></tr>\n");
        }
        return html.append("</table>\n").toString();
    }

    /** The name, email and organization of the creator that are set. */
    private static String creator(ArchiveInfo.Creator creator) {
        List<String> parts = new ArrayList<>();
        String name =
                String.join(" ", creator.givenName(), creator.familyName()).strip();
        if (!name.isEmpty()) {
            parts.add(name);
        }
        if (!creator.organization().isEmpty()) {
            parts.add(creator.organization());
        }
        if (!creator.email().isEmpty()) {
            parts.add(creator.email());
        }
        return String.join(", ", parts);
    }
}
