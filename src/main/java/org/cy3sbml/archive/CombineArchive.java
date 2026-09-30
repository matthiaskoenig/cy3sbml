package org.cy3sbml.archive;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.cy3sbml.util.XMLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Reads a COMBINE archive (OMEX): unpacks it into a directory and reads its manifest
 * ({@code manifest.xml}) and the metadata of the archive ({@code metadata.rdf}).
 * <p>
 * The metadata is read as written by libCombine (vCard creators, {@code rdf:about="."}) and
 * as in OMEX metadata 1.2 (FOAF creators, the archive as {@code http://omex-library.org/<name>}).
 */
public final class CombineArchive {
    private static final Logger logger = LoggerFactory.getLogger(CombineArchive.class);

    private static final String MANIFEST = "manifest.xml";
    private static final String MANIFEST_FORMAT = "omex-manifest";
    private static final String ARCHIVE_FORMAT = "combine.specifications/omex";
    private static final String METADATA_FORMAT = "omex-metadata";
    private static final String RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";
    private static final List<String> DC_NS = List.of("http://purl.org/dc/terms/", "http://purl.org/dc/elements/1.1/");
    private static final String VCARD_NS = "http://www.w3.org/2006/vcard/ns#";
    private static final String FOAF_NS = "http://xmlns.com/foaf/0.1/";
    private static final String XHTML_DESCRIPTION_CLASS = "dc:description";

    /** The metadata of the archive itself. */
    private record Metadata(String title, String description, List<ArchiveInfo.Creator> creators) {}

    private CombineArchive() {}

    /**
     * Unpacks the archive into the directory and reads its content.
     *
     * @param stream    the archive
     * @param name      the file name of the archive, for the messages and the metadata
     * @param directory the directory the archive is unpacked into, created if needed
     * @return the content of the archive; the file of an entry is
     *     {@code directory.resolve(entry.location())}
     * @throws CombineArchiveException if the file is no COMBINE archive, has an entry outside
     *     the archive, lacks a file to import, or has no SBML file
     */
    public static ArchiveInfo extract(InputStream stream, String name, Path directory)
            throws CombineArchiveException, IOException {
        Path root = directory.toAbsolutePath().normalize();
        Files.createDirectories(root);
        unzip(stream, name, root);

        Path manifest = root.resolve(MANIFEST);
        if (!Files.isRegularFile(manifest)) {
            throw new CombineArchiveException(
                    String.format("The file %s is not a COMBINE archive: it has no %s.", name, MANIFEST));
        }
        List<ArchiveInfo.Entry> entries = readManifest(manifest, name, root);
        Metadata metadata = readMetadata(root, name, entries);
        ArchiveInfo info =
                new ArchiveInfo(name, metadata.title(), metadata.description(), metadata.creators(), entries);

        if (info.modelsToImport().isEmpty()) {
            throw new CombineArchiveException(String.format("The archive %s contains no SBML file.", name));
        }
        for (ArchiveInfo.Entry entry : entries) {
            if (!Files.isRegularFile(root.resolve(entry.location()))) {
                if (info.modelsToImport().contains(entry)) {
                    throw new CombineArchiveException(String.format(
                            "The archive %s has no file %s, which its manifest lists.", name, entry.location()));
                }
                logger.warn("The archive {} has no file {}, which its manifest lists.", name, entry.location());
            }
        }
        return info;
    }

    /** Unpacks the zip into the directory; every entry must stay inside the directory. */
    private static void unzip(InputStream stream, String name, Path root) throws CombineArchiveException, IOException {
        boolean zip = false;
        try (ZipInputStream zipStream = new ZipInputStream(stream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zipStream.getNextEntry()) != null) {
                zip = true;
                Path target = root.resolve(zipEntry.getName()).normalize();
                if (!target.startsWith(root) || target.equals(root)) {
                    throw new CombineArchiveException(String.format(
                            "The archive %s has the entry '%s' outside the archive.", name, zipEntry.getName()));
                }
                if (zipEntry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(zipStream, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        if (!zip) {
            throw new CombineArchiveException(
                    String.format("The file %s is not a COMBINE archive: it is no zip file.", name));
        }
    }

    /** The entries of the manifest, without the archive itself and the manifest. */
    private static List<ArchiveInfo.Entry> readManifest(Path manifest, String name, Path root)
            throws CombineArchiveException {
        Document document;
        try {
            document = documentBuilder().parse(manifest.toFile());
        } catch (SAXException | IOException | ParserConfigurationException e) {
            throw new CombineArchiveException(
                    String.format("The manifest of the archive %s cannot be read: %s", name, e.getMessage()));
        }
        List<ArchiveInfo.Entry> entries = new ArrayList<>();
        NodeList contents = document.getElementsByTagNameNS("*", "content");
        for (int i = 0; i < contents.getLength(); i++) {
            Element content = (Element) contents.item(i);
            String location = normalizeLocation(content.getAttribute("location"));
            String format = content.getAttribute("format").strip();
            if (location.isEmpty()
                    || location.equals(MANIFEST)
                    || format.endsWith(MANIFEST_FORMAT)
                    || format.endsWith(ARCHIVE_FORMAT)) {
                continue;
            }
            Path target = root.resolve(location).normalize();
            if (!target.startsWith(root) || target.equals(root)) {
                throw new CombineArchiveException(
                        String.format("The archive %s has the entry '%s' outside the archive.", name, location));
            }
            entries.add(new ArchiveInfo.Entry(
                    location,
                    format,
                    Boolean.parseBoolean(content.getAttribute("master").strip())));
        }
        return entries;
    }

    /** The location without a leading {@code ./} or {@code /}; the archive itself is empty. */
    static String normalizeLocation(String location) {
        String value = location.strip();
        while (value.startsWith("./") || value.startsWith("/")) {
            value = value.substring(value.startsWith("./") ? 2 : 1);
        }
        return value.equals(".") ? "" : value;
    }

    /**
     * The title, description and creators of the archive from its metadata file; empty
     * values if there is none or it cannot be read.
     */
    private static Metadata readMetadata(Path root, String name, List<ArchiveInfo.Entry> entries) {
        Metadata empty = new Metadata("", "", List.of());
        Path file = entries.stream()
                .filter(e -> isMetadata(e.format()))
                .map(e -> root.resolve(e.location()))
                .filter(Files::isRegularFile)
                .findFirst()
                .orElse(null);
        if (file == null) {
            return empty;
        }
        try {
            Document document = documentBuilder().parse(file.toFile());
            Element archive = archiveDescription(document, name);
            if (archive == null) {
                return empty;
            }
            List<ArchiveInfo.Creator> creators = new ArrayList<>();
            for (Element creator : dcChildren(archive, "creator")) {
                Element person = described(document, creator);
                if (person != null) {
                    creators.add(creator(document, person));
                }
            }
            return new Metadata(
                    text(first(dcChildren(archive, "title"))),
                    description(text(first(dcChildren(archive, "description")))),
                    creators);
        } catch (SAXException | IOException | ParserConfigurationException e) {
            logger.debug("The metadata of the archive {} cannot be read", name, e);
            return empty;
        }
    }

    /** The RDF description of the archive itself. */
    private static Element archiveDescription(Document document, String name) {
        for (Element description : elements(document.getElementsByTagNameNS(RDF_NS, "Description"))) {
            String about = description.getAttributeNS(RDF_NS, "about").strip();
            if (about.equals(".") || about.equals("./") || about.equals(name) || about.endsWith("/" + name)) {
                return description;
            }
        }
        return null;
    }

    /** The node a property describes: its nested description, or the one it references. */
    private static Element described(Document document, Element property) {
        for (Element child : childElements(property)) {
            if (RDF_NS.equals(child.getNamespaceURI()) && "Description".equals(child.getLocalName())) {
                return child;
            }
        }
        String resource = property.getAttributeNS(RDF_NS, "resource");
        String nodeId = property.getAttributeNS(RDF_NS, "nodeID");
        for (Element description : elements(document.getElementsByTagNameNS(RDF_NS, "Description"))) {
            if ((!resource.isEmpty() && resource.equals(description.getAttributeNS(RDF_NS, "about")))
                    || (!nodeId.isEmpty() && nodeId.equals(description.getAttributeNS(RDF_NS, "nodeID")))) {
                return description;
            }
        }
        return null;
    }

    /**
     * The creator described by vCard (libCombine) or FOAF (OMEX metadata 1.2); a FOAF name
     * without given and family name is the family name.
     */
    private static ArchiveInfo.Creator creator(Document document, Element person) {
        String given = "";
        String family = "";
        Element name = first(children(person, VCARD_NS, "hasName"));
        if (name != null) {
            Element parts = described(document, name);
            if (parts != null) {
                given = text(first(children(parts, VCARD_NS, "given-name")));
                family = text(first(children(parts, VCARD_NS, "family-name")));
            }
        }
        if (given.isEmpty() && family.isEmpty()) {
            given = text(first(children(person, FOAF_NS, "givenName")));
            family = text(first(children(person, FOAF_NS, "familyName")));
        }
        if (given.isEmpty() && family.isEmpty()) {
            family = text(first(children(person, FOAF_NS, "name")));
        }
        String email = resourceOrText(first(children(person, VCARD_NS, "hasEmail")));
        if (email.isEmpty()) {
            email = resourceOrText(first(children(person, FOAF_NS, "mbox")));
        }
        String organization = text(first(children(person, VCARD_NS, "organization-name")));
        return new ArchiveInfo.Creator(given, family, email.replaceFirst("^mailto:", ""), organization);
    }

    /**
     * The description as plain text. An XHTML description (as in BioModels archives) is
     * reduced to the text of its {@code dc:description} element if it has one.
     */
    static String description(String text) {
        String value = text.strip();
        if (!value.startsWith("<")) {
            return collapse(value);
        }
        try {
            Document xhtml = documentBuilder().parse(new InputSource(new StringReader(value)));
            for (Element element : elements(xhtml.getElementsByTagNameNS("*", "*"))) {
                if (element.getAttribute("class").contains(XHTML_DESCRIPTION_CLASS)) {
                    return collapse(element.getTextContent());
                }
            }
            return collapse(xhtml.getDocumentElement().getTextContent());
        } catch (SAXException | IOException | ParserConfigurationException e) {
            return collapse(value.replaceAll("<[^>]*>", " "));
        }
    }

    private static String collapse(String text) {
        return text.replaceAll("\\s+", " ").strip();
    }

    private static List<Element> dcChildren(Element parent, String localName) {
        List<Element> result = new ArrayList<>();
        for (String namespace : DC_NS) {
            result.addAll(children(parent, namespace, localName));
        }
        return result;
    }

    private static List<Element> children(Element parent, String namespace, String localName) {
        return childElements(parent).stream()
                .filter(e -> namespace.equals(e.getNamespaceURI()) && localName.equals(e.getLocalName()))
                .toList();
    }

    private static List<Element> childElements(Element parent) {
        List<Element> result = new ArrayList<>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element element) {
                result.add(element);
            }
        }
        return result;
    }

    private static List<Element> elements(NodeList nodes) {
        List<Element> result = new ArrayList<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            result.add((Element) nodes.item(i));
        }
        return result;
    }

    private static Element first(List<Element> elements) {
        return elements.isEmpty() ? null : elements.get(0);
    }

    private static String text(Element element) {
        return element == null ? "" : element.getTextContent().strip();
    }

    private static String resourceOrText(Element element) {
        if (element == null) {
            return "";
        }
        String resource = element.getAttributeNS(RDF_NS, "resource").strip();
        return resource.isEmpty() ? text(element) : resource;
    }

    /**
     * A namespace aware document builder of the hardened factory of {@link XMLUtil} (no
     * DTDs, no external entities) that reports parse errors only through its exceptions; the
     * default error handler also prints them to stderr.
     */
    private static DocumentBuilder documentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = XMLUtil.documentBuilderFactory();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setErrorHandler(new DefaultHandler() {
            @Override
            public void fatalError(SAXParseException e) throws SAXException {
                throw e;
            }
        });
        return builder;
    }

    /** Whether the format is the one of the OMEX metadata, ignoring case. */
    static boolean isMetadata(String format) {
        return format.toLowerCase(Locale.ROOT).endsWith(METADATA_FORMAT);
    }
}
