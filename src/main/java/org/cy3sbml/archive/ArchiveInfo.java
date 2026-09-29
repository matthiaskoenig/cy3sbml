package org.cy3sbml.archive;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The content of a COMBINE archive (OMEX): its manifest entries and the metadata of the
 * archive from {@code metadata.rdf}.
 *
 * @param name        the file name of the archive
 * @param title       the title of the archive, empty if not set
 * @param description the description of the archive as plain text, empty if not set
 * @param creators    the creators of the archive
 * @param entries     the files of the archive in manifest order, without the archive itself
 *                    and the manifest
 */
public record ArchiveInfo(String name, String title, String description, List<Creator> creators, List<Entry> entries) {

    /** A creator of the archive; the parts that are not set are empty. */
    public record Creator(String givenName, String familyName, String email, String organization) {}

    /**
     * A file of the archive.
     *
     * @param location the path of the file in the archive, without a leading {@code ./}
     * @param format   the format URI or media type of the file
     * @param master   whether the file is a master file of the archive
     */
    public record Entry(String location, String format, boolean master) {}

    private static final Pattern COMBINE_SPECIFICATION = Pattern.compile(
            "^https?://identifiers\\.org/combine\\.specifications/([^./]+)(?:\\.level-(\\d+)\\.version-(\\d+))?.*$",
            Pattern.CASE_INSENSITIVE);
    private static final String MEDIATYPES = "/mediatypes/";

    public ArchiveInfo {
        creators = List.copyOf(creators);
        entries = List.copyOf(entries);
    }

    /** The SBML files of the archive. */
    public List<Entry> sbmlEntries() {
        return entries.stream().filter(e -> isSbml(e.format())).toList();
    }

    /**
     * The SBML files to import: the master SBML files if there is one, else all SBML files.
     * The other SBML files are only read if an imported file references them.
     */
    public List<Entry> modelsToImport() {
        List<Entry> sbml = sbmlEntries();
        List<Entry> masters = sbml.stream().filter(Entry::master).toList();
        return masters.isEmpty() ? sbml : masters;
    }

    /**
     * Whether the format is SBML: a COMBINE specification URI of SBML (any level and
     * version) or the media type {@code application/sbml+xml}.
     */
    public static boolean isSbml(String format) {
        if (format == null) {
            return false;
        }
        Matcher matcher = COMBINE_SPECIFICATION.matcher(format.strip());
        if (matcher.matches()) {
            return "sbml".equalsIgnoreCase(matcher.group(1));
        }
        String lower = format.strip().toLowerCase(Locale.ROOT);
        return lower.endsWith("application/sbml+xml") || lower.endsWith("text/xml+sbml");
    }

    /**
     * A short name of the format, e.g. {@code SBML L3V1}, {@code SED-ML}, {@code OMEX metadata}
     * for COMBINE specifications, the media type for a media type URI
     * ({@code http://purl.org/NET/mediatypes/text/csv} is {@code text/csv}), else the format.
     */
    public static String formatLabel(String format) {
        if (format == null || format.isBlank()) {
            return "";
        }
        String value = format.strip();
        Matcher matcher = COMBINE_SPECIFICATION.matcher(value);
        if (matcher.matches()) {
            String label = specificationName(matcher.group(1));
            if (matcher.group(2) != null) {
                label += " L" + matcher.group(2) + "V" + matcher.group(3);
            }
            return label;
        }
        int mediatypes = value.toLowerCase(Locale.ROOT).indexOf(MEDIATYPES);
        if (mediatypes >= 0) {
            return value.substring(mediatypes + MEDIATYPES.length());
        }
        return value;
    }

    private static String specificationName(String specification) {
        return switch (specification.toLowerCase(Locale.ROOT)) {
            case "sbml" -> "SBML";
            case "sed-ml" -> "SED-ML";
            case "sbgn" -> "SBGN";
            case "cellml" -> "CellML";
            case "sbol" -> "SBOL";
            case "omex" -> "OMEX";
            case "omex-manifest" -> "OMEX manifest";
            case "omex-metadata" -> "OMEX metadata";
            default -> specification;
        };
    }
}
