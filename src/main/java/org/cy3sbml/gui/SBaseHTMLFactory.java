package org.cy3sbml.gui;

import static org.cy3sbml.gui.GUIConstants.*;
import static org.cy3sbml.miriam.RegistryUtil.getMiriamContent;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.StringEscapeUtils;
import org.cy3sbml.chebi.ChebiAccess;
import org.cy3sbml.miriam.Namespace;
import org.cy3sbml.miriam.RegistryUtil;
import org.cy3sbml.miriam.Resource;
import org.cy3sbml.ols.OlsClient;
import org.cy3sbml.ols.OlsTerm;
import org.cy3sbml.uniprot.UniprotAccess;
import org.cy3sbml.util.IOUtil;
import org.cy3sbml.util.SBMLUtil;
import org.cy3sbml.util.XMLUtil;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.groups.Group;
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
    private static String baseDir;
    public static final transient String IDENTIFIERS_BASE = "https://identifiers.org/";
    public static final String FILENAME_NAMESPACE = "identifiersOrgNamespace.txt";
    public static String delim = "/";
    public static final Map<String, Namespace> result = getMiriamContent();

    /**
     * OLS client used to resolve ontology terms for display.
     * Set by {@code CyActivator} on startup. A static field for now;
     * PR 3 turns it into an instance dependency.
     */
    private static OlsClient olsClient;

    /**
     * UniProt client used to resolve UniProt accessions for display.
     * Set by {@code CyActivator} on startup. A static field for now;
     * PR 3 turns it into an instance dependency.
     */
    private static UniprotAccess uniprotAccess;

    /**
     * ChEBI client used to resolve ChEBI ids for display.
     * Set by {@code CyActivator} on startup. A static field for now;
     * PR 3 turns it into an instance dependency.
     */
    private static ChebiAccess chebiAccess;

    private SBase sbase;
    private String html;

    /**
     * Constructor.
     */
    public SBaseHTMLFactory(Object obj) {
        sbase = (SBase) obj;
    }

    /**
     * Sets the baseDir for the HTML document.
     * This is used to find relative resources within the WebView.
     */
    public static void setBaseDir(String baseDir) {
        SBaseHTMLFactory.baseDir = baseDir;
    }

    /**
     * Get the html base directory.
     */
    public static String getBaseDir() {
        return baseDir;
    }

    /**
     * Sets the OLS client used to resolve ontology terms for display.
     */
    public static void setOlsClient(OlsClient olsClient) {
        SBaseHTMLFactory.olsClient = olsClient;
    }

    /**
     * Gets the OLS client used to resolve ontology terms for display,
     * lazily creating a default one (e.g. for tests that do not run
     * {@code CyActivator}). Keeps a single shared instance.
     */
    private static synchronized OlsClient getOlsClient() {
        if (olsClient == null) {
            olsClient = new OlsClient(org.cy3sbml.util.HttpJson.createDefault());
        }
        return olsClient;
    }

    /**
     * Sets the UniProt client used to resolve UniProt accessions for display.
     */
    public static void setUniprotAccess(UniprotAccess uniprotAccess) {
        SBaseHTMLFactory.uniprotAccess = uniprotAccess;
    }

    /**
     * Gets the UniProt client used to resolve UniProt accessions for display,
     * lazily creating a default one (e.g. for tests that do not run
     * {@code CyActivator}). Keeps a single shared instance.
     */
    private static synchronized UniprotAccess getUniprotAccess() {
        if (uniprotAccess == null) {
            uniprotAccess = new UniprotAccess(org.cy3sbml.util.HttpJson.createDefault());
        }
        return uniprotAccess;
    }

    /**
     * Sets the ChEBI client used to resolve ChEBI ids for display.
     */
    public static void setChebiAccess(ChebiAccess chebiAccess) {
        SBaseHTMLFactory.chebiAccess = chebiAccess;
    }

    /**
     * Gets the ChEBI client used to resolve ChEBI ids for display,
     * lazily creating a default one (e.g. for tests that do not run
     * {@code CyActivator}). Keeps a single shared instance.
     */
    private static synchronized ChebiAccess getChebiAccess() {
        if (chebiAccess == null) {
            chebiAccess = new ChebiAccess(org.cy3sbml.util.HttpJson.createDefault());
        }
        return chebiAccess;
    }

    /**
     * Sets the baseDir from the application directory.
     */
    public static void setBaseDirFromAppDir(File appDir) {
        String baseDir = appDir.toURI().toString();
        baseDir = baseDir.replace("file:/", "file:///");
        SBaseHTMLFactory.setBaseDir(baseDir + "gui/");
    }

    /**
     * Get created information string.
     * No information String created in cache mode.
     */
    public String getHtml() {
        return html;
    }

    /**
     * Creates HTML for given text String.
     */
    public static String createHTMLText(String text, String title) {
        return HTML_START_TEMPLATE.replace("{baseHref}", baseDir).replace("pageTitle", title)
                + text
                + HTML_STOP_TEMPLATE;
    }

    /**
     * Creates HTML text.
     */
    public static String createHTMLText(String text) {
        return createHTMLText(text, CY3SBML);
    }

    /**
     * Parse and create information for current Sbase.
     */
    public void createInfo() throws IOException {
        String title = getTitle(sbase);
        html = String.format(HTML_START_TEMPLATE, baseDir, title);

        html += createInfoForSBase(sbase);
        if (sbase instanceof SBMLDocument) {
            // in case of SBMLDocument add the model information
            SBMLDocument doc = (SBMLDocument) sbase;
            if (doc.isSetModel()) {
                html += createInfoForSBase(doc.getModel());
            }
        }
        html += HTML_STOP_TEMPLATE;
    }

    /**
     * Creates info for given SBase.
     */
    private static String createInfoForSBase(SBase sbase) throws IOException {
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
    private static String createSBase(SBase item) {
        Map<String, String> map;

        // core //
        if (item instanceof SBMLDocument sbmlDocument) {
            map = SBMLUtil.createSBMLDocumentMap(sbmlDocument);
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
        }

        // fbc //
        else if (item instanceof GeneProduct geneProduct) {
            map = SBMLUtil.createGeneProductMap(geneProduct);
        }

        // comp //
        else if (item instanceof Port port) {
            map = SBMLUtil.createPortMap(port);
        }

        // group //
        else if (item instanceof Group group) {
            map = SBMLUtil.createGroupMap(group);
        }

        // Not supported
        else {
            logger.warn(MessageFormat.format(
                    "No object map support for {0} <{1}>", SBMLUtil.getUnqualifiedClassName(item)));
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
    private static String createCVTerms(SBase sbase) throws IOException {
        List<CVTerm> cvterms = sbase.getCVTerms();
        // Handle SBO
        addCVTermForSBO(sbase);

        // Create HTML
        String text = "";
        if (cvterms.size() > 0) {
            for (CVTerm term : cvterms) {
                text += createCVTerm(term);
            }
        }
        return text;
    }

    /**
     * Adds the CVTerm for SBO to the CVTerms.
     */
    private static void addCVTermForSBO(SBase sbase) {
        List<CVTerm> cvterms = sbase.getCVTerms();

        // add the SBO term to the annotations if not existing already
        if (sbase.isSetSBOTerm()) {
            String nameSpace = getPrefixValue(SBO);

            String sboTermId = sbase.getSBOTermID();

            CVTerm term = new CVTerm(
                    CVTerm.Qualifier.BQB_IS,
                    String.valueOf(StringTools.concat(IDENTIFIERS_BASE, nameSpace, delim, sboTermId)));

            Boolean termExists = false;
            outerloop:
            for (CVTerm t : cvterms) {
                for (String uri : t.getResources()) {
                    if (uri.endsWith(sboTermId)) {
                        termExists = true;
                        break outerloop;
                    }
                }
            }
            if (!termExists) {
                cvterms.add(0, term);
            }
        }
    }

    /**
     * Creates HTML for single CVTerm.
     */
    private static String createCVTerm(CVTerm cvterm) throws IOException {

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
            // bugfix to handle https://identifier.org/ resources
            if (resourceURI.contains("identifiers.org")) {
                resourceURI = resourceURI.replace("https://identifiers.org", "http://identifiers.org");
                String[] tokens = resourceURI.split("/");
                String compactIdentifier = getCompactId(tokens);

                String dataCollection = RegistryUtil.getDataCollectionPartFromURI(resourceURI);
                String prefix =
                        StringUtils.substringBefore(compactIdentifier, ":").toLowerCase(Locale.ROOT);
                if (result.get(prefix) == null && tokens.length > 3) {
                    prefix = tokens[3].toLowerCase(Locale.ROOT);
                }
                dataType = (result.get(prefix) == null)
                        ? result.get(StringUtils.substringAfter(prefix, "."))
                        : result.get(prefix);

                String identifier = RegistryUtil.getIdentifierFromURI(resourceURI);
                if (identifier == null) {
                    identifier = StringUtils.substringAfter(resourceURI, "http://identifiers.org/");
                }
                // link to primary resource via id
                String resourceLink = null;

                if (dataType == null) {
                    resourceLink = resourceURI;

                } else {
                    for (Resource resource : dataType.getResources()) {
                        // take first one
                        resourceLink = createURL(dataType, resource, identifier);

                        break;
                    }
                }

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
                    text += qualifierHTML
                            + MIRIAM_COLLECTION_LINK
                                    .replace(
                                            "{dataTypeURL}",
                                            dataType.getResources().get(0).getResourceHomeUrl())
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
     * Creates the URL for the given location and identifier.
     */
    private static String createURL(Namespace namespace, Resource resource, String identifier) {
        String url;
        String identifier2;
        if (Strings.CI.contains(identifier, namespace.getPrefix())) {
            identifier2 = StringUtils.substringAfter(identifier, ":");
        } else {
            identifier2 = identifier;
        }
        url = resource.getUrlPattern().replace("{$id}", identifier2);

        return url;
    }

    // FIXME: This is only a temporary solution for creating olsURLs for a variety of identifier prefixes

    /**
     * Information for non-OLS location.
     */
    private static String createNonOLSLocation(Namespace namespace, Resource resource, String identifier) {

        String info = resource.getDescription();

        return String.format("\t<a href=\"%s\"> %s</a><br />\n", createURL(namespace, resource, identifier), info);
    }

    /**
     * Information for an OLS location.
     * Only the identifier needed for the query.
     */
    private static String createOLSLocation(Namespace namespace, Resource resource, String identifier) {
        String html = "";
        // Necessary to get the OLS identifier from the OLS url, in case there are prefixes and suffixes

        String olsURL = createURL(namespace, resource, identifier);

        // for some ontologies the OLS term query term is not the identifier
        String termIdentifier = identifier;

        // the last non-empty token after the first one; a URL ending with "=" keeps the
        // identifier
        String[] tokens = olsURL.split("=", -1);
        int last = tokens.length - 1;
        while (last > 0 && tokens[last].isEmpty()) {
            last--;
        }
        if (last > 0) {
            termIdentifier = tokens[last];
        }

        Optional<OlsTerm> optionalTerm = getOlsClient().term(termIdentifier);

        if (optionalTerm.isPresent()) {
            OlsTerm term = optionalTerm.get();

            String purlURL = term.iri();
            String ontologyURL = createURL(namespace, resource, identifier);
            html += ONTOLOGY_TERM_LINK
                    .replace("{ontologyURL}", ontologyURL)
                    .replace("{ontologyName}", term.ontologyName().toUpperCase(Locale.ROOT))
                    .replace("{termLabel}", term.label())
                    .replace("{purlURL}", purlURL)
                    .replace("{purlDisplay}", purlURL);

            List<String> synonyms = term.synonyms();
            if (synonyms != null && !synonyms.isEmpty()) {
                html += SYNONYMS_LABEL;
                for (String syn : synonyms) {
                    html += String.format("%s; ", syn);
                }
                html += "<br />\n";
            }
            List<String> descriptions = term.descriptions();
            if (descriptions != null && !descriptions.isEmpty()) {
                for (String description : descriptions) {
                    html += DESCRIPTION_LABEL.replace("{DESCRIPTION}", StringEscapeUtils.escapeHtml4(description));
                }
            }
        } else {
            html += OLS_TERM_ERROR
                    .replace("{ICON_WARNING}", ICON_WARNING)
                    .replace("{TERM_ID}", termIdentifier)
                    .replace("{OLS_URL}", olsURL);
            ;
            html += createNonOLSLocation(namespace, resource, identifier);
        }
        return html;
    }

    /**
     * Resolves secondary resourses and returns the HTML.
     *
     * @return html string
     */
    public static String createSecondaryInformation(Namespace dataType, String identifier) {
        String html = "";
        String namespace = dataType.getPrefix();

        if (namespace.equals("uniprot")) {
            html += getUniprotAccess().html(identifier);
        } else if (namespace.equals("chebi")) {
            html += getChebiAccess().html(identifier);
        }

        return html;
    }

    /**
     * Creates compact identifier."
     */
    public static String getCompactId(String[] tokens) {

        String identifier;
        if (tokens[tokens.length - 1].contains(":")) { // format : identifiers.org/[namespace prefix]:[accession]
            identifier = tokens[tokens.length - 1];
        } else if (tokens[tokens.length - 1].contains("[!\"#$%&'()*+,\\-./;<=>?@[\\\\\\]^_`{|}~]")) {
            identifier = tokens[tokens.length - 1].replace("[!\"#$%&'()*+,\\-./;<=>?@[\\\\\\]^_`{|}~]", ":");
        } else {
            identifier = tokens[tokens.length - 2] + ":" + tokens[tokens.length - 1];
        }

        identifier = identifier.toUpperCase(Locale.ROOT);
        return identifier;
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
