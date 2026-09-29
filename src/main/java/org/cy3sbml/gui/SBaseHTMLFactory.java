package org.cy3sbml.gui;

import static org.cy3sbml.gui.GUIConstants.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamException;
import org.cy3sbml.chebi.ChebiAccess;
import org.cy3sbml.comp.CompTargets;
import org.cy3sbml.miriam.MiriamRegistry;
import org.cy3sbml.miriam.Namespace;
import org.cy3sbml.miriam.RegistryUtil;
import org.cy3sbml.miriam.Resource;
import org.cy3sbml.ols.OlsClient;
import org.cy3sbml.ols.OlsTerm;
import org.cy3sbml.uniprot.UniprotAccess;
import org.cy3sbml.util.HtmlUtil;
import org.cy3sbml.util.IOUtil;
import org.cy3sbml.util.SBMLUtil;
import org.cy3sbml.util.XMLUtil;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.comp.SBaseRef;
import org.sbml.jsbml.ext.comp.Submodel;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.qual.Input;
import org.sbml.jsbml.ext.qual.Output;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.util.StringTools;
import org.sbml.jsbml.xml.XMLNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates HTML information for given SBase.
 * Core information is parsed from the NamedSBase object,
 * with additional information like resources retrieved via
 * web services (MIRIAM).
 * <p>
 * Here the HTML information string is created which is displayed
 * on selection of SBML objects in the graph.
 * <p>
 */
public class SBaseHTMLFactory {
    public static final String SBO = "SBO";
    public static final String CY3SBML = "cy3sbml";
    private static final Logger logger = LoggerFactory.getLogger(SBaseHTMLFactory.class);
    public static final transient String IDENTIFIERS_BASE = "https://identifiers.org/";
    public static final String FILENAME_NAMESPACE = "identifiersOrgNamespace.txt";
    public static final String delim = "/";

    private final String baseDir;
    private final MiriamRegistry miriamRegistry;
    private final OlsClient olsClient;
    private final UniprotAccess uniprotAccess;
    private final ChebiAccess chebiAccess;
    // the comp references of the open documents, null if not known
    private final CompTargets compTargets;

    /**
     * Creates the factory.
     *
     * @param baseDir base URL of the gui resources, which resolves the relative resources within the WebView
     * @param miriamRegistry data collections for resolving annotation URIs
     * @param olsClient resolves ontology terms for display
     * @param uniprotAccess resolves UniProt accessions for display
     * @param chebiAccess resolves ChEBI ids for display
     */
    public SBaseHTMLFactory(
            String baseDir,
            MiriamRegistry miriamRegistry,
            OlsClient olsClient,
            UniprotAccess uniprotAccess,
            ChebiAccess chebiAccess) {
        this(baseDir, miriamRegistry, olsClient, uniprotAccess, chebiAccess, null);
    }

    /**
     * Creates the factory, which shows the targets of the comp references.
     *
     * @param compTargets the comp references of the open documents, may be null
     */
    public SBaseHTMLFactory(
            String baseDir,
            MiriamRegistry miriamRegistry,
            OlsClient olsClient,
            UniprotAccess uniprotAccess,
            ChebiAccess chebiAccess,
            CompTargets compTargets) {
        this.baseDir = baseDir;
        this.miriamRegistry = miriamRegistry;
        this.olsClient = olsClient;
        this.uniprotAccess = uniprotAccess;
        this.chebiAccess = chebiAccess;
        this.compTargets = compTargets;
    }

    /**
     * Returns the base URL of the gui resources in the given application directory.
     */
    public static String baseDirFromAppDir(File appDir) {
        String baseDir = appDir.toURI().toString();
        baseDir = baseDir.replace("file:/", "file:///");
        return baseDir + "gui/";
    }

    /**
     * Creates HTML for given text String.
     */
    public String createHTMLText(String text, String title) {
        return String.format(HTML_START_TEMPLATE, baseDir, title) + text + HTML_STOP_TEMPLATE;
    }

    /**
     * Creates HTML text.
     */
    public String createHTMLText(String text) {
        return createHTMLText(text, CY3SBML);
    }

    /**
     * Creates the HTML information for the given SBase.
     */
    public String createInfo(SBase sbase) throws IOException {
        String title = getTitle(sbase);
        String html = String.format(HTML_START_TEMPLATE, baseDir, title);

        html += createInfoForSBase(sbase);
        if (sbase instanceof SBMLDocument doc) {
            // in case of SBMLDocument add the model information
            if (doc.isSetModel()) {
                html += createInfoForSBase(doc.getModel());
            }
        }
        html += HTML_STOP_TEMPLATE;
        return html;
    }

    /**
     * Creates info for given SBase.
     */
    private String createInfoForSBase(SBase sbase) throws IOException {
        if (sbase == null) {
            return "";
        }

        String html = createHeader(sbase);
        html += createSBase(sbase);
        html += createHistory(sbase);
        html += createCVTerms(sbase);
        html += createNonRDFAnnotation(sbase);
        html += createNotes(sbase);
        return html;
    }

    /**
     * Create title string for given SBase.
     */
    private static String getTitle(SBase sbase) {
        // handle SBMLDocument case
        if (sbase instanceof SBMLDocument) {
            SBMLDocument doc = (SBMLDocument) sbase;
            if (doc.isSetModel()) {
                sbase = doc.getModel();
            }
        }

        // title from class and id
        String id = "";
        if (NamedSBase.class.isAssignableFrom(sbase.getClass())) {
            NamedSBase nsb = (NamedSBase) sbase;
            if (nsb.isSetId()) {
                id = nsb.getId();
            }
        }
        return String.format("%s %s", id, SBMLUtil.getUnqualifiedClassName(sbase));
    }

    /**
     * Creates header HTML.
     * Displays class information, in addition id and name if existing.
     */
    private static String createHeader(SBase sbase) {
        String className = SBMLUtil.getUnqualifiedClassName(sbase);
        String header = MessageFormat.format("<h2>{0}{1}</h2>\n", EXPORT_HTML, className);

        // if NamedSBase get additional information
        if (NamedSBase.class.isAssignableFrom(sbase.getClass())) {
            String exportHTML = EXPORT_HTML;
            // already added via SBMLDocument
            if (sbase instanceof Model) {
                exportHTML = "";
            }
            NamedSBase nsb = (NamedSBase) sbase;
            header = MessageFormat.format("<h2>{0}{1} <small>{2}</small></h2>\n", exportHTML, className, nsb.getId());
        }
        return header;
    }

    /**
     * Create History HTML.
     * The history encodes information about the creator(s) of the encoding and a
     * sequence of dates recording the dates of creation and subsequent modifcations of the SBML model encoding.
     *
     * @return HTML String of History
     */
    // reason: the JSBML History API returns java.util.Date
    @SuppressWarnings("JavaUtilDate")
    private static String createHistory(SBase sbase) {
        if (!sbase.isSetHistory()) {
            return "";
        }

        String html = "<p class=\"cvterm\">";
        History h = sbase.getHistory();
        for (Creator c : h.getListOfCreators()) {
            String givenName = c.isSetGivenName() ? c.getGivenName() : "";
            String familyName = c.isSetFamilyName() ? c.getFamilyName() : "";
            String organisation = c.isSetOrganisation() ? String.format(", %s", c.getOrganisation()) : "";
            String email = "";
            if (c.isSetEmail()) {
                email = EMAIL_LINK.replace("{email}", c.getEmail());
            }
            html += MessageFormat.format("{0} {1} {2}{3}</br>\n", givenName, familyName, email, organisation);
        }
        if (h.isSetCreatedDate()) {
            html += CREATED_DATE1.replace("{date}", h.getCreatedDate().toString());
        }
        if (h.isSetListOfModification()) {
            for (Date date : h.getListOfModifiedDates()) {
                html += MODIFIED_DATE.replace("{date}", date.toString());
            }
        }
        html += "</p>\n";
        return html;
    }

    /**
     * Creates the HTML table from map.
     */
    private static String createTableFromMap(Map<String, String> map) {
        if (map == null || map.size() == 0) {
            return "";
        }
        String html = TABLE_START;
        for (String key : map.keySet()) {
            html += TS + key + TM + map.get(key) + TE;
        }
        return html + TABLE_END;
    }

    /**
     * Creation of class specific attribute information.
     * This mimics the SBMLReaderTaskFactory
     */
    private String createSBase(SBase item) {
        Map<String, String> map;

        // core //
        if (item instanceof SBMLDocument sbmlDocument) {
            map = SBMLUtil.createSBMLDocumentMap(sbmlDocument);
            map.putAll(SBMLUtil.createCompDocumentMap(sbmlDocument, compTargets));
        } else if (item instanceof Model model) {
            map = SBMLUtil.createModelMap(model);
        } else if (item instanceof Compartment compartment) {
            map = SBMLUtil.createCompartmentMap(compartment);
        } else if (item instanceof Parameter parameter) {
            map = SBMLUtil.createParameterMap(parameter);
        } else if (item instanceof InitialAssignment initialAssignment) {
            map = SBMLUtil.createInitialAssignmentMap(initialAssignment);
        } else if (item instanceof Rule rule) {
            map = SBMLUtil.createRuleMap(rule);
        } else if (item instanceof LocalParameter localParameter) {
            map = SBMLUtil.createLocalParameterMap(localParameter);
        } else if (item instanceof Species species) {
            map = SBMLUtil.createSpeciesMap(species);
        } else if (item instanceof Reaction reaction) {
            map = SBMLUtil.createReactionMap(reaction);
        } else if (item instanceof KineticLaw kineticLaw) {
            map = SBMLUtil.createKineticLawMap(kineticLaw);
        } else if (item instanceof FunctionDefinition functionDefinition) {
            map = SBMLUtil.createFunctionDefinitionMap(functionDefinition);
        } else if (item instanceof UnitDefinition unitDefinition) {
            map = SBMLUtil.createUnitDefinitionMap(unitDefinition);
        } else if (item instanceof Unit unit) {
            map = SBMLUtil.createUnitMap(unit);
        } else if (item instanceof Constraint constraint) {
            map = SBMLUtil.createConstraintMap(constraint);
        } else if (item instanceof Event event) {
            map = SBMLUtil.createEventMap(event);
        } else if (item instanceof EventAssignment eventAssignment) {
            map = SBMLUtil.createEventAssignmentMap(eventAssignment);
        }

        // qual //
        else if (item instanceof QualitativeSpecies qualitativeSpecies) {
            map = SBMLUtil.createQualitativeSpeciesMap(qualitativeSpecies);
        } else if (item instanceof Transition transition) {
            map = SBMLUtil.createTransitionMap(transition);
        } else if (item instanceof Input input) {
            map = SBMLUtil.createInputMap(input);
        } else if (item instanceof Output output) {
            map = SBMLUtil.createOutputMap(output);
        }

        // fbc //
        else if (item instanceof GeneProduct geneProduct) {
            map = SBMLUtil.createGeneProductMap(geneProduct);
        }

        // comp //
        else if (item instanceof Submodel submodel) {
            map = SBMLUtil.createSubmodelMap(submodel, compTargets);
        } else if (item instanceof SBaseRef ref) {
            // port, deletion, replaced element, replaced by
            map = SBMLUtil.createSBaseRefMap(ref, compTargets);
        }

        // group //
        else if (item instanceof Group group) {
            map = SBMLUtil.createGroupMap(group);
        }

        // Not supported
        else {
            logger.warn(MessageFormat.format(
                    "No object map support for {0} <{1}>", SBMLUtil.getUnqualifiedClassName(item), item));
            if (item instanceof NamedSBase namedSBase) {
                map = SBMLUtil.createNamedSBaseMap(namedSBase);
            } else {
                map = SBMLUtil.createSBaseMap(item);
            }
        }
        return createTableFromMap(map);
    }

    /**
     * Create HTML for CVTerms.
     */
    private String createCVTerms(SBase sbase) throws IOException {
        String text = "";
        for (CVTerm term : cvTermsForDisplay(sbase)) {
            text += createCVTerm(term);
        }
        return text;
    }

    /**
     * The CVTerms of the SBase, preceded by a CVTerm for its SBO term if none of them refers
     * to it. Rendering runs concurrently with other readers of the document (e.g. saving the
     * session), so this never changes the SBase: the SBO CVTerm is only part of the returned
     * list.
     */
    private static List<CVTerm> cvTermsForDisplay(SBase sbase) {
        List<CVTerm> cvterms = sbase.isSetAnnotation() ? sbase.getCVTerms() : List.of();
        List<CVTerm> terms = new ArrayList<>(cvterms.size() + 1);
        if (sbase.isSetSBOTerm()) {
            String sboTermId = sbase.getSBOTermID();
            boolean termExists =
                    cvterms.stream().flatMap(t -> t.getResources().stream()).anyMatch(uri -> uri.endsWith(sboTermId));
            if (!termExists) {
                String nameSpace = getPrefixValue(SBO);
                terms.add(new CVTerm(
                        CVTerm.Qualifier.BQB_IS,
                        String.valueOf(StringTools.concat(IDENTIFIERS_BASE, nameSpace, delim, sboTermId))));
            }
        }
        terms.addAll(cvterms);
        return terms;
    }

    /**
     * Creates HTML for single CVTerm.
     */
    private String createCVTerm(CVTerm cvterm) throws IOException {

        // get the biological/model qualifier type
        CVTerm.Qualifier bmQualifierType = null;
        if (cvterm.isModelQualifier()) {
            bmQualifierType = cvterm.getModelQualifierType();
        } else if (cvterm.isBiologicalQualifier()) {
            bmQualifierType = cvterm.getBiologicalQualifierType();
        }

        String text = "";

        String qualifierHTML = String.format("""
                <p class="cvterm">
                \t<span class="qualifier" title="%s">%s</span>
                """, cvterm.getQualifierType(), bmQualifierType);

        Namespace dataType = null;
        // List of Resource URIs

        for (String resourceURI : cvterm.getResources()) {
            if (RegistryUtil.isIdentifiersURI(resourceURI)) {
                // bugfix to handle https://identifier.org/ resources
                resourceURI = resourceURI.replace("https://identifiers.org", "http://identifiers.org");
                String dataCollection = RegistryUtil.getDataCollectionPartFromURI(resourceURI);
                MiriamRegistry.ResolvedURI resolved = miriamRegistry.resolve(resourceURI);
                dataType = resolved == null ? null : resolved.dataCollection();

                String identifier = resolved == null ? null : resolved.identifier();
                if (identifier == null) {
                    identifier = substringAfter(resourceURI, "http://identifiers.org/");
                }
                // link to primary resource via id
                Resource primaryResource = dataType == null ? null : dataType.getPrimaryResource();
                String resourceLink =
                        primaryResource == null ? resourceURI : createURL(dataType, primaryResource, identifier);

                // identifier
                String identifierHTML =
                        IDENTIFIER_LINK.replace("{resourceLink}", resourceLink).replace("{identifier}", identifier);

                // not possible to resolve dataType from MIRIAM registry
                if (dataType == null) {
                    logger.warn(MessageFormat.format(
                            "DataType could not be retrieved for data collection part: <{0}>", dataCollection));
                    text += UNKNOWN_DATA_COLLECTION
                            .replace("{qualifierHTML}", qualifierHTML)
                            .replace("{identifierHTML}", identifierHTML)
                            .replace("{ICON_WARNING}", ICON_WARNING)
                            .replace("{dataCollectionURL}", dataCollection)
                            .replace("{dataCollectionID}", dataCollection);

                    text += INVISIBLE_RESOURCE_LINK
                            .replace("{ICON_INVISIBLE}", ICON_INVISIBLE)
                            .replace("{resourceURI}", resourceURI);
                }
                // dataType found
                if (dataType != null) {
                    String dataTypeURL = primaryResource == null ? resourceURI : primaryResource.getResourceHomeUrl();
                    text += qualifierHTML
                            + MIRIAM_COLLECTION_LINK
                                    .replace("{dataTypeURL}", dataTypeURL)
                                    .replace("{dataTypeName}", dataType.getName())
                                    .replace("{identifierHTML}", identifierHTML);

                    // check that identifier is correct for given datatype
                    String pattern = dataType.getPattern();
                    if (!RegistryUtil.checkRegexp(identifier, pattern)) {
                        logger.warn(MessageFormat.format(
                                "Identifier <{0}> does not match pattern <{1}> of data collection: <{2}>",
                                identifier, pattern, dataType.getId()));
                        text += IDENTIFIER_PATTERN_MISMATCH
                                .replace("{ICON_WARNING}", ICON_WARNING)
                                .replace("{identifier}", identifier)
                                .replace("{pattern}", pattern);
                    }

                    // Create OLS resource for location
                    for (Resource resource : dataType.getResources()) {
                        if (resource.isDeprecated()) {
                            continue;
                        }
                        if (OlsClient.isOlsResource(resource)) {

                            text += createOLSLocation(dataType, resource, identifier);
                        }
                    }
                    // Create other locations
                    for (Resource resource : dataType.getResources()) {
                        if (resource.isDeprecated()) {
                            continue;
                        }
                        if (!OlsClient.isOlsResource(resource)) {

                            text += createNonOLSLocation(dataType, resource, identifier);
                        }
                    }

                    // add secondary information
                    text += createSecondaryInformation(dataType, identifier);
                }
                text += "</p>\n";
            }
        }
        return text;
    }

    /**
     * The text after the first occurrence of the separator, empty if it does not occur.
     */
    private static String substringAfter(String text, String separator) {
        int index = text.indexOf(separator);
        return index < 0 ? "" : text.substring(index + separator.length());
    }

    /**
     * Creates the URL for the given location and identifier.
     */
    private static String createURL(Namespace namespace, Resource resource, String identifier) {
        String url;
        String identifier2;
        if (Boolean.TRUE.equals(namespace.getNamespaceEmbeddedInLui()) && identifier.contains(":")) {
            identifier2 = substringAfter(identifier, ":");
        } else {
            identifier2 = identifier;
        }
        url = resource.getUrlPattern().replace("{$id}", identifier2);

        return url;
    }

    /**
     * Information for non-OLS location.
     */
    private static String createNonOLSLocation(Namespace namespace, Resource resource, String identifier) {

        String info = resource.getDescription();

        return String.format("\t<a href=\"%s\"> %s</a><br />\n", createURL(namespace, resource, identifier), info);
    }

    /**
     * Information for an OLS location: the term shown by the OLS term page for the identifier.
     */
    private String createOLSLocation(Namespace namespace, Resource resource, String identifier) {
        String html = "";
        String olsURL = createURL(namespace, resource, identifier);
        Optional<OlsTerm> optionalTerm = olsClient.termForPage(olsURL);

        if (optionalTerm.isPresent()) {
            OlsTerm term = optionalTerm.get();

            String purlURL = term.iri();
            html += ONTOLOGY_TERM_LINK
                    .replace("{ontologyURL}", olsURL)
                    .replace("{ontologyName}", term.ontologyName().toUpperCase(Locale.ROOT))
                    .replace("{termLabel}", ontologyTextHTML(term.label()))
                    .replace("{purlURL}", purlURL)
                    .replace("{purlDisplay}", purlURL);

            List<String> synonyms = term.synonyms();
            if (synonyms != null && !synonyms.isEmpty()) {
                html += SYNONYMS_LABEL;
                for (String syn : synonyms) {
                    html += String.format("%s; ", ontologyTextHTML(syn));
                }
                html += "<br />\n";
            }
            List<String> descriptions = term.descriptions();
            if (descriptions != null && !descriptions.isEmpty()) {
                for (String description : descriptions) {
                    html += DESCRIPTION_LABEL.replace("{DESCRIPTION}", ontologyTextHTML(description));
                }
            }
        } else {
            html += OLS_TERM_ERROR
                    .replace("{ICON_WARNING}", ICON_WARNING)
                    .replace("{TERM_ID}", identifier)
                    .replace("{OLS_URL}", olsURL);
            html += createNonOLSLocation(namespace, resource, identifier);
        }
        return html;
    }

    /** Inline formatting tags without attributes that ontology texts use, e.g. {@code <small>D</small>}. */
    private static final String INLINE_TAGS = "sub|sup|small|i|em|b";

    /**
     * An escaped pair of the same inline tag around content without escaped inline tags;
     * restored tags in the content are allowed, so nested pairs are restored inside out.
     */
    private static final Pattern ESCAPED_INLINE_PAIR = Pattern.compile("&lt;(" + INLINE_TAGS + ")&gt;"
            + "((?:(?!&lt;/?(?:" + INLINE_TAGS + ")&gt;)[^<]|<[^>]*>)*?)"
            + "&lt;/\\1&gt;");

    /**
     * HTML for a text from an ontology term (label, synonym or description).
     * The text is escaped, matched pairs of inline formatting tags such as sub, sup and small
     * are kept; unbalanced or crossing tags stay escaped.
     */
    static String ontologyTextHTML(String text) {
        String html = HtmlUtil.escape(text);
        while (true) {
            String restored = ESCAPED_INLINE_PAIR.matcher(html).replaceAll("<$1>$2</$1>");
            if (restored.equals(html)) {
                return html;
            }
            html = restored;
        }
    }

    /**
     * Resolves secondary resourses and returns the HTML.
     *
     * @return html string
     */
    public String createSecondaryInformation(Namespace dataType, String identifier) {
        String prefix = dataType.getPrefix();
        if (prefix == null) {
            return "";
        }
        return switch (prefix) {
            case "uniprot" -> uniprotAccess.html(identifier);
            case "chebi" -> chebiAccess.html(identifier);
            default -> "";
        };
    }

    /**
     * Create non-RDF annotation XML.
     * This is for instance used to process the SABIO-RK data.
     * Parses all the information in the annotation xml which is not RDF CV-Terms.
     */
    static String createNonRDFAnnotation(SBase sbase) {
        String html = "";
        if (sbase.isSetAnnotation()) {
            Annotation annotation = sbase.getAnnotation();
            String text = "";
            XMLNode xmlNode = annotation.getNonRDFannotation();
            if (xmlNode != null) {
                // get all children which are not RDF
                for (int i = 0; i < xmlNode.getChildCount(); i++) {
                    XMLNode child = xmlNode.getChildAt(i);
                    String name = child.getName();
                    if (!"RDF".equals(name)) {
                        try {
                            String xml = XMLNode.convertXMLNodeToString(child);
                            // Handle special case of whitespaces/empty text nodes
                            xml = xml.trim();
                            if (xml.length() > 0) {
                                xml = XMLUtil.xml2Html(xml);
                                if (xml != null) {
                                    text += xml;
                                } else {
                                    logger.error("Annotation XML could not be parsed.");
                                }
                            }
                        } catch (XMLStreamException e) {
                            logger.error("Error parsing annotation xml", e);
                        }
                    }
                }
            }

            // move into <code> tag for display
            if (text.length() > 0) {
                html = String.format("<code>%s</code>", text);
            }
        }
        return html;
    }

    /**
     * Create HMTL for notes.
     */
    private static String createNotes(SBase sbase) {
        String notes = SBMLUtil.parseNotes(sbase);
        if (notes != null) {
            return String.format("""
                                 <hr />
                                 <div id="notes">
                                 %s
                                 </div>
                                 """, notes);
        }
        return "";
    }

    /**
     * Creates true or false HTML depending on boolean.
     */
    public static String booleanHTML(boolean b) {
        return b ? ICON_TRUE : ICON_FALSE;
    }

    public static String getPrefixValue(String keyToFind) {
        InputStream inputStream = IOUtil.readResource("/gui/" + FILENAME_NAMESPACE);
        if (inputStream == null) {
            logger.error("Could not find the namespace resource: {}", FILENAME_NAMESPACE);
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Properties namespaces = new Properties();
            namespaces.load(reader);
            return namespaces.getProperty(keyToFind);
        } catch (IOException e) {
            logger.error("Could not read the prefix value: {}", keyToFind, e);
        }
        return null;
    }
}
