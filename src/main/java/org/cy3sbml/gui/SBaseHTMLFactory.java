package org.cy3sbml.gui;

import java.io.File;
import java.util.*;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.cy3sbml.miriam.RegistryUtil;
import org.cy3sbml.ols.OLSAccess;
import org.cy3sbml.ols.OLSCache;
import org.cy3sbml.uniprot.UniprotCache;
import org.cy3sbml.util.XMLUtil;
import org.identifiers.registry.RegistryDatabase;
import org.identifiers.registry.RegistryLocalProvider;
import org.identifiers.registry.RegistryUtilities;
import org.identifiers.registry.data.DataType;
import org.identifiers.registry.data.PhysicalLocation;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.xml.XMLNode;

import org.cy3sbml.util.SBMLUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.kraken.interfaces.uniprot.*;
import uk.ac.ebi.kraken.interfaces.uniprot.comments.*;
import uk.ac.ebi.kraken.interfaces.uniprot.description.Field;
import uk.ac.ebi.kraken.interfaces.uniprot.description.Name;
import uk.ac.ebi.pride.utilities.ols.web.service.model.Term;

/** 
 * Creates HTML information for given SBase.
 * Core information is parsed from the NamedSBase object,
 * with additional information like resources retrieved via
 * web services (MIRIAM).
 *
 * Here the HTML information string is created which is displayed
 * on selection of SBML objects in the graph.
 *

 * TODO: more compact layout, i.e remove empty rows
 */
public class SBaseHTMLFactory {
    private static final Logger logger = LoggerFactory.getLogger(SBaseHTMLFactory.class);
    private static String baseDir;

    ///////////////////////////////////////////////
    // HTML template strings
    ///////////////////////////////////////////////

    public static final String HTML_START_TEMPLATE =
            "<!DOCTYPE html>\n" +
                    "<html>\n" +
                    "<head>\n" +
                    "\t<base href=\"%s\" />\n" +
                    "\t<meta charset=\"utf-8\">\n" +
                    "\t<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
                    "\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
                    "\t<title>%s</title>\n" +
                    "\t<link rel=\"shortcut icon\" href=\"./images/favicon.ico\" />\n" +
                    "\t<link rel=\"stylesheet\" href=\"./css/bootstrap.min.css\">\n" +
                    "\t<link rel=\"stylesheet\" href=\"./font-awesome-4.6.3/css/font-awesome.min.css\">\n" +
                    "\t<link rel=\"stylesheet\" href=\"./css/cy3sbml.css\">\n" +
                    "</head>\n\n" +
                    "<body>\n" +
                    "<div class=\"container\">\n";

    public static final String HTML_STOP_TEMPLATE =
            "</div>\n" +
                    "<script src=\"./js/jquery.min.js\"></script>\n" +
                    "<script src=\"./js/bootstrap.min.js\"></script>\n" +
                    "</body>\n" +
                    "</html>\n";

    public static final String ICON_TRUE = "<span class=\"fa fa-check-circle fa-lg\" title=\"true\" style=\"color:green\"> </span>";
    public static final String ICON_FALSE = "<span class=\"fa fa-times-circle fa-lg\" title=\"false\" style=\"color:red\"> </span>";
    public static final String ICON_NONE = "<span class=\"fa fa-circle-o fa-lg\" title=\"none\"> </span>";
    public static final String ICON_INVISIBLE = "<span class=\"fa fa-circle-o fa-lg icon-invisible\" title=\"none\"> </span>";

    public static final String EXPORT_HTML = "<small><a href=\"http://html-file\"><span class=\"fa fa-share-square-o\" aria-hidden=\"true\" style=\"color:black\" title=\"Export HTML information\"></span></a></small>&nbsp;&nbsp;";

    public static final String TABLE_START = "<table class=\"table table-striped table-condensed table-hover\">\n";
    public static final String TABLE_END = "</table>\n";
    public static final String TS = "\t<tr>\n\t\t<td><b>";
    public static final String TM = "</b></td>\n\t\t<td>";
    public static final String TE = "<td/>\n\t</tr>\n";

    ///////////////////////////////////////////////

	private SBase sbase;
	private String html;

    /** Constructor. */
	public SBaseHTMLFactory(Object obj){
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
     * Get created information string.
     * No information String created in cache mode.
     */
    public String getHtml() {
        return html;
    }

    /**
     * Creates HTML for given text String.
     */
    public static String createHTMLText(String text, String title){
        return String.format(HTML_START_TEMPLATE, baseDir, title) + text + HTML_STOP_TEMPLATE;
    }

    /** Creates HTML text. */
    public static String createHTMLText(String text){
        return createHTMLText(text, "cy3sbml");
    }

	/** Parse and create information for the current sbmlObject. */
	public void createInfo() {
		if (sbase == null){
			return;
		}
        // title from class and id
        String id = "";
        if (NamedSBase.class.isAssignableFrom(sbase.getClass())){
            NamedSBase nsb = (NamedSBase) sbase;
            if (nsb.isSetId()){
                id = nsb.getId();
            }
        }
		String title = String.format(
		        "%s %s", id, SBMLUtil.getUnqualifiedClassName(sbase));

        html = String.format(HTML_START_TEMPLATE, baseDir, title);
		html += createHeader(sbase);
        html += createSBase(sbase);
        html += createHistory(sbase);
        html += createCVTerms(sbase);
        html += createNonRDFAnnotation(sbase);
        html += createNotes(sbase);
  		html += HTML_STOP_TEMPLATE;
	}
	
	/**
	 * Creates header HTML.
	 * Displays class information, in addition id and name if existing.
	 */
	private static String createHeader(SBase sbase){
		String className = SBMLUtil.getUnqualifiedClassName(sbase);
		String header = String.format(
		        "<h2>%s%s</h2>\n",
                EXPORT_HTML, className);
		// if NamedSBase get additional information
		if (NamedSBase.class.isAssignableFrom(sbase.getClass())){
			NamedSBase nsb = (NamedSBase) sbase;
            header = String.format(
                    "<h2>%s%s <small>%s</small></h2>\n",
                    EXPORT_HTML, className, nsb.getId());
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
    private static String createHistory(SBase sbase){
        String html = "";
        History h = sbase.getHistory();
        for (Creator c: h.getListOfCreators()){
            String givenName = c.isSetGivenName() ? c.getGivenName() : "";
            String familyName = c.isSetFamilyName() ? c.getFamilyName() : "";
            String organisation = c.isSetOrganisation() ? String.format("; %s", c.getOrganisation()) : "";
            String email = "";
            if (c.isSetEmail()){
                email = String.format("(<a href=\"mailto:%s\">%s</a>)", c.getEmail(), c.getEmail());
            }
            html += String.format("%s %s %s %s</br>\n", givenName, familyName, email, organisation);
        }
        if (h.isSetCreatedDate()){
            html += String.format("created: <code>%s</code></br>\n", h.getCreatedDate());
        }
        if (h.isSetListOfModification()){
            for (Date date: h.getListOfModifiedDates()){
                html += String.format("modified: <code>%s</code></br>\n", date);
            }
        }
        return html;
    }

    /**
     * Creates the HTML table from map.
     */
	private static String createTableFromMap(Map<String, String> map){
        String html = TABLE_START;
        for (String key: map.keySet()){
            html += TS + key + TM + map.get(key) + TE;
        }
        return html + TABLE_END;
    }

	/** 
	 * Creation of class specific attribute information.
     * This mimics the SBMLReader
	 */
	private static String createSBase(SBase item){
	    LinkedHashMap<String, String> map;

        // Model //
		if (item instanceof Model){
		    map = SBMLUtil.createModelMap((Model) item);
		}
        // Compartment //
		else if (item instanceof Compartment){
            map = SBMLUtil.createCompartmentMap((Compartment) item);
		}
        // Parameter //
		else if (item instanceof Parameter){
            map = SBMLUtil.createParameterMap((Parameter) item);
		}
		// InitialAssignment //
		else if (item instanceof InitialAssignment){
            map = SBMLUtil.createInitialAssignmentMap((InitialAssignment) item);
		}
		// Rule //
		else if (item instanceof Rule){
            map = SBMLUtil.createRuleMap((Rule) item);
		}
		// LocalParameter //
		else if (item instanceof LocalParameter){
			map = SBMLUtil.createLocalParameterMap((LocalParameter) item);
		}
		// Species //
		else if (item instanceof Species){
			map = SBMLUtil.createSpeciesMap((Species) item);
		}
		// Reaction
		else if (item instanceof Reaction){
			map = SBMLUtil.createReactionMap((Reaction) item);
		}
		// KineticLaw
		else if (item instanceof KineticLaw){
			map = SBMLUtil.createKineticLawMap((KineticLaw) item);
		}
        // FunctionDefinition //
        else if (item instanceof FunctionDefinition){
            map = SBMLUtil.createFunctionDefinitionMap((FunctionDefinition) item);
        }
		// qual:QualitativeSpecies
		else if (item instanceof QualitativeSpecies){
			map = SBMLUtil.createQualitativeSpeciesMap((QualitativeSpecies) item);
		}
		// qual:Transition //
		else if (item instanceof Transition){
            map = SBMLUtil.createTransitionMap((Transition) item);
		}
		// fbc:GeneProduct //
		else if (item instanceof GeneProduct){
            map = SBMLUtil.createGeneProductMap((GeneProduct) item);
		}
		// comp:Port //
		else if (item instanceof Port){
			map = SBMLUtil.createPortMap((Port) item);
		}
		// Not supported
		else {
            logger.warn(String.format("No object map support for %s <%s>", SBMLUtil.getUnqualifiedClassName(item), item));
		    if (item instanceof NamedSBase){
                map = SBMLUtil.createNamedSBaseMap((NamedSBase) item);
            } else {
                map = SBMLUtil.createSBaseMap(item);
            }
        }
		return createTableFromMap(map);
	}

    /** Create HTML for CVTerms. */
    private static String createCVTerms(SBase sbase){
        List<CVTerm> cvterms = sbase.getCVTerms();
        // Handle SBO
        addCVTermForSBO(sbase);

        // Create HTML
        String text = "";
        if (cvterms.size() > 0){
            for (CVTerm term : cvterms){
                text += createCVTerm(term);
            }
        }
        return text;
    }

    /** Adds the CVTerm for SBO to the CVTerms. */
    private static void addCVTermForSBO(SBase sbase){
        List<CVTerm> cvterms = sbase.getCVTerms();

        // add the SBO term to the annotations if not existing already
        if (sbase.isSetSBOTerm()){
            String sboTermId = sbase.getSBOTermID();
            CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS, "http://identifiers.org/biomodels.sbo/" + sboTermId);
            // createCVTerm(term) + "<hr />\n";

            Boolean termExists = false;
            outerloop:
            for (CVTerm t : cvterms){
                for (String uri: t.getResources()){
                    if (uri.endsWith(sboTermId)){
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

    /** Creates HTML for single CVTerm. */
    private static String createCVTerm(CVTerm cvterm){


        // get the biological/model qualifier type
        CVTerm.Qualifier bmQualifierType = null;
        if (cvterm.isModelQualifier()){
            bmQualifierType = cvterm.getModelQualifierType();
        } else if (cvterm.isBiologicalQualifier()){
            bmQualifierType = cvterm.getBiologicalQualifierType();
        }

        String text = "";

        String qualifierHTML = String.format(
                "<p class=\"cvterm\">\n" +
                "\t<span class=\"qualifier\" title=\"%s\">%s</span>\n",
                cvterm.getQualifierType(), bmQualifierType);

        // List of Resource URIs
        for (String resourceURI : cvterm.getResources()){

            String identifier = RegistryUtilities.getIdentifierFromURI(resourceURI);
            String dataCollection = RegistryUtilities.getDataCollectionPartFromURI(resourceURI);

            //DataType dataType = RegistryUtilities.getDataType(dataCollection);
            DataType dataType = RegistryDatabase.getInstance().getDataTypeByURI(dataCollection);

            String identifierHTML = String.format(
                    "<span class=\"identifier\" title=\"Resource identifier\">%s</span>",
                    identifier);

            // check that identifier is correct for given datatype
            if (dataType != null){
                String pattern = dataType.getRegexp();
                if (!RegistryUtilities.checkRegexp(identifier, pattern)){
                    logger.warn(String.format(
                            "Identifier <%s> does not match pattern <%s> of data collection: <%s>",
                            identifier, pattern, dataType.getId()));
                }
            }

            // TODO: link to primary resource via id

            // not possible to resolve dataType from MIRIAM registry
            if (dataType == null){
                logger.warn(String.format("DataType could not be retrieved for data collection part: <%s>", dataCollection));
                text += qualifierHTML + identifierHTML + "<br />\n";
                text += String.format(
                        "\t%s <a href=\"%s\"> %s</a><br />\n",
                        ICON_INVISIBLE, resourceURI, resourceURI);
            }
            // dataType found
            if (dataType != null){
                text += qualifierHTML + String.format(
                        "\t<a href=\"%s\"><span class=\"collection\" title=\"MIRIAM registry data collection\">%s</span></a>%s<br/>\n",
                        dataType.getURL(), dataType.getName(), identifierHTML);

                // Create OLS resource for location
                for (PhysicalLocation location: dataType.getPhysicalLocations()) {
                    if (location.isObsolete()) { continue; }
                    if (OLSAccess.isPhysicalLocationOLS(location)){
                        text += createOLSLocation(location, identifier);
                    }
                }
                // Create other locations
                for (PhysicalLocation location: dataType.getPhysicalLocations()){
                    if (location.isObsolete()){ continue; }
                    if (! OLSAccess.isPhysicalLocationOLS(location)) {
                        text += createNonOLSLocation(location, identifier);
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
     * Information for non-OLS location.
     */
    private static String createNonOLSLocation(PhysicalLocation location, String identifier){
        String text = "";
        String url = String.format(
                "%s%s%s",
                location.getUrlPrefix(), identifier, location.getUrlSuffix());
        Boolean primary = location.isPrimary();
        String info = location.getInfo();

        text += String.format(
                "\t%s <a href=\"%s\"> %s</a><br />\n",
                (primary == true) ? ICON_TRUE : ICON_INVISIBLE, url, info);
        return text;
    }


    /**
     * Information for an OLS location.
     * Only the identifier needed for the query.
     */
    private static String createOLSLocation(PhysicalLocation location, String identifier){
        String html = "";
        // Term term = OLSAccess.getTerm(identifier);
        Term term = OLSCache.getTerm(identifier);
        if (term != null) {

            String purlURL = term.getIri().getIdentifier();
            String ontologyURL = String.format(
                    "%s%s%s",
                    location.getUrlPrefix(), identifier, location.getUrlSuffix());
            html += String.format(
                    "\t<a href=\"%s\"><span class=\"ontology\" title=\"Ontology\">%s</span></a> <b>%s</b> <a href=%s class=\"text-muted\">%s</a><br />\n",
                    ontologyURL, term.getOntologyName().toUpperCase(), term.getLabel(),
                    purlURL, purlURL);

            String [] synonyms = term.getSynonyms();
            if (synonyms != null && synonyms.length > 0) {
                html += "\t<span class=\"comment\">Synonyms</span> ";
                for (String syn: synonyms) {
                    html += String.format("%s; ", syn);
                }
                html += "<br />\n";
            }

            String [] descriptions = term.getDescription();
            if (descriptions != null && descriptions.length > 0) {
                for (String description : descriptions) {
                    html += String.format("\t<span class=\"text-success\">%s</span><br />\n", description);
                }
            }

            html += "\t<br />";

        } else {
            createNonOLSLocation(location, identifier);
        }
        return html;
    }


    /**
     * Resolves secondary resourses and returns the HTML.
     * @param dataType
     * @param identifier
     * @return html string
     */
    public static String createSecondaryInformation(DataType dataType, String identifier){
        String html = "";
        String namespace = dataType.getNamespace();

        if (namespace.equals("uniprot")) {
            html += uniprotHTML(identifier);
        }
        else if (namespace.equals("chebi")) {
            html += chebiHTML(identifier);
        }

        return html;
    }

    /**
     * Creates additional chebi information for the entry.
     * Identifier is of form "CHEBI:28061"
     */
    private static String chebiHTML(String identifier){
        // Image
        String text = "";
        String[] tokens = identifier.split(":");
        String imageSource = String.format(
                "http://www.ebi.ac.uk/chebi/displayImage.do;?defaultImage=true&imageIndex=0&chebiId=%s&dimensions=200",
                tokens[1]);
        String imageLink = String.format(
                "http://www.ebi.ac.uk/chebi/searchId.do?chebiId=CHEBI:%s",
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
     * Creates additional information for entry.
     * Identifier of the form "P29218"
     */
    private static String uniprotHTML(String accession){
        String text = "\t<br />\n";
        // UniProtEntry entry = UniprotAccess.getUniProtEntry(accession);
        UniProtEntry entry = UniprotCache.getUniProtEntry(accession);
        if (entry != null) {
            String uniProtId = entry.getUniProtId().toString();
            text += String.format(
                    "\t<a href=\"http://www.uniprot.org/uniprot\"><img src=\"./images/logos/uniprot_icon.png\" title=\"Information from UniProt\"/></a>&nbsp;&nbsp;\n" +
                    "\t<a href=\"http://www.uniprot.org/uniprot/%s\"><span class=\"identifier\">%s</span></a> (%s)<br />\n", accession, accession, uniProtId);

            // description
            ProteinDescription description = entry.getProteinDescription();

            // Names (Full, Short, EC, AltName)
            Name name = description.getRecommendedName();
            List<Field> fields = name.getFields();
            for (Field field: fields){
                String value = field.getValue();
                if (field.getType().getValue().equals("Full")){
                    text += String.format(
                            "\t<b>%s</b><br />\n",
                            field.getValue());
                }else {
                    text += String.format(
                            "\t<b>%s</b>: %s<br />\n",
                            field.getType().getValue(), field.getValue());
                }
            }

            // organism
            Organism organism = entry.getOrganism();
            String organismStr = organism.getScientificName().toString();
            if (organism.hasCommonName()){
                organismStr += String.format(" (%s)", organism.getCommonName());
            }
            text += String.format(
                    "\t<b>Organism</b>: %s<br />\n",
                    organismStr);

            // genes
            for (Gene gene : entry.getGenes()){
                String geneName = gene.getGeneName().getValue();
                text += String.format("\t<b>Gene</b>: %s<br />\n", geneName);
            }

            // alternative names
            text +="\t<span class=\"comment\">Synonyms</span>";
            for (Name n: description.getAlternativeNames()){
                text += String.format(
                        "%s; ", n.getFields().get(0).getValue());
            }
            text += "<br />\n";

            // comments
            for (Comment comment : entry.getComments()){
                CommentType ctype = comment.getCommentType();
                if (ctype.equals(CommentType.FUNCTION)){
                    FunctionComment fComment = (FunctionComment) comment;
                    for (CommentText commentText : fComment.getTexts()) {
                        text += String.format("\t<span class=\"comment\">Function</span> <span class=\"text-success\">%s</span><br />\n", commentText.getValue());
                    }
                }
                else if (ctype.equals(CommentType.CATALYTIC_ACTIVITY)) {
                    CatalyticActivityComment caComment = (CatalyticActivityComment) comment;
                    for (CommentText commentText : caComment.getTexts()) {
                        text += String.format("\t<span class=\"comment\">Catalytic Activity</span>%s<br />\n", commentText.getValue());
                    }
                }
                else if (ctype.equals(CommentType.PATHWAY)) {
                    PathwayComment pComment = (PathwayComment) comment;
                    for (CommentText commentText : pComment.getTexts()) {
                        text += String.format("\t<span class=\"comment\">Pathway</span>%s<br />\n", commentText.getValue());
                    }
                }
            }
        }
        return text;
    }


    /**
     * Create non-RDF annotation XML.
     * This is for instance used to process the SABIO-RK data.
     * Parses all the information in the annotation xml which is not RDF CV-Terms.
     */
    private static String createNonRDFAnnotation(SBase sbase){
        String html = "";
        if (sbase.isSetAnnotation()){
            Annotation annotation = sbase.getAnnotation();
            String text = "";
            XMLNode xmlNode = annotation.getNonRDFannotation();
            if (xmlNode != null){
                // get all children which are not RDF
                for (int i=0; i<xmlNode.getChildCount(); i++){
                    XMLNode child = xmlNode.getChildAt(i);
                    String name = child.getName();
                    if (name != "RDF"){
                        try {
                            String xml = XMLNode.convertXMLNodeToString(child);
                            // Handle special case of whitespaces/empty text nodes
                            xml = xml.trim();
                            if (xml.length() > 0){
                                xml = XMLUtil.xml2Html(xml);
                                if (xml != null) {
                                    text += xml;
                                } else {
                                    logger.error("Annotation XML could not be parsed.");
                                }
                            }
                        } catch(XMLStreamException e){
                            logger.error("Error parsing annotation xml", e);
                            e.printStackTrace();
                        }
                    }
                }
            }

            // move into <code> tag for display
            if (text.length()>0){
                html = String.format("<code>%s</code>", text);
            }
        }
        return html;
    }

    /** Create HMTL for notes. */
    private static String createNotes(SBase sbase){
        String notes = SBMLUtil.parseNotes(sbase);
        if (notes != null){
            return String.format(
                    "<hr />\n" +
                    "<div id=\"notes\">\n" +
                    "%s\n" +
                    "</div>\n",
                    notes);
        }
        return "";

    }

    /** Creates true or false HTML depending on boolean. */
    public static String booleanHTML(boolean b){
        return (b) ? SBaseHTMLFactory.ICON_TRUE : SBaseHTMLFactory.ICON_FALSE;
    }

    /////////////////////////////////////////////////////////////////////////////////////

    /**
     * <main> : Testing the HTML creation
     *
     * Create HTML and write to test file for fast
     * development iterations.
     */
    public static void main(String[] args) throws Exception{
        // resources for HTML
        SBaseHTMLFactory.setBaseDir("file:///home/mkoenig/git/cy3sbml/src/main/resources/gui/");
        // where to write the tmp HTML
        String targetDir = "/home/mkoenig/git/cy3sbml/src/main/resources/tmp";
        // prepare miriam registry support
        RegistryUtil.loadRegistry();

        // Create the HTML for selected SBMLDocuments and SBases

        // SBMLDocument doc = SBMLUtil.readSBMLDocument("/models/BIOMD0000000016.xml");
        SBMLDocument doc = SBMLUtil.readSBMLDocument("/models/Koenig_galactose_v31.xml");

        Model model = doc.getModel();
        Object object = model;

        object = model.getListOfSpecies().get("c__gal");
        object = model.getListOfReactions().get("c__GALTM2");


        // retrieve info for object
        SBaseHTMLFactory f = new SBaseHTMLFactory(object);
        f.createInfo();
        String html = f.getHtml();

        System.out.println("------------------------------------");
        System.out.println(html);
        System.out.println("------------------------------------");

        // Save to tmp file for viewing
        File file = new File(targetDir, "testinfo.html");
        FileUtils.writeStringToFile(file, html);
    }

}
