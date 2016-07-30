package org.cy3sbml.gui;

import java.io.File;
import java.util.*;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.cy3sbml.miriam.RegistryUtil;
import org.cy3sbml.ols.OLSObject;
import org.cy3sbml.util.XMLUtil;
import org.identifiers.registry.RegistryUtilities;
import org.identifiers.registry.data.DataType;
import org.identifiers.registry.data.PhysicalLocation;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.SBasePlugin;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.xml.XMLNode;

import org.cy3sbml.util.SBMLUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * TODO: refactor SBML HTML information completely

 */
public class SBaseHTMLFactory {
    private static final Logger logger = LoggerFactory.getLogger(SBaseHTMLFactory.class);
    private static String baseDir;

    ///////////////////////////////////////////////
    // HTML template strings
    ///////////////////////////////////////////////

	private static final String HTML_START_TEMPLATE =
			"<!DOCTYPE html>\n" +
            "<html>\n" +
			"<head>\n" +
            "\t<base href=\"%s\" />\n" +
			"\t<meta charset=\"utf-8\">\n" +
			"\t<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
			"\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
			"\t<title>cy3sbml</title>\n" +
			"\t<link rel=\"stylesheet\" href=\"./css/bootstrap.min.css\">\n" +
            "\t<link rel=\"stylesheet\" href=\"./font-awesome-4.6.3/css/font-awesome.min.css\">\n" +
			"\t<link rel=\"stylesheet\" href=\"./css/cy3sbml.css\">\n" +
			"</head>\n\n" +
            "<body>\n" +
            "<div class=\"container\">\n";

	private static final String HTML_STOP_TEMPLATE =
			"</div>\n" +
			"<script src=\"./js/jquery.min.js\"></script>\n" +
			"<script src=\"./js/bootstrap.min.js\" crossorigin=\"anonymous\"></script>\n" +
			"</body>\n" +
			"</html>\n";

	private static final String ICON_TRUE = "<span class=\"fa fa-check-circle fa-lg\" title=\"true\" style=\"color:green\"> </span>";
	private static final String ICON_FALSE = "<span class=\"fa fa-times-circle fa-lg\" title=\"false\" style=\"color:red\"> </span>";
	private static final String ICON_NONE = "<span class=\"fa fa-circle-o fa-lg\" title=\"none\"> </span>";
    private static final String ICON_INVISIBLE = "<span class=\"fa fa-circle-o fa-lg icon-invisible\" title=\"none\"> </span>";

    private static final String TABLE_START = "<table class=\"table table-striped table-condensed table-hover\">\n";
    private static final String TABLE_END = "</table>\n";
    private static final String TS = "\t<tr>\n\t\t<td><b>";
    private static final String TM = "</b></td>\n\t\t<td>";
    private static final String TE = "<td/>\n\t</tr>\n";

    private static final String TEMPLATE_MODEL =
            TABLE_START +
            TS + "L%sV%s" + TM + "<a href=\"%s\"><img src=\"./images/sbml_logo.png\" height=\"20\" /></a>" + TE +
            TABLE_END;

    private static final String TEMPLATE_COMPARTMENT =
            TABLE_START +
            TS + "spatialDimensions" + TM + "%s" + TE +
            TS + "size" + TM + "%s <span class=\"unit\">%s</span>" + TE +
            TS + "constant" + TM + "%s" + TE +
            TABLE_END;

    private static final String TEMPLATE_PARAMETER =
            TABLE_START +
            TS + "value" + TM + "%s <span class=\"unit\">%s</span>" + TE +
            TS + "constant" + TM + "%s" +
            TABLE_END;

    private static final String TEMPLATE_INITIAL_ASSIGNMENT =
            TABLE_START +
            TS + "%s" + TM + "= %s" + TE +
            TABLE_END;

    private static final String TEMPLATE_RULE = TEMPLATE_INITIAL_ASSIGNMENT;

    private static final String TEMPLATE_LOCAL_PARAMETER =
            TABLE_START +
            TS + "value" + TM + "%s <span class=\"unit\">%s</span>" + TE +
            TABLE_END;

     private static final String TEMPLATE_SPECIES =
             TABLE_START +
             TS + "compartment" + TM + "%s" + TE +
             TS + "value" + TM + "%s <span class=\"unit\">%s</span>" + TE +
             TS + "constant" + TM + "%s" + TE +
             TS + "boundaryCondition" + TM + "%s" + TE +
             TABLE_END;

    private static final String TEMPLATE_QUALITATIVE_SPECIES =
            TABLE_START +
            TS + "compartment" + TM + "%s" + TE +
            TS + "initial/max level" + TM + "%s/%s" + TE +
            TS + "constant" + TM + "%s" + TE +
            TABLE_END;

    private static final String TEMPLATE_REACTION =
            TABLE_START +
            TS + "compartment" + TM + "%s" + TE +
            TS + "reversible" + TM + "%s" + TE +
            TS + "fast" + TM + "%s" + TE +
            TS + "kineticLaw" + TM + "%s" + TE +
            TS + "units" + TM + "<span class=\"unit\">%s</span>" + TE +
            TABLE_END;

    private static final String TEMPLATE_KINETIC_LAW =
            TABLE_START +
            TS + "kineticLaw" + TM + "%s" + TE +
            TABLE_END;

    private static final String TEMPLATE_PORT =
            TABLE_START +
            TS + "portRef" + TM + "%s " + TE +
            TS + "idRef" + TM + "%s " + TE +
            TS + "unitRef" + TM + "%s " + TE +
            TS + "metaIdRef" + TM + "%s" + TE +
            TABLE_END;

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
	public static String createHTMLText(String text){
        return String.format(HTML_START_TEMPLATE, baseDir) + text + HTML_STOP_TEMPLATE;
    }

	/** Parse and create information for the current sbmlObject. */
	public void createInfo() {
		if (sbase == null){
			return;
		}
        html = String.format(HTML_START_TEMPLATE, baseDir);
		html += createHeader(sbase);
        html += createSBase(sbase);
        html += createSBO(sbase);
        html += createCVTerms(sbase);

        // TODO: implement
        // html += createHistory(sbase);

        html += createNonRDFAnnotation(sbase);
        html += createNotes(sbase);
  		html += HTML_STOP_TEMPLATE;
	}
	
	/**
	 * Creates header HTML.
	 * Displays class information, in addition id and name if existing.
	 */
	private static String createHeader(SBase item){
	    String export = "<small><a href=\"http://html-file\"><span class=\"fa fa-share-square-o\" aria-hidden=\"true\" style=\"color:black\" title=\"Export HTML information\"></span></a></small>&nbsp;&nbsp;";
		String className = SBMLUtil.getUnqualifiedClassName(item);
		String header = String.format("<h2>%s%s</h2>\n", export, className);
		// if NamedSBase get additional information
		if (NamedSBase.class.isAssignableFrom(item.getClass())){
			NamedSBase nsb = (NamedSBase) item;
            header = String.format("<h2>%s%s <small>%s</small></h2>\n", export, className, nsb.getId());
		}
		return header; 
	}

	/** 
	 * Creation of class specific attribute information.
	 */
	private static String createSBase(SBase item){

		// Model //
		if (item instanceof Model){
			Model model = (Model) item;
            Map<String, SBasePlugin> packageMap = model.getExtensionPackages();
            // TODO: add the package information
            // TODO: add the areaUnits, ...

  			return String.format(TEMPLATE_MODEL,
                    model.getLevel(), model.getVersion(), GUIConstants.URL_SBMLFILE);
		}
		
		// Compartment //
		else if (item instanceof Compartment){
			Compartment compartment = (Compartment) item;
			String dimensions = (compartment.isSetSpatialDimensions()) ? ((Double) compartment.getSpatialDimensions()).toString() : ICON_NONE;
			String size = (compartment.isSetSize()) ? ((Double)compartment.getSize()).toString() : ICON_NONE;
			String units = (compartment.isSetUnits()) ? compartment.getUnits() : ICON_NONE;
			String constant = (compartment.isSetConstant()) ? booleanHTML(compartment.getConstant()) : ICON_NONE;
			return String.format(TEMPLATE_COMPARTMENT, dimensions, size, units, constant);
		}

		// Parameter //
		else if (item instanceof Parameter){
			Parameter p = (Parameter) item;
			String value = (p.isSetValue()) ? ((Double) p.getValue()).toString() : ICON_NONE;
			String units = (p.isSetUnits()) ? p.getUnits() : ICON_NONE;
			String constant = (p.isSetConstant()) ? booleanHTML(p.getConstant()) : ICON_NONE;
			return String.format(TEMPLATE_PARAMETER, value, units, constant);
		}

		// InitialAssignment //
		else if (item instanceof InitialAssignment){
			InitialAssignment ass = (InitialAssignment) item;
            String variable = (ass.isSetVariable()) ? ass.getVariable() : ICON_NONE;
            String math = (ass.isSetMath()) ? ass.getMath().toFormula() : ICON_NONE;
            return String.format(TEMPLATE_INITIAL_ASSIGNMENT, variable, math);
		}

		// Rule //
		else if (item instanceof Rule){
            Rule rule = (Rule) item;
            String math = (rule.isSetMath()) ? rule.getMath().toFormula() : ICON_NONE;
            String variable = SBMLUtil.getVariableFromRule(rule);
            if (variable == null){
                variable = ICON_NONE;
            }
            return String.format(TEMPLATE_RULE, variable, math);
		}

		// LocalParameter //
		else if (item instanceof LocalParameter){
			LocalParameter lp = (LocalParameter) item;
			String value = (lp.isSetValue()) ? ((Double) lp.getValue()).toString() : ICON_NONE;
			String units = (lp.isSetUnits()) ? lp.getUnits() : ICON_NONE;
			return String.format(TEMPLATE_LOCAL_PARAMETER, value, units);
		}
		
		// Species //
		else if (item instanceof Species){
			Species s = (Species) item;
			String compartment = (s.isSetCompartment()) ? s.getCompartment().toString() : ICON_NONE;
			String value = (s.isSetValue()) ? ((Double) s.getValue()).toString() : ICON_NONE;
            String units = getDerivedUnitString((AbstractNamedSBaseWithUnit) item);
			String constant = (s.isSetConstant()) ? booleanHTML(s.isConstant()) : ICON_NONE;
			String boundaryCondition = (s.isSetBoundaryCondition()) ? booleanHTML(s.getBoundaryCondition()) : ICON_NONE;

            // TODO: charge & package information (formula, charge)

			return String.format(TEMPLATE_SPECIES, compartment, value, units, constant, boundaryCondition);
		}
		
		// Reaction
		else if (item instanceof Reaction){
			Reaction r = (Reaction) item;

			String compartment = (r.isSetCompartment()) ? r.getCompartment().toString() : ICON_NONE;
			String reversible = (r.isSetReversible()) ? booleanHTML(r.getReversible()) : ICON_NONE;
			String fast = (r.isSetFast()) ? booleanHTML(r.getFast()) : ICON_NONE;
            String kineticLaw = ICON_NONE;
			if (r.isSetKineticLaw()){
				KineticLaw law = r.getKineticLaw();
				if (law.isSetMath()){
					kineticLaw = law.getMath().toFormula();	
				}
			}
            String units = getDerivedUnitString((SBaseWithDerivedUnit) item);

            // TODO: extension information (upper & lower bound, objective)

			return String.format(TEMPLATE_REACTION,
                    compartment,
                    reversible,
                    fast,
                    kineticLaw,
                    units);
		}

		// KineticLaw
		else if (item instanceof KineticLaw){
			KineticLaw law = (KineticLaw) item;
			String kineticLaw = (law.isSetMath()) ? law.getMath().toFormula() : ICON_NONE;
			return String.format(TEMPLATE_KINETIC_LAW, kineticLaw);
		}
		
		// QualitativeSpecies
		else if (item instanceof QualitativeSpecies){
			QualitativeSpecies qs = (QualitativeSpecies) item;

			String compartment = (qs.isSetCompartment()) ? qs.getCompartment().toString() : ICON_NONE;
			String initialLevel = (qs.isSetInitialLevel()) ? ((Integer) qs.getInitialLevel()).toString() : ICON_NONE;
			String maxLevel = (qs.isSetMaxLevel()) ? ((Integer) qs.getMaxLevel()).toString() : ICON_NONE;
			String constant = (qs.isSetConstant()) ? booleanHTML(qs.getConstant()) : ICON_NONE;
			return String.format(TEMPLATE_QUALITATIVE_SPECIES, compartment, initialLevel, maxLevel, constant);
		}

		// Transition //
		else if (item instanceof Transition){
            // TODO:
		}

		// GeneProduct //
		else if (item instanceof GeneProduct){
            // TODO:
		}

		// FunctionDefinition //
		else if (item instanceof FunctionDefinition){
			FunctionDefinition fd = (FunctionDefinition) item;
            String math = (fd.isSetMath()) ? fd.getMath().toFormula() : ICON_NONE;
			return String.format(TEMPLATE_KINETIC_LAW, math);
		}
		
		// comp:Port //
		else if (item instanceof Port){
			Port port = (Port) item;
			String portRef = (port.isSetPortRef()) ? port.getPortRef() : ICON_NONE;
			String idRef = (port.isSetIdRef()) ? port.getIdRef() : ICON_NONE;
			String unitRef = (port.isSetUnitRef()) ? port.getUnitRef() : ICON_NONE;
			String metaIdRef = (port.isSetMetaIdRef()) ? port.getMetaIdRef() : ICON_NONE;
			return String.format(TEMPLATE_PORT, portRef, idRef, unitRef, metaIdRef);
		}
		return "";
	}


    /** Creates SBO HTML. */
    private static String createSBO(SBase item){
        if (item.isSetSBOTerm()){
            String sboTermId = item.getSBOTermID();
            CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS, "http://identifiers.org/biomodels.sbo/" + sboTermId);
            return createCVTerm(term) + "<hr />\n";
        }
        return "";
    }

    /** Create HTML for CVTerms. */
    private static String createCVTerms(SBase sbase){
        List<CVTerm> cvterms = sbase.getCVTerms();
        String text = "";
        if (cvterms.size() > 0){
            for (CVTerm term : cvterms){
                text += createCVTerm(term);
            }
        }
        return text;
    }

    /** Creates HTML for single CVTerm. */
    private static String createCVTerm(CVTerm cvterm){
        // TODO: check if the SBO term is double, i.e. in RDF and SBO only display once
        // TODO: link to primary resource via id
        // TODO: put OLS description on top

        // get the biological/model qualifier type
        CVTerm.Qualifier bmQualifierType = null;
        if (cvterm.isModelQualifier()){
            bmQualifierType = cvterm.getModelQualifierType();
        } else if (cvterm.isBiologicalQualifier()){
            bmQualifierType = cvterm.getBiologicalQualifierType();
        }
        String text = "<p>\n";

        String qualifierHTML = String.format(
                "\t<span class=\"qualifier\" title=\"%s\">%s</span>\n",
                cvterm.getQualifierType(), bmQualifierType);

        // List of Resource URIs
        for (String resourceURI : cvterm.getResources()){

            String identifier = RegistryUtilities.getIdentifierFromURI(resourceURI);
            String dataCollection = RegistryUtilities.getDataCollectionPartFromURI(resourceURI);
            DataType dataType = RegistryUtilities.getDataType(dataCollection);

            // check that identifier is correct for given datatype
            if (dataType != null){
                String pattern = dataType.getRegexp();
                if (!RegistryUtilities.checkRegexp(identifier, pattern)){
                    logger.warn(String.format(
                            "Identifier <%s> does not match pattern <%s> of data collection: <%s>",
                            identifier, pattern, dataType.getId()));
                }
            }

            // not possible to resolve dataType from MIRIAM registry
            if (dataType == null){
                logger.warn(String.format("DataType could not be retrieved for data collection part: <%s>", dataCollection));
                text += qualifierHTML + String.format(
                        "\t<span class=\"identifier\" title=\"identifier\">%s</span><br/>\n",
                        identifier);
                text += String.format(
                        "\t%s <a href=\"%s\"> %s</a><br />\n",
                        ICON_INVISIBLE, resourceURI, resourceURI);
            }
            // dataType found
            if (dataType != null){
                text += qualifierHTML + String.format(
                        "\t<a href=\"%s\"><span class=\"collection\" title=\"MIRIAM registry data collection\">%s</span></a>\n" +
                                "\t<span class=\"identifier\" title=\"identifier\">%s</span><br/>\n",
                        dataType.getURL(), dataType.getName(),
                        identifier);


                for (PhysicalLocation location: dataType.getPhysicalLocations()){
                    if (location.isObsolete()){
                        continue;
                    }
                    String url = String.format("%s%s%s", location.getUrlPrefix(), identifier, location.getUrlSuffix());
                    Boolean primary = location.isPrimary();
                    String info = location.getInfo();
                    if (RegistryUtil.isPhysicalLocationOLS(location)){
                        info = String.format(
                            "<span class=\"ontology\" title=\"Ontology\">%s</span>",
                            info);
                    }
                    text += String.format(
                            "\t%s <a href=\"%s\"> %s</a><br />\n",
                            (primary == true) ? ICON_TRUE : ICON_INVISIBLE, url, info);

                    // OLS resource, we can query the term
                    if (RegistryUtil.isPhysicalLocationOLS(location)){
                        Term term = OLSObject.getTermFromIdentifier(identifier);
                        if (term != null) {
                            // FIXME: check for null in the information
                            text += String.format("<span class=\"ontology\">%s</span> <b>%s</b><br />\n", term.getOntologyName(), term.getLabel());
                            text += String.format("\t<a href=%s>%s</a><br />", term.getIri().getIdentifier(), term.getIri().getIdentifier());
                            String [] descriptions = term.getDescription();
                            if (descriptions != null) {
                                for (String description : term.getDescription()) {
                                    text += String.format("\t%s<br />\n", description);
                                }
                            }
                            text += String.format("%s<br />\n", term.getShortForm());
                            text += String.format("%s<br />\n", term.getSynonyms());

                        } else {
                            logger.error("OLS term could not be fetched.");
                        }
                    }
                }

                // special information
                if (dataType.getNamespace().equals("uniprot")) {
                    text += uniprotHTML(identifier);
                }

                // special information
                if (dataType.getNamespace().equals("chebi")) {
                    text += chebiHTML(identifier);
                }




            }
        }
        text += "</p>\n";
        return text;
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
        text += String.format("<a href=\"%s\"><img src=\"%s\" /></a><br />", imageLink, imageSource);
        return text;
        // TODO: additional things like formula, net charge, average mass, ...
    }

    /**
     * Creates additional information for entry.
     * Identifier of the form "P29218"
     */
    private static String uniprotHTML(String identifier){
        // TODO: xml available for parsing
        // http://www.uniprot.org/uniprot/P29218.xml

        // names
        // gene
        // ec number
        // organism
        return "";
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
                            logger.error("Error parsing annotation xml");
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

	/////////////////////////////////////////////////////////////////////////////////////
    // Helper functions
    /////////////////////////////////////////////////////////////////////////////////////

    /** Creates true or false HTML depending on boolean. */
    private static String booleanHTML(boolean b){
        return (b == true) ? ICON_TRUE : ICON_FALSE;
    }

    /** Derived unit string. */
    private static String getDerivedUnitString(SBaseWithDerivedUnit usbase){
        String units = ICON_NONE;
        UnitDefinition udef = usbase.getDerivedUnitDefinition();
        if (udef != null){
            units = udef.toString();
        }
        return units;
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
