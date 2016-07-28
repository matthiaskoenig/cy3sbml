package org.cy3sbml.gui;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import org.sbml.jsbml.*;
import org.sbml.jsbml.ext.comp.Port;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;
import org.sbml.jsbml.ontology.Term;
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
 * Here the HTML information string is created which is displayed
 * on selection of SBML objects in the graph.
 *
 * TODO: refactor SBML HTML information completely
 * TODO: tests for creating HTML information with simple main.
 *
 */
public class SBaseHTMLFactory {
    private static String baseDir;

	private static final String HTML_START_TEMPLATE =
			"<html>\n" +
			"<head>\n" +
            "<base href=\"%s\" />\n" +
			"\t<meta charset=\"utf-8\">\n" +
			"\t<meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge\">\n" +
			"\t<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n" +
			"\t<title>cy3sabiork</title>\n" +
			"\t<link rel=\"stylesheet\" href=\"./css/bootstrap.min.css\"\n" +
			"\t\t  integrity=\"sha384-1q8mTJOASx8j1Au+a5WDVnPi2lkFfwwEAa8hDDdjZlpLegxhjVME1fgjWPGmkzs7\" crossorigin=\"anonymous\">\n" +
			"\t<link rel=\"stylesheet\" href=\"./css/cy3sbml.css\">\n" +
			"</head>";

	private static final String HTML_STOP_TEMPLATE =
			"</div>\n" +
			"<script src=\"./js/jquery.min.js\"></script>\n" +
			"<script src=\"./js/bootstrap.min.js\" crossorigin=\"anonymous\"></script>\n" +
			"</body>\n" +
			"</html>\n";

    private static final String TRUE_HTML = "<img src=\"images/true2.png\" alt=\"true\" height=\"15\" width=\"15\"></img>";
    private static final String FALSE_HTML = "<img src=\"images/false2.png\" alt=\"false\" height=\"15\" width=\"15\"></img>";
    private static final String NONE_HTML = "<img src=\"images/none2.png\" alt=\"none\" height=\"15\" width=\"15\"></img>";

    private static final String TABLE_START = "<table class=\"table table-striped table-condensed table-hover\">";
    private static final String TABLE_END = "</table>";
    private static final String TS = "<tr><td><b>";
    private static final String TM = "</b></td><td>";
    private static final String TE = "<td/></tr>";

    private static final String TEMPLATE_MODEL =
            TABLE_START +
            TS + "L%sV%s" + TM + "<a href=\"%s\">(SBML file)</a>" + TE +
            TABLE_END;

    private static final String TEMPLATE_COMPARTMENT =
            TABLE_START +
            TS + "spatialDimensions" + TM + "%s" + TE +
            TS + "size" + TM + "%s [%s]" + TE +
            TS + "constant" + TM + "%s" + TE +
            TABLE_END;

    private static final String TEMPLATE_PARAMETER =
            TABLE_START +
            TS + "value" + TM + "%s [%s]" + TE +
            TS + "constant" + TM + "%s" +
            TABLE_END;

    private static final String TEMPLATE_INITIAL_ASSIGNMENT =
            TABLE_START +
            TS + "%s" + TM + "= %s" + TE +
            TABLE_END;

    private static final String TEMPLATE_RULE = TEMPLATE_INITIAL_ASSIGNMENT;

    private static final String TEMPLATE_LOCAL_PARAMETER =
            TABLE_START +
            TS + "value" + TM + "%s [%s]" + TE +
            TABLE_END;

     private static final String TEMPLATE_SPECIES =
             TABLE_START +
             TS + "compartment" + TM + "%s" + TE +
             TS + "value" + TM + "%s [%s]" + TE +
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
            TS + "units" + TM + "[%s]" + TE +
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


    private static final Logger logger = LoggerFactory.getLogger(SBaseHTMLFactory.class);
	private SBase sbase;
	private String info = ""; 
	
	public SBaseHTMLFactory(Object obj){
		sbase = (SBase) obj;
	}

    /** Get the created information string. */
	public String getInfo() {
	    return info;
	}

	public static void setBaseDir(String baseDir) {
        SBaseHTMLFactory.baseDir = baseDir;
    }


    /**
	 * Cache the information for the given sbase.
	 * TODO: cache the costly lookups and creations.
	 */
    public void cacheInformation(){
    	cacheMiriamInformation();
	}

	/**
	 * Cache the miriam information.
	 */
	private void cacheMiriamInformation(){
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
        info = String.format(HTML_START_TEMPLATE, baseDir);
		info += createHeader(sbase);
        info += createSBO(sbase);
        info += createSBase(sbase);
        info += createCVTerms(sbase);
        // TODO: implement
        // info += createHistory(sbase);
        info += createAnnotation(sbase);
        info += createNotes(sbase);
  		info += HTML_STOP_TEMPLATE;
	}
	
	/**
	 * Creates header HTML.
	 * Displays class information, in addition id and name if existing.
	 */
	private String createHeader(SBase item){
		String className = SBMLUtil.getUnqualifiedClassName(item);
		String header = String.format("<h2>%s</h2>", className);
		// if NamedSBase get additional information
		if (NamedSBase.class.isAssignableFrom(item.getClass())){
			NamedSBase nsb = (NamedSBase) item;
			String name = nsb.getId();
			if (nsb.isSetName()){
				name =  String.format("%s (%s)", nsb.getId(), nsb.getName());
			}
			header = String.format("<h2>%s <small>%s</small></h2>", className, name);
		}
		return header; 
	}

    /**
     * Creates SBO HTML.
     * TODO: handle as CVTerm.
     */
	private String createSBO(SBase item){
		String text = "";
		if (item.isSetSBOTerm()){
			String sboTermId = item.getSBOTermID();
			Term sboTerm = SBO.getTerm(sboTermId);
			String definition = parseSBOTermDefinition(sboTerm.getDefinition());

  			CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS, "http://identifiers.org/biomodels.sbo/" + sboTermId);
            String template =
                    "<b>%s</b> <span class=\"term\">%s</span> <span class=\"ontology\">SBO</span> <br />";
  			text += String.format(template, sboTerm.getName(), sboTermId);
  			text += definition + "<br />";
  			for (String rURI : term.getResources()){
  				text += createInfoForURI(rURI);
  			}
  		}
		return text;
	}

	/** Create HTML for CVTerms. */
	private String createCVTerms(SBase sbase){
        List<CVTerm> cvterms = sbase.getCVTerms();

		String text = "";
		if (cvterms.size() > 0){
			for (CVTerm term : cvterms){
			    CVTerm.Qualifier bmQualifierType = null;
				if (term.isModelQualifier()){
					bmQualifierType = term.getModelQualifierType();
				} else if (term.isBiologicalQualifier()){
					bmQualifierType = term.getBiologicalQualifierType();
				}
                text += String.format("<p><b>%s</b> <span class=\"qualifier\">%s</span>", term.getQualifierType(), bmQualifierType);
				
				Map<String, String> map = null;
				for (String rURI : term.getResources()){
					map = AnnotationUtil.getIdCollectionMapForURI(rURI);
					text += String.format("<span class=\"term\">%s</span> <span class=\"ontology\">%s</span><br/>", map.get("id"), map.get("collection").toUpperCase());
					text += createInfoForURI(rURI);
				}
				text += "</p>";
			}
		}
  		return text;
	}

    /**
     * Create annotation XML.
     */
	private String createAnnotation(SBase sbase){
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
                            text += StringEscapeUtils.escapeHtml(XMLNode.convertXMLNodeToString(child));
                        } catch(XMLStreamException e){
                            logger.error("Error parsing annotation xml");
                            e.printStackTrace();
                        }
                    }
                }
            }
            html = String.format("<code>%s</code>", text);
        }
        return html;
    }

    /**
     * Create HMTL for notes.
     * Have to be set at the end due to the html content which
     * breaks the rest of the html.
     */
    private String createNotes(SBase sbase){
        String html = "";
        if (sbase.isSetNotes()){
            try {
                String notes = sbase.getNotesString();
                if (!notes.equals("") && notes != null) {
                    html = String.format("<h2>Notes</h2><p>%s</p>", notes);
                }
            } catch (XMLStreamException e){
                logger.error("Error parsing notes xml");
                e.printStackTrace();
            }
        }
        return html;
    }
		
	/** 
	 * Creation of class specific attribute information.
	 */
	private String createSBase(SBase item){

		// Model //
        // TODO: add the package information, i.e. which packages are used in the model
		if (item instanceof Model){
			Model model = (Model) item;
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
            String units = getDerivedUnitString((AbstractNamedSBaseWithUnit) item);
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

	/////////////////////////////////////////////////////////////////////////////////////
    // Helper functions
    /////////////////////////////////////////////////////////////////////////////////////

    /** Creates true or false HTML depending on boolean. */
    private String booleanHTML(boolean b){
        return (b == true) ? TRUE_HTML : FALSE_HTML;
    }

    /** Derived unit string. */
    private String getDerivedUnitString(AbstractNamedSBaseWithUnit usbase){
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
     */
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
                items[k] = String.format("<a href=\"%s\">%s</a><br>", location, serverFromLocation(location));
            }
            text = org.apache.commons.lang3.StringUtils.join(items, "");

        } else {
            logger.warn("No locations for URI: " + resourceURI);
        }
        return text;
    }

    /** Get short server string from full location. */
    private static String serverFromLocation(String location) {
        // get everything instead of the last item
        String[] items = location.split("/");
        String[] serverItems = Arrays.copyOfRange(items, 0, items.length-1);
        String text = org.apache.commons.lang3.StringUtils.join(serverItems, "/");
        return text;
    }

    private String parseSBOTermDefinition(String definition){
        String[] tokens = definition.split("\"");
        String[] defTokens = (String []) ArrayUtils.subarray(tokens, 1, tokens.length-1);
        return StringUtils.join(defTokens, "\"");
    }

    /////////////////////////////////////////////////////////////////////////////////////
}
