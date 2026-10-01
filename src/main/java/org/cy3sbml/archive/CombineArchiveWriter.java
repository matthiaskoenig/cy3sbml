package org.cy3sbml.archive;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.comp.CompModels;
import org.cy3sbml.comp.ModelResolution;
import org.cy3sbml.util.HtmlUtil;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLWriter;
import org.sbml.jsbml.ext.comp.ExternalModelDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Writes an SBML document into a COMBINE archive (OMEX), with the files of its comp external
 * model definitions, so that a tool which reads the archive (sbml4humans) resolves the external
 * models inside the archive as cy3sbml resolved them.
 * <p>
 * The document is the master entry. The document of an external model definition is written at
 * the source of the definition, resolved against the location of the entry which names it, so
 * the relative sources of the files stay valid; this is followed for the external model
 * definitions of the written documents too. A definition whose source is a url, resolves
 * outside the archive or could not be read is left out, its model is not resolved in the
 * archive.
 */
public final class CombineArchiveWriter {
    private static final Logger logger = LoggerFactory.getLogger(CombineArchiveWriter.class);

    /** The file name of the master entry, if the document has none. */
    static final String DEFAULT_LOCATION = "model.xml";

    private static final String OMEX = "http://identifiers.org/combine.specifications/omex";
    private static final String MANIFEST_FORMAT = "http://identifiers.org/combine.specifications/omex-manifest";
    private static final Pattern FILE_NAME = Pattern.compile("[A-Za-z0-9._-]+");

    private CombineArchiveWriter() {}

    /**
     * Writes the archive of the document.
     *
     * @param document the document, the master entry
     * @param location the location of the document in the archive, e.g. {@code model.xml}
     * @param models   the resolution of the external model definitions of the document
     * @return the location of the master entry in the manifest, e.g. {@code ./model.xml}
     * @throws IOException if the archive cannot be written
     */
    public static String write(SBMLDocument document, String location, CompModels models, OutputStream out)
            throws IOException {
        Map<String, SBMLDocument> entries = entries(document, location, models);
        try (ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8)) {
            zip.putNextEntry(new ZipEntry("manifest.xml"));
            zip.write(manifest(entries, location).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
            for (Map.Entry<String, SBMLDocument> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(sbml(entry.getValue()));
                zip.closeEntry();
            }
        }
        return "./" + location;
    }

    /**
     * The location of the document in an archive: its location in the archive it was imported
     * from, else the file name of its location, else {@value #DEFAULT_LOCATION}.
     */
    public static String masterLocation(SBMLDocument document, Optional<ArchiveImport> archive) {
        if (archive.isPresent()) {
            String imported = archive.get().location();
            return imported.startsWith("./") ? imported.substring(2) : imported;
        }
        if (document.isSetLocationURI()) {
            try {
                String path = new URI(document.getLocationURI()).getPath();
                if (path != null) {
                    String name = path.substring(path.lastIndexOf('/') + 1);
                    if (FILE_NAME.matcher(name).matches()) {
                        return name;
                    }
                }
            } catch (URISyntaxException e) {
                logger.debug("The location '{}' is no URI", document.getLocationURI());
            }
        }
        return DEFAULT_LOCATION;
    }

    /** The documents of the archive by their location, the document first. */
    private static Map<String, SBMLDocument> entries(SBMLDocument document, String location, CompModels models) {
        Map<String, SBMLDocument> entries = new LinkedHashMap<>();
        // by identity, JSBML's equals compares the content
        IdentityHashMap<SBMLDocument, String> locations = new IdentityHashMap<>();
        entries.put(location, document);
        locations.put(document, location);
        // the externals named by the documents of the archive, until no document is added; an
        // external named by a document which is left out is not followed
        List<CompModels.External> remaining = new ArrayList<>(models.externalModels());
        boolean added = true;
        while (added) {
            added = false;
            for (Iterator<CompModels.External> it = remaining.iterator(); it.hasNext(); ) {
                CompModels.External external = it.next();
                String parent = locations.get(external.definition().getSBMLDocument());
                if (parent == null) {
                    continue;
                }
                it.remove();
                added |= add(external, parent, entries, locations);
            }
        }
        return entries;
    }

    /** Adds the document of the external to the archive, returns whether it was added. */
    private static boolean add(
            CompModels.External external,
            String parent,
            Map<String, SBMLDocument> entries,
            IdentityHashMap<SBMLDocument, String> locations) {
        ExternalModelDefinition definition = external.definition();
        if (!(external.resolution() instanceof ModelResolution.Resolved resolved)) {
            logger.warn(
                    "The external model definition '{}' is not resolved, it is left out of the archive",
                    definition.getId());
            return false;
        }
        SBMLDocument target = resolved.document();
        if (locations.containsKey(target)) {
            return false;
        }
        Optional<String> location = resolve(parent, definition.getSource());
        if (location.isEmpty() || entries.containsKey(location.get())) {
            logger.warn(
                    "The source '{}' of the external model definition '{}' is no file of the archive, it is left out",
                    definition.getSource(),
                    definition.getId());
            return false;
        }
        entries.put(location.get(), target);
        locations.put(target, location.get());
        return true;
    }

    /**
     * The location of a relative source, a path or a relative {@code file:} URI, resolved
     * against the directory of the location of the entry which names it; empty for another
     * source or one outside the archive.
     */
    static Optional<String> resolve(String parent, String source) {
        if (source == null || source.isBlank()) {
            return Optional.empty();
        }
        String path;
        try {
            URI uri = new URI(source.strip());
            if (uri.getScheme() == null) {
                path = uri.getPath();
            } else if ("file".equalsIgnoreCase(uri.getScheme())
                    && !uri.getSchemeSpecificPart().startsWith("/")) {
                path = uri.getSchemeSpecificPart();
            } else {
                return Optional.empty();
            }
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        if (path == null || path.isEmpty() || path.startsWith("/")) {
            return Optional.empty();
        }
        Deque<String> segments = new ArrayDeque<>();
        String directory = parent.contains("/") ? parent.substring(0, parent.lastIndexOf('/') + 1) : "";
        for (String segment : (directory + path).split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".")) {
                continue;
            }
            if (segment.equals("..")) {
                if (segments.isEmpty()) {
                    // outside the archive
                    return Optional.empty();
                }
                segments.removeLast();
            } else {
                segments.addLast(segment);
            }
        }
        return segments.isEmpty() ? Optional.empty() : Optional.of(String.join("/", segments));
    }

    private static String manifest(Map<String, SBMLDocument> entries, String master) {
        StringBuilder xml = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8"?>
                <omexManifest xmlns="http://identifiers.org/combine.specifications/omex-manifest">
                """);
        xml.append(String.format("  <content location=\".\" format=\"%s\"/>%n", OMEX));
        xml.append(String.format("  <content location=\"./manifest.xml\" format=\"%s\"/>%n", MANIFEST_FORMAT));
        for (Map.Entry<String, SBMLDocument> entry : entries.entrySet()) {
            SBMLDocument document = entry.getValue();
            xml.append(String.format(
                    "  <content location=\"./%s\" format=\"%s\"%s/>%n",
                    HtmlUtil.escape(entry.getKey()),
                    String.format(
                            "http://identifiers.org/combine.specifications/sbml.level-%d.version-%d",
                            document.getLevel(), document.getVersion()),
                    entry.getKey().equals(master) ? " master=\"true\"" : ""));
        }
        return xml.append("</omexManifest>\n").toString();
    }

    private static byte[] sbml(SBMLDocument document) throws IOException {
        try {
            // writing only reads the document, which the info panel may render at the same time
            return new SBMLWriter().writeSBMLToString(document).getBytes(StandardCharsets.UTF_8);
        } catch (XMLStreamException e) {
            throw new IOException("The SBML document could not be written: " + e.getMessage(), e);
        }
    }
}
