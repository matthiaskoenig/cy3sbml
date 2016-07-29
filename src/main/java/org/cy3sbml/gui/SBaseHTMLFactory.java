package org.cy3sbml.gui;

import java.io.File;
import java.util.*;

import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.cy3sbml.util.XMLUtil;
import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.SBasePlugin;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.xml.XMLNode;

import org.cy3sbml.miriam.MiriamResource;
import org.cy3sbml.util.SBMLUtil;
import org.cy3sbml.util.AnnotationUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            "\t<meta http-equiv=\"refresh\" content=\"5\">\n" +
			"</head>\n\n" +
            "<body>\n" +
            "<div class=\"container\">\n";

	private static final String HTML_STOP_TEMPLATE =
			"</div>\n" +
			"<script src=\"./js/jquery.min.js\"></script>\n" +
			"<script src=\"./js/bootstrap.min.js\" crossorigin=\"anonymous\"></script>\n" +
			"</body>\n" +
			"</html>\n";

	private static final String TRUE_HTML = "<span class=\"fa fa-check-circle fa-lg\" title=\"true\" style=\"color:green\"> </span>";
	private static final String FALSE_HTML = "<span class=\"fa fa-times-circle fa-lg\" title=\"false\" style=\"color:red\"> </span>";
	private static final String NONE_HTML = "<span class=\"fa fa-circle-o fa-lg\" title=\"none\"> </span>";

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
	 * Cache the information for the given sbase.
	 * TODO: cache the costly lookups and creations.
	 */
    public void cacheInformation(){
        // cache miriam information
        for (CVTerm term : sbase.getCVTerms()){
            for (String rURI : term.getResources()){
                MiriamResource.getLocationsFromURI(rURI);
            }
        }
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

        html += createAnnotation(sbase);
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
	private static String createCVTerm(CVTerm term){
        // get the biological/model qualifier type
        CVTerm.Qualifier bmQualifierType = null;
        if (term.isModelQualifier()){
            bmQualifierType = term.getModelQualifierType();
        } else if (term.isBiologicalQualifier()){
            bmQualifierType = term.getBiologicalQualifierType();
        }
        String text = "<p>\n";

        String qualifierHTML = String.format(
                "\t<span class=\"qualifier\" title=\"%s\">%s</span>\n",
                term.getQualifierType(), bmQualifierType);

        Map<String, String> map = null;
        for (String rURI : term.getResources()){
            map = AnnotationUtil.getIdCollectionMapForURI(rURI);
            text += qualifierHTML + String.format(
                    "\t<span class=\"ontology\" title=\"ontology\">%s</span>\n\t<span class=\"term\" title=\"term\">%s</span><br/>\n",
                    map.get("collection").toUpperCase(), map.get("id"));

            // TODO: create the URIs (use datatype, and registry tools)
            // Use information about dataResource
            text += createInfoForURI(rURI);

            // TODO: get the definition from OLS & other infos
            String definition = "DEFINITION";
            if (definition != null) {
                text += String.format("\t%s\n", definition);
            }
        }
        text += "</p>\n";
        return text;
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
			String dimensions = (compartment.isSetSpatialDimensions()) ? ((Double) compartment.getSpatialDimensions()).toString() : NONE_HTML;
			String size = (compartment.isSetSize()) ? ((Double)compartment.getSize()).toString() : NONE_HTML;
			String units = (compartment.isSetUnits()) ? compartment.getUnits() : NONE_HTML;
			String constant = (compartment.isSetConstant()) ? booleanHTML(compartment.getConstant()) : NONE_HTML;
			return String.format(TEMPLATE_COMPARTMENT, dimensions, size, units, constant);
		}

		// Parameter //
		else if (item instanceof Parameter){
			Parameter p = (Parameter) item;
			String value = (p.isSetValue()) ? ((Double) p.getValue()).toString() : NONE_HTML;
			String units = (p.isSetUnits()) ? p.getUnits() : NONE_HTML;
			String constant = (p.isSetConstant()) ? booleanHTML(p.getConstant()) : NONE_HTML;
			return String.format(TEMPLATE_PARAMETER, value, units, constant);
		}

		// InitialAssignment //
		else if (item instanceof InitialAssignment){
			InitialAssignment ass = (InitialAssignment) item;
            String variable = (ass.isSetVariable()) ? ass.getVariable() : NONE_HTML;
            String math = (ass.isSetMath()) ? ass.getMath().toFormula() : NONE_HTML;
            return String.format(TEMPLATE_INITIAL_ASSIGNMENT, variable, math);
		}

		// Rule //
		else if (item instanceof Rule){
            Rule rule = (Rule) item;
            String math = (rule.isSetMath()) ? rule.getMath().toFormula() : NONE_HTML;
            String variable = SBMLUtil.getVariableFromRule(rule);
            if (variable == null){
                variable = NONE_HTML;
            }
            return String.format(TEMPLATE_RULE, variable, math);
		}

		// LocalParameter //
		else if (item instanceof LocalParameter){
			LocalParameter lp = (LocalParameter) item;
			String value = (lp.isSetValue()) ? ((Double) lp.getValue()).toString() : NONE_HTML;
			String units = (lp.isSetUnits()) ? lp.getUnits() : NONE_HTML;
			return String.format(TEMPLATE_LOCAL_PARAMETER, value, units);
		}
		
		// Species //
		else if (item instanceof Species){
			Species s = (Species) item;
			String compartment = (s.isSetCompartment()) ? s.getCompartment().toString() : NONE_HTML;
			String value = (s.isSetValue()) ? ((Double) s.getValue()).toString() : NONE_HTML;
            String units = getDerivedUnitString((AbstractNamedSBaseWithUnit) item);
			String constant = (s.isSetConstant()) ? booleanHTML(s.isConstant()) : NONE_HTML;
			String boundaryCondition = (s.isSetBoundaryCondition()) ? booleanHTML(s.getBoundaryCondition()) : NONE_HTML;

            // TODO: charge & package information (formula, charge)

			return String.format(TEMPLATE_SPECIES, compartment, value, units, constant, boundaryCondition);
		}
		
		// Reaction
		else if (item instanceof Reaction){
			Reaction r = (Reaction) item;

			String compartment = (r.isSetCompartment()) ? r.getCompartment().toString() : NONE_HTML;
			String reversible = (r.isSetReversible()) ? booleanHTML(r.getReversible()) : NONE_HTML;
			String fast = (r.isSetFast()) ? booleanHTML(r.getFast()) : NONE_HTML;
            String kineticLaw = NONE_HTML;
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
			String kineticLaw = (law.isSetMath()) ? law.getMath().toFormula() : NONE_HTML;
			return String.format(TEMPLATE_KINETIC_LAW, kineticLaw);
		}
		
		// QualitativeSpecies
		else if (item instanceof QualitativeSpecies){
			QualitativeSpecies qs = (QualitativeSpecies) item;

			String compartment = (qs.isSetCompartment()) ? qs.getCompartment().toString() : NONE_HTML;
			String initialLevel = (qs.isSetInitialLevel()) ? ((Integer) qs.getInitialLevel()).toString() : NONE_HTML;
			String maxLevel = (qs.isSetMaxLevel()) ? ((Integer) qs.getMaxLevel()).toString() : NONE_HTML;
			String constant = (qs.isSetConstant()) ? booleanHTML(qs.getConstant()) : NONE_HTML;
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
            String math = (fd.isSetMath()) ? fd.getMath().toFormula() : NONE_HTML;
			return String.format(TEMPLATE_KINETIC_LAW, math);
		}
		
		// comp:Port //
		else if (item instanceof Port){
			Port port = (Port) item;
			String portRef = (port.isSetPortRef()) ? port.getPortRef() : NONE_HTML;
			String idRef = (port.isSetIdRef()) ? port.getIdRef() : NONE_HTML;
			String unitRef = (port.isSetUnitRef()) ? port.getUnitRef() : NONE_HTML;
			String metaIdRef = (port.isSetMetaIdRef()) ? port.getMetaIdRef() : NONE_HTML;
			return String.format(TEMPLATE_PORT, portRef, idRef, unitRef, metaIdRef);
		}
		return "";
	}

    /**
     * Create annotation XML.
     * This is for instance used to process the SABIO-RK data.
     */
    private static String createAnnotation(SBase sbase){
        String html = "";
        // Non-RDF annotation
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

    /**
     * Creates true or false HTML depending on boolean.
     */
    private static String booleanHTML(boolean b){
        return (b == true) ? TRUE_HTML : FALSE_HTML;
    }

    /**
     * Derived unit string.
     */
    private static String getDerivedUnitString(SBaseWithDerivedUnit usbase){
        String units = NONE_HTML;
        UnitDefinition udef = usbase.getDerivedUnitDefinition();
        if (udef != null){
            units = udef.toString();
        }
        return units;
    }

    /**
     * Creates the information for a given resourceURI.
     * Resolves the locations and uses it them to create the links.
     * FIXME: update me with the new Miriam Functionality
     * TODO: resolve information with OLS
     */
    @Deprecated
    private static String createInfoForURI(String resourceURI) {
        String text = "";
        String[] locations = MiriamResource.getLocationsFromURI(resourceURI);

        if (locations != null){
            if (locations.length == 0){
                logger.warn("No locations for URI:" + resourceURI);
            }
            String[] items = new String[locations.length];
            for (int k=0; k<locations.length; k++) {
                String location = locations[k];
                items[k] = String.format("\t<a href=\"%s\">%s</a><br />\n", location, serverFromLocation(location));
            }
            text = StringUtils.join(items, "");

        } else {
            logger.warn("No locations for URI: " + resourceURI);
        }
        return text;
    }

    /**
     * Get short server string from full location.
     */
    @Deprecated
    private static String serverFromLocation(String location) {
        // get everything instead of the last item
        String[] items = location.split("/");
        String[] serverItems = Arrays.copyOfRange(items, 0, items.length-1);
        String text = org.apache.commons.lang3.StringUtils.join(serverItems, "/");
        return text;
    }

    /////////////////////////////////////////////////////////////////////////////////////

    /** Create HTML and write to test file for fast
     * development iterations.
     */
    public static void main(String[] args) throws Exception{
        SBaseHTMLFactory.setBaseDir("file:///home/mkoenig/git/cy3sbml/src/main/resources/gui/");
        String targetDir = "/home/mkoenig/git/cy3sbml/src/main/resources/tmp";


        SBMLDocument doc = SBMLUtil.readSBMLDocument("/models/BIOMD0000000016.xml");
        Model model = doc.getModel();
        Object object = model;

        //object = model.getListOfSpecies().get(0);

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
