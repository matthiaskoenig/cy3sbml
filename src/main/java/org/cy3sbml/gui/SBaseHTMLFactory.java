package org.cy3sbml.gui;

import java.io.*;
import java.text.MessageFormat;
import java.util.*;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import org.identifiers.registry.RegistryUtilities;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.groups.Group;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.util.StringTools;
import org.sbml.jsbml.xml.XMLNode;

import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.cy3sbml.miriam.Namespace;
import org.cy3sbml.miriam.RegistryUtil;
import org.cy3sbml.miriam.Resource;
import org.cy3sbml.ols.OLSAccess;
import org.cy3sbml.ols.OLSCache;
import org.cy3sbml.uniprot.UniprotAccess;
import org.cy3sbml.util.IOUtil;
import org.cy3sbml.util.XMLUtil;
import org.cy3sbml.util.SBMLUtil;

import static org.cy3sbml.gui.GUIConstants.*;
import static org.cy3sbml.miriam.RegistryUtil.getMiriamContent;


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
     * Sets the baseDir from the application directory.
     *
     * @param appDir
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
        return HTML_START_TEMPLATE.replace("{baseHref}", baseDir)
                .replace("pageTitle", title) + text + HTML_STOP_TEMPLATE;

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
     *
     * @param sbase
     * @return
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
     *
     * @param sbase
     * @return
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
        return String.format("%s %s",
                id, SBMLUtil.getUnqualifiedClassName(sbase));
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
            header = MessageFormat.format("<h2>{0}{1} <small>{2}</small></h2>\n",
                    exportHTML, className, nsb.getId());
        }
        return header;
    }


    /**
     * Create History HTML.
     * The history encodes information about the creator(s) of the encoding and a
     * sequence of dates recording the dates of creation and subsequent modifcations of the SBML model encoding.
     *
     * @param sbase
     * @return HTML String of History
     */
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
            html += MessageFormat.format("{0} {1} {2}{3}</br>\n",
                    givenName,
                    familyName,
                    email,
                    organisation);
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
        LinkedHashMap<String, String> map;

        // core //
        if (item instanceof SBMLDocument) {
            map = SBMLUtil.createSBMLDocumentMap((SBMLDocument) item);
        } else if (item instanceof Model) {
            map = SBMLUtil.createModelMap((Model) item);
        } else if (item instanceof Compartment) {
            map = SBMLUtil.createCompartmentMap((Compartment) item);
        } else if (item instanceof Parameter) {
            map = SBMLUtil.createParameterMap((Parameter) item);
        } else if (item instanceof InitialAssignment) {
            map = SBMLUtil.createInitialAssignmentMap((InitialAssignment) item);
        } else if (item instanceof Rule) {
            map = SBMLUtil.createRuleMap((Rule) item);
        } else if (item instanceof LocalParameter) {
            map = SBMLUtil.createLocalParameterMap((LocalParameter) item);
        } else if (item instanceof Species) {
            map = SBMLUtil.createSpeciesMap((Species) item);
        } else if (item instanceof Reaction) {
            map = SBMLUtil.createReactionMap((Reaction) item);
        } else if (item instanceof KineticLaw) {
            map = SBMLUtil.createKineticLawMap((KineticLaw) item);
        } else if (item instanceof FunctionDefinition) {
            map = SBMLUtil.createFunctionDefinitionMap((FunctionDefinition) item);
        } else if (item instanceof UnitDefinition) {
            map = SBMLUtil.createUnitDefinitionMap((UnitDefinition) item);
        } else if (item instanceof Unit) {
            map = SBMLUtil.createUnitMap((Unit) item);
        } else if (item instanceof Constraint) {
            map = SBMLUtil.createConstraintMap((Constraint) item);
        } else if (item instanceof Event) {
            map = SBMLUtil.createEventMap((Event) item);
        } else if (item instanceof EventAssignment) {
            map = SBMLUtil.createEventAssignmentMap((EventAssignment) item);
        }

        // qual //
        else if (item instanceof QualitativeSpecies) {
            map = SBMLUtil.createQualitativeSpeciesMap((QualitativeSpecies) item);
        } else if (item instanceof Transition) {
            map = SBMLUtil.createTransitionMap((Transition) item);
        }

        // fbc //
        else if (item instanceof GeneProduct) {
            map = SBMLUtil.createGeneProductMap((GeneProduct) item);
        }

        // comp //
        else if (item instanceof Port) {
            map = SBMLUtil.createPortMap((Port) item);
        }

        // group //
        else if (item instanceof Group) {
            map = SBMLUtil.createGroupMap((Group) item);
        }

        // Not supported
        else {
            logger.warn(MessageFormat.format(
                    "No object map support for {0} <{1}>",
                    SBMLUtil.getUnqualifiedClassName(item)));
            if (item instanceof NamedSBase) {
                map = SBMLUtil.createNamedSBaseMap((NamedSBase) item);
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

            CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS, String.valueOf(StringTools.concat(IDENTIFIERS_BASE, nameSpace, delim, sboTermId)));


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

        String qualifierHTML = String.format(
                "<p class=\"cvterm\">\n" +
                        "\t<span class=\"qualifier\" title=\"%s\">%s</span>\n",
                cvterm.getQualifierType(), bmQualifierType);


        Namespace dataType = null;
        // List of Resource URIs
        for (String resourceURI : cvterm.getResources()) {
            // bugfix to handle https://identifier.org/ resources
            resourceURI = resourceURI.replace("https://identifiers.org", "http://identifiers.org");
            String[] tokens = resourceURI.split("/");
            String compactIdentifier = getCompactId(tokens);

            String dataCollection = RegistryUtilities.getDataCollectionPartFromURI(resourceURI);
            String prefix = StringUtils.substringBefore(compactIdentifier, ":").toLowerCase();
            if (result.get(prefix) == null) {
                prefix = tokens[3].toLowerCase();
            }
            dataType = (result.get(prefix) == null)
                    ? result.get(StringUtils.substringAfter(prefix, "."))
                    : result.get(prefix);

            String identifier = RegistryUtilities.getIdentifierFromURI(resourceURI);
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


            String identifierHTML = IDENTIFIER_LINK
                    .replace("{resourceLink}", resourceLink)
                    .replace("{identifier}", identifier);


            // not possible to resolve dataType from MIRIAM registry
            if (dataType == null && resourceURI.contains("identifiers.org")) {
                logger.warn(MessageFormat.format(
                        "DataType could not be retrieved for data collection part: <{0}>",
                        dataCollection));
                text += UNKNOWN_DATA_COLLECTION.replace("{qualifierHTML}", qualifierHTML)
                        .replace("{identifierHTML}", identifierHTML)
                        .replace("{ICON_WARNING}", ICON_WARNING)
                        .replace("{dataCollectionURL}", dataCollection)
                        .replace("{dataCollectionID}", dataCollection);


                text += INVISIBLE_RESOURCE_LINK.replace("{ICON_INVISIBLE}", ICON_INVISIBLE)
                        .replace("{resourceURI}", resourceURI);
            }
            // dataType found
            if (dataType != null) {
                text += qualifierHTML + MIRIAM_COLLECTION_LINK
                        .replace("{dataTypeURL}", dataType.getResources().get(0).getResourceHomeUrl())
                        .replace("{dataTypeName}", dataType.getName())
                        .replace("{identifierHTML}", identifierHTML);

                // check that identifier is correct for given datatype
                String pattern = dataType.getPattern();
                if (!RegistryUtilities.checkRegexp(identifier, pattern)) {
                    logger.warn(MessageFormat.format(
                            "Identifier <{0}> does not match pattern <{1}> of data collection: <{2}>",
                            identifier,
                            pattern,
                            dataType.getId()
                    ));
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
                    if (OLSAccess.isPhysicalLocationOLS(resource)) {

                        text += createOLSLocation(dataType, resource, identifier);
                    }
                }
                // Create other locations
                for (Resource resource : dataType.getResources()) {
                    if (resource.isDeprecated()) {
                        continue;
                    }
                    if (!OLSAccess.isPhysicalLocationOLS(resource)) {

                        text += createNonOLSLocation(dataType, resource, identifier);
                    }
                }

                // add secondary information
                text += createSecondaryInformation(dataType, identifier);
            }
            text += "</p>\n";
        }
        return text;
    }

    /**
     * Creates the URL for the given location and identifier.
     *
     * @param identifier
     * @return
     */
    private static String createURL(Namespace namespace, Resource resource, String identifier) {
        String url;
        String identifier2;
        if (StringUtils.containsIgnoreCase(identifier, namespace.getPrefix())) {
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

        return String.format(
                "\t<a href=\"%s\"> %s</a><br />\n",
                createURL(namespace, resource, identifier),
                info);
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


        String[] tokens = olsURL.split("=");
        if (tokens.length > 1) {
            termIdentifier = tokens[tokens.length - 1];
        }

        Term term = OLSCache.getTerm(termIdentifier);

        if (term != null) {

            String purlURL = term.getIri().getIdentifier();
            String ontologyURL = createURL(namespace, resource, identifier);
            html += ONTOLOGY_TERM_LINK.replace("{ontologyURL}", ontologyURL)
                    .replace("{ontologyName}", term.getOntologyName().toUpperCase())
                    .replace("{termLabel}", term.getLabel())
                    .replace("{purlURL}", purlURL)
                    .replace("{purlDisplay}", purlURL);

            String[] synonyms = term.getSynonyms();
            if (synonyms != null && synonyms.length > 0) {
                html += SYNONYMS_LABEL;
                for (String syn : synonyms) {
                    html += String.format("%s; ", syn);
                }
                html += "<br />\n";
            }
            Map<String, String> oboSynonyms = term.getOboSynonyms();
            if (oboSynonyms != null && oboSynonyms.size() > 0) {
                html += OBO_SYNONYMS_LABEL;
                for (String name : oboSynonyms.keySet()) {
                    html += String.format("%s; ", name);
                }
                html += "<br />\n";
            }
            String[] descriptions = term.getDescription();
            if (descriptions != null && descriptions.length > 0) {
                for (String description : descriptions) {
                    html += DESCRIPTION_LABEL
                            .replace("{DESCRIPTION}", StringEscapeUtils.escapeHtml4(description));
                }
            }
        } else {
            html += OLS_TERM_ERROR.replace("{ICON_WARNING}", ICON_WARNING)
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
     * @param dataType
     * @param identifier
     * @return html string
     */
    public static String createSecondaryInformation(Namespace dataType, String identifier) {
        String html = "";
        String namespace = dataType.getPrefix();

        if (namespace.equals("uniprot")) {

            html += UniprotAccess.uniprotHTML(identifier);
        } else if (namespace.equals("chebi")) {
            html += chebiHTML(identifier);
        }

        return html;
    }

    /**
     * Creates additional chebi information for the entry.
     * Identifier is of form "CHEBI:28061"
     */
    public static String getCompactId(String[] tokens){

        String identifier;
        if (tokens[tokens.length - 1].contains(":")){ //format : identifiers.org/[namespace prefix]:[accession]
            identifier = tokens[tokens.length - 1];
        } else if (tokens[tokens.length - 1].contains("[!\"#$%&'()*+,\\-./;<=>?@[\\\\\\]^_`{|}~]")){
            identifier = tokens[tokens.length - 1].replace("[!\"#$%&'()*+,\\-./;<=>?@[\\\\\\]^_`{|}~]",":");
        }
        else {
            identifier = tokens[tokens.length - 2]+":"+tokens[tokens.length - 1];
        }

        identifier = identifier.toUpperCase();
        return identifier;
    }
    private static String chebiHTML(String identifier) {
        // Image
        String text = "";
        String[] tokens = identifier.split(":");
        String imageSource = String.format(
                "https://www.ebi.ac.uk/chebi/displayImage.do;?defaultImage=true&imageIndex=0&chebiId=%s&dimensions=200",
                tokens[1]);
        String imageLink = String.format(
                "https://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI:%s",
                tokens[1]);

        // Resolve additional webservice information
        // FIXME: this is not working in OSGI bundle
        /*
        Entity entity = ChebiAccess.getEntityByAccession(identifier);
        String info = "";
        if (entity != null){
            String formula = "";
            List<DataItem> items = entity.getFormulae();
            if (items != null && items.size() > 0){
                formula = items.get(0).getData();
            }
            info = String.format(
                    TABLE_START +
                    TS + "Formula" + TM + "%s" + TE +
                    TS + "Charge" + TM + "%s" + TE +
                    TS + "Mass" + TM + "%s" + TE +
                    TABLE_END,
                    formula, entity.getCharge(), entity.getMass());
        }
        */

        text += String.format(
                //"<a href=\"http://www.ebi.ac.uk/chebi/init.do\"><img src=\"./images/chebi_logo.png\" title=\"Information from ChEBI\"/></a>" +
                "<a href=\"%s\"><img src=\"%s\" /></a><br />\n",
                imageLink, imageSource);
        return text;
    }


    /**
     * Create non-RDF annotation XML.
     * This is for instance used to process the SABIO-RK data.
     * Parses all the information in the annotation xml which is not RDF CV-Terms.
     */
    private static String createNonRDFAnnotation(SBase sbase) {
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
                    if (name != "RDF") {
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
                            e.printStackTrace();
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
            return String.format(
                    "<hr />\n" +
                            "<div id=\"notes\">\n" +
                            "%s\n" +
                            "</div>\n",
                    notes);
        }
        return "";

    }

    /**
     * Creates true or false HTML depending on boolean.
     */
    public static String booleanHTML(boolean b) {
        return (b) ? ICON_TRUE : ICON_FALSE;
    }

    /////////////////////////////////////////////////////////////////////////////////////

    /**
     * <main> : Testing the HTML creation
     * <p>
     * Create HTML and write to test file for fast
     * development iterations.
     */
    public static void main(String[] args) throws Exception {
        // resources for HTML
        File f = File.createTempFile("MiriamRegistry", ".json");
        // prepare miriam registry support
        Map<String, Namespace> result = RegistryUtil.loadRegistry(f);

        // Create the HTML for selected SBMLDocuments and SBases

        SBMLDocument doc = SBMLUtil.readSBMLDocument("/models/BIOMD0000000016.xml");

        Model model = doc.getModel();

        // object = model.getListOfSpecies().get("c__gal");
        // object = model.getListOfReactions().get("c__GALTM2");


        // retrieve info for object
        SBaseHTMLFactory fac = new SBaseHTMLFactory(model);
        fac.createInfo();
        String html = fac.getHtml();


        // Save to tmp file for viewing
        File file = new File("src/main/resources/tmp", "htmlCreationTest.html");
        FileUtils.writeStringToFile(file, html, StandardCharsets.UTF_8);
    }

    public static String getPrefixValue(String keyToFind) {
        try {
            Properties namespaces = new Properties();
            InputStream inputStream = IOUtil.readResource("/gui/" + FILENAME_NAMESPACE);
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found in resources");
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            namespaces.load(reader);
            return namespaces.getProperty(keyToFind);
           /* String line;
            while ((line = reader.readLine()) != null) {
                // Unescape the line for readability
                String cleanLine = line.replaceAll("\\\\", "");
                if (cleanLine.startsWith(keyToFind + "=")) {
                    return cleanLine.split("=", 2)[1]; // Extract value after '='
                }
            }*/
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
