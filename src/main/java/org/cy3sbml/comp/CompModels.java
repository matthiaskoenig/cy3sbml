package org.cy3sbml.comp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.comp.ModelResolution.Failed;
import org.cy3sbml.comp.ModelResolution.Resolved;
import org.cy3sbml.util.HttpJson;
import org.cy3sbml.util.SBMLUtil;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.SBMLDocument;
import org.sbml.jsbml.SBMLReader;
import org.sbml.jsbml.ext.comp.CompConstants;
import org.sbml.jsbml.ext.comp.CompSBMLDocumentPlugin;
import org.sbml.jsbml.ext.comp.ExternalModelDefinition;
import org.sbml.jsbml.ext.comp.ModelDefinition;
import org.sbml.jsbml.ext.comp.Submodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves the model references of the comp package for one document: the model a
 * {@link Submodel} instantiates, which is the main model, a {@link ModelDefinition}
 * or the model of an {@link ExternalModelDefinition}.
 * <p>
 * External model definitions are loaded from their source, resolved relative to the
 * location of the document that defines them ({@link SBMLDocument#getLocationURI()}),
 * also chains of external model definitions. Every source is read once. A failure,
 * e.g. a missing file or a cycle, is returned as {@link Failed}, nothing is thrown.
 */
public final class CompModels {
    private static final Logger logger = LoggerFactory.getLogger(CompModels.class);

    /** Base of the sources of a document without location, to detect relative sources. */
    private static final URI UNKNOWN_LOCATION = URI.create("unknown-location:/");

    private final SBMLDocument document;
    // documents read from the sources, and the reasons of the sources that could not be read
    private final Map<URI, SBMLDocument> documents = new HashMap<>();
    private final Map<URI, String> failures = new HashMap<>();
    // md5 checksums of the sources
    private final Map<URI, String> md5s = new HashMap<>();
    // the external model definitions whose md5 was checked, by identity
    private final Set<ExternalModelDefinition> md5Checked = Collections.newSetFromMap(new IdentityHashMap<>());
    // the reason of the last failed read that is not remembered
    private String lastFailure;
    // the document and the documents read, by identity (JSBML's equals compares the content)
    private final Set<SBMLDocument> ownDocuments = Collections.newSetFromMap(new IdentityHashMap<>());
    private HttpJson http;

    /** Resolver for the model references of the document and the documents it loads. */
    public CompModels(SBMLDocument document) {
        this(document, Map.of());
    }

    /**
     * Resolver that uses the given open documents for their locations instead of reading
     * the files again, e.g. the documents of a restored session.
     */
    public CompModels(SBMLDocument document, Map<URI, SBMLDocument> openDocuments) {
        documents.putAll(openDocuments);
        ownDocuments.addAll(openDocuments.values());
        this.document = document;
        ownDocuments.add(document);
        if (document.isSetLocationURI()) {
            // a source that refers back to the file itself is this document
            try {
                documents.put(new URI(document.getLocationURI()), document);
            } catch (URISyntaxException e) {
                logger.warn("The location '{}' is no valid URI.", document.getLocationURI());
            }
        }
    }

    /** The document whose model references are resolved. */
    public SBMLDocument document() {
        return document;
    }

    /** True if the document is the document of this resolver or one it read. */
    public boolean owns(SBMLDocument other) {
        return ownDocuments.contains(other);
    }

    /** The model the submodel instantiates. */
    public ModelResolution resolve(Submodel submodel) {
        SBMLDocument scope = submodel.getSBMLDocument();
        if (scope == null) {
            return new Failed(String.format("The submodel '%s' is not part of a document.", submodel.getId()));
        }
        return resolve(scope, submodel.getModelRef());
    }

    /**
     * The model with the id in the scope document: its main model, a model definition or
     * the model of an external model definition.
     */
    public ModelResolution resolve(SBMLDocument scope, String modelRef) {
        return resolve(scope, modelRef, List.of());
    }

    /**
     * Every external model definition of the document and of the documents it loads,
     * each once, in document order, with the resolution of its model.
     */
    public List<External> externalModels() {
        List<External> externals = new ArrayList<>();
        Set<SBMLDocument> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        collectExternalModels(document, externals, visited);
        return externals;
    }

    /** An external model definition with the resolution of its model. */
    public record External(ExternalModelDefinition definition, ModelResolution resolution) {}

    private void collectExternalModels(SBMLDocument scope, List<External> externals, Set<SBMLDocument> visited) {
        if (!visited.add(scope)) {
            return;
        }
        CompSBMLDocumentPlugin plugin = compPlugin(scope);
        if (plugin == null || !plugin.isSetListOfExternalModelDefinitions()) {
            return;
        }
        List<SBMLDocument> loaded = new ArrayList<>();
        for (ExternalModelDefinition definition : plugin.getListOfExternalModelDefinitions()) {
            ModelResolution resolution = resolveExternal(definition, List.of());
            externals.add(new External(definition, resolution));
            if (resolution instanceof Resolved resolved) {
                loaded.add(resolved.document());
            }
        }
        for (SBMLDocument external : loaded) {
            collectExternalModels(external, externals, visited);
        }
    }

    /**
     * @param path the external model definitions on the way to this reference, as
     *     {@code <source URI>#<modelRef>}, to detect cycles
     */
    private ModelResolution resolve(SBMLDocument scope, String modelRef, List<String> path) {
        if (modelRef == null || modelRef.isEmpty()) {
            return new Failed("The model reference is not set.");
        }
        CompSBMLDocumentPlugin plugin = compPlugin(scope);
        if (plugin != null) {
            ModelDefinition modelDefinition = plugin.getModelDefinition(modelRef);
            if (modelDefinition != null) {
                return new Resolved(modelDefinition, scope);
            }
            ExternalModelDefinition external = plugin.getExternalModelDefinition(modelRef);
            if (external != null) {
                return resolveExternal(external, path);
            }
        }
        Model main = scope.getModel();
        if (main != null && modelRef.equals(main.getId())) {
            return new Resolved(main, scope);
        }
        return new Failed(String.format("There is no model '%s' in %s.", modelRef, name(scope)));
    }

    private ModelResolution resolveExternal(ExternalModelDefinition definition, List<String> path) {
        URI source;
        try {
            source = sourceUri(definition);
        } catch (URISyntaxException | MalformedURLException | IllegalArgumentException e) {
            return new Failed(String.format(
                    "The source '%s' of the external model definition '%s' is no valid URI: %s",
                    definition.getSource(), definition.getId(), e.getMessage()));
        }
        if (source == null) {
            return new Failed(String.format(
                    "The location of the file is not known, so the relative source '%s' of the external model "
                            + "definition '%s' cannot be found. Import the file from the file system.",
                    definition.getSource(), definition.getId()));
        }
        String modelRef = definition.isSetModelRef() ? definition.getModelRef() : null;
        String key = source + "#" + (modelRef == null ? "" : modelRef);
        if (path.contains(key)) {
            List<String> cycle = new ArrayList<>(path);
            cycle.add(key);
            return new Failed("The external model definitions form a cycle: " + String.join(" -> ", cycle));
        }
        SBMLDocument external = load(source);
        if (external == null) {
            return new Failed(failures.getOrDefault(source, lastFailure));
        }
        if (definition.isSetMd5()) {
            checkMd5(definition, source);
        }
        if (modelRef == null) {
            // without model reference, the main model of the source is referenced
            return external.isSetModel()
                    ? new Resolved(external.getModel(), external)
                    : new Failed(String.format("There is no model in %s.", source));
        }
        List<String> next = new ArrayList<>(path);
        next.add(key);
        return resolve(external, modelRef, next);
    }

    /**
     * The absolute URI of the source of the external model definition, resolved relative
     * to the location of its document.
     *
     * @return the URI, {@code null} for a relative source of a document without location
     */
    static URI sourceUri(ExternalModelDefinition definition) throws URISyntaxException, MalformedURLException {
        SBMLDocument scope = definition.getSBMLDocument();
        if (scope != null && scope.isSetLocationURI()) {
            URI location = new URI(scope.getLocationURI());
            return definition.getAbsoluteSourceURI(location.resolve("."));
        }
        URI source = definition.getAbsoluteSourceURI(UNKNOWN_LOCATION);
        return UNKNOWN_LOCATION.getScheme().equals(source.getScheme()) ? null : source;
    }

    /**
     * Reads the document at the URI once, with its location set; null if it cannot be
     * read, with the reason in {@link #failures}. Only a missing file and invalid content are
     * remembered, a failed read (e.g. a network problem) is tried again the next time.
     */
    private SBMLDocument load(URI source) {
        if (documents.containsKey(source)) {
            return documents.get(source);
        }
        if (failures.containsKey(source)) {
            return null;
        }
        String reason;
        boolean permanent = true;
        try {
            byte[] content = read(source);
            if (!SBMLUtil.isSBML(new ByteArrayInputStream(content))) {
                throw new XMLStreamException("the root element is no sbml element");
            }
            md5s.put(source, md5(content));
            SBMLDocument loaded = new SBMLReader().readSBMLFromStream(new ByteArrayInputStream(content));
            loaded.setLocationURI(source.toString());
            documents.put(source, loaded);
            ownDocuments.add(loaded);
            return loaded;
        } catch (NoSuchFileException e) {
            reason = String.format("The file %s does not exist.", source);
        } catch (IOException e) {
            reason = String.format("%s could not be read: %s", source, e.getMessage());
            permanent = false;
        } catch (XMLStreamException | RuntimeException e) {
            // JSBML throws runtime exceptions for some invalid SBML
            reason = String.format("%s is no valid SBML: %s", source, e.getMessage());
        }
        logger.warn(reason);
        if (permanent) {
            failures.put(source, reason);
        } else {
            lastFailure = reason;
        }
        return null;
    }

    private byte[] read(URI source) throws IOException {
        if ("file".equalsIgnoreCase(source.getScheme())) {
            try {
                return Files.readAllBytes(Path.of(source));
            } catch (IllegalArgumentException e) {
                // e.g. a file URI with an authority (file://server/share/model.xml)
                throw new IOException("unsupported file URI", e);
            }
        }
        if (http == null) {
            http = HttpJson.createDefault();
        }
        Path file = Files.createTempFile("cy3sbml-external", ".xml");
        try {
            http.download(source, file);
            return Files.readAllBytes(file);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    /**
     * Logs once per definition if the md5 checksum of the source differs from the one in
     * the definition. A source that was not read from its file (this document or an open
     * document) has no checksum and is not checked.
     */
    private void checkMd5(ExternalModelDefinition definition, URI source) {
        if (!md5Checked.add(definition)) {
            return;
        }
        String md5 = md5s.get(source);
        if (md5 == null) {
            logger.debug("No md5 checksum of {} to check, it was not read from its file.", source);
        } else if (!definition.getMd5().equalsIgnoreCase(md5)) {
            logger.warn(
                    "The md5 checksum of {} is {}, not {} as the external model definition '{}' says. "
                            + "The file may have changed.",
                    source,
                    md5,
                    definition.getMd5(),
                    definition.getId());
        }
    }

    private static String md5(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(content));
        } catch (NoSuchAlgorithmException e) {
            // every Java platform supports MD5
            throw new IllegalStateException(e);
        }
    }

    private static CompSBMLDocumentPlugin compPlugin(SBMLDocument document) {
        return document.getExtension(CompConstants.shortLabel) instanceof CompSBMLDocumentPlugin plugin ? plugin : null;
    }

    private static String name(SBMLDocument document) {
        return document.isSetLocationURI() ? document.getLocationURI() : "the document";
    }
}
