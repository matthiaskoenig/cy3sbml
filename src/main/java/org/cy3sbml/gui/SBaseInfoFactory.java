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
public class SBaseInfoFactory {

	private static String HTML_START_TEMPLATE =
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

	private static String HTML_STOP_TEMPLATE =
			"</div>\n" +
			"<script src=\"./js/jquery.min.js\"></script>\n" +
			"<script src=\"./js/bootstrap.min.js\" crossorigin=\"anonymous\"></script>\n" +
			"</body>\n" +
			"</html>\n";

    private static String baseDir;

    private static final Logger logger = LoggerFactory.getLogger(SBaseInfoFactory.class);
	private SBase sbase;
	private String info = ""; 
	
	public SBaseInfoFactory(Object obj){
		sbase = (SBase) obj;
	}

    /** Get the created information string. */
	public String getInfo() {
	    return info;
	}

	public static void setBaseDir(String baseDir) {
        SBaseInfoFactory.baseDir = baseDir;
    }


	public void cacheMiriamInformation(){
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
        info += createSBase(sbase);
		info += createSBO(sbase);
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
  			text += String.format("<b>%s</b> <span class=\"term\">%s</span> <span class=\"ontology\">SBO</span> <br />", sboTerm.getName(), sboTermId);
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
	 * The general NamedSBase information is created in the 
	 * header. Here the Class specific attribute information is generated.
     * TODO: create map<attribute, value> and create the HTML from the map
	 */
	private String createSBase(SBase item){
		String text = "";

		// Model
		if (item instanceof Model){
			Model model = (Model) item;
			String template = "<b>L%sV%s</b> <a href=\"http://sbml-file\">(SBML file)</a>"; 
  			text = String.format(template, model.getLevel(), model.getVersion());
		}
		
		// Compartment
		else if (item instanceof Compartment){
			Compartment compartment = (Compartment) item;
			String template =
                    "<table class=\"table table-striped table-condensed table-hover\">" +
                    "<tr><td><b>spatialDimensions</b></td><td>%s<td/></tr>" +
                    "<tr><td><b>size</b></td><td>%s [%s]<td/></tr>" +
                    "<tr><td><b>constant</b></td><td>%s<td/></tr>" +
                    "</table>";
			String dimensions = "?";
			String size = noneHTML();
			String units = noneHTML();
			String constant = noneHTML();
			if (compartment.isSetSize()){
				size = ((Double)compartment.getSize()).toString();
			}
			if (compartment.isSetUnits()){
				units = compartment.getUnits();
			}
			if (compartment.isSetSpatialDimensions()){
				dimensions = ((Double) compartment.getSpatialDimensions()).toString();
			}
			if (compartment.isSetConstant()){
				constant = booleanHTML(compartment.getConstant());
			}
			text = String.format(template, dimensions, size, units, constant); 
		}
		// Parameter
		else if (item instanceof Parameter){
			Parameter parameter = (Parameter) item;
			String template = "<b>value</b>: %s [%s]<br />" +
							  "<b>constant</b>: %s";
			String value = noneHTML();
			String units = noneHTML();
			String constant = noneHTML();
			if (parameter.isSetValue()){
				value = ((Double) parameter.getValue()).toString();
			}
			if (parameter.isSetUnits()){
				units = parameter.getUnits();
			}
			if (parameter.isSetConstant()){
				constant = booleanHTML(parameter.getConstant());
			}
			text = String.format(template, value, units, constant); 
		}
		// InitialAssignment
		else if (item instanceof InitialAssignment){
			InitialAssignment assignment = (InitialAssignment) item;
            String template = "<b>%s</b> = %s";
            String variable = noneHTML();
            String math = noneHTML();
            if (assignment.isSetVariable()){
                variable = assignment.getVariable();
            }
            if (assignment.isSetMath()){
                math = assignment.getMath().toFormula();
            }
            text = String.format(template, variable, math);
		}
		// Rule
		else if (item instanceof Rule){
            Rule rule = (Rule) item;
            String template = "<b>%s</b> = %s";

            String math = noneHTML();
            String variable = SBMLUtil.getVariableFromRule(rule);
            if (variable == null){
                variable = noneHTML();
            }
            if (rule.isSetMath()){
                math = rule.getMath().toFormula();
            }
            text = String.format(template, variable, math);
		}

		// LocalParameter
		else if (item instanceof LocalParameter){
			LocalParameter parameter = (LocalParameter) item;
			String template = "<b>value</b>: %s [%s]";
			String value = noneHTML();
			String units = noneHTML();
			if (parameter.isSetValue()){
				value = ((Double) parameter.getValue()).toString();
			}
			if (parameter.isSetUnits()){
				units = parameter.getUnits();
			}
			text = String.format(template, value, units); 
		}
		
		// Species
		else if (item instanceof Species){
			Species species = (Species) item;
			String template = "<b>compartment</b>: %s<br />" +
							  "<b>value</b>: %s [%s]<br />" +
							  "<b>constant</b>: %s<br />" +
							  "<b>boundaryCondition</b>: %s";
			String compartment = noneHTML();
			String value = noneHTML();
			String units = noneHTML();
			String constant = noneHTML();
			String boundaryCondition = noneHTML();
			
			if (species.isSetCompartment()){
				compartment = species.getCompartment().toString();
			}
			if (species.isSetValue()){
				value = ((Double) species.getValue()).toString();
			}
			UnitDefinition udef = species.getDerivedUnitDefinition();
			if (udef != null){
				units = udef.toString();
			}
			if (species.isSetConstant()){
				constant = booleanHTML(species.isConstant());
			}
			if (species.isSetBoundaryCondition()){
				boundaryCondition = booleanHTML(species.getBoundaryCondition());
			}
			text = String.format(template, compartment, value, units, constant, boundaryCondition); 
		}
		
		// Reaction
		else if (item instanceof Reaction){
			Reaction reaction = (Reaction) item;
			String template = "<b>compartment</b>: %s<br />" +
							  "<b>reversible</b>: %s<br />" +
							  "<b>fast</b>: %s<br />" +
							  "<b>kineticLaw</b>: %s<br />" +
							  "<b>units</b>: [%s]";
			String compartment = noneHTML();
			String reversible = noneHTML();
			String fast = noneHTML();
			String kineticLaw = noneHTML();
			String units = noneHTML();
			
			if (reaction.isSetCompartment()){
				compartment = reaction.getCompartment().toString();
			}
			if (reaction.isSetReversible()){
				reversible = booleanHTML(reaction.getReversible());
			}
			if (reaction.isSetFast()){
				fast = booleanHTML(reaction.getFast());
			}
			if (reaction.isSetKineticLaw()){
				KineticLaw law = reaction.getKineticLaw();
				if (law.isSetMath()){
					kineticLaw = law.getMath().toFormula();	
				}
			}
			UnitDefinition udef = reaction.getDerivedUnitDefinition();
			if (udef != null){
				units = udef.toString();
			}
			text = String.format(template, compartment, reversible, fast, kineticLaw, units); 
		}
		// KineticLaw
		else if (item instanceof KineticLaw){
			KineticLaw law = (KineticLaw) item;
			String template = "<b>kineticLaw</b>: %s";
			String kineticLaw = noneHTML();
			if (law.isSetMath()){
				kineticLaw = law.getMath().toFormula();	
			}
			text = String.format(template, kineticLaw);
		}
		
		// QualitativeSpecies
		else if (item instanceof QualitativeSpecies){
			QualitativeSpecies species = (QualitativeSpecies) item;
			String template = "<b>compartment</b>: %s<br />" +
							  "<b>initial/max level</b>: %s/%s<br />" +
							  "<b>constant</b>: %s";
			String compartment = noneHTML();
			String initialLevel = noneHTML();
			String maxLevel = noneHTML();
			String constant = noneHTML();
			
			if (species.isSetCompartment()){
				compartment = species.getCompartment().toString();
			}
			if (species.isSetInitialLevel()){
				initialLevel = ((Integer) species.getInitialLevel()).toString();
			}
			if (species.isSetMaxLevel()){
				maxLevel = ((Integer) species.getMaxLevel()).toString();
			}
			if (species.isSetConstant()){
				constant = booleanHTML(species.getConstant());
			}
			text = String.format(template, compartment, initialLevel, maxLevel, constant); 
		}
		// Transition
		else if (item instanceof Transition){
			
		}
		// GeneProduct
		else if (item instanceof GeneProduct){
			
		}
		// FunctionDefinition
		else if (item instanceof FunctionDefinition){
			FunctionDefinition fd = (FunctionDefinition) item;
			String template = "<b>kineticLaw</b>: %s";
			String math = noneHTML();
			if (fd.isSetMath()){
				math = fd.getMath().toFormula();
			}
			text = String.format(template, math);
		}
		
		// comp:Port
		else if (item instanceof Port){
			Port port = (Port) item;
			String template = "<b>portRef</b>: %s <br />" +
							  "<b>idRef</b>: %s <br />" +
							  "<b>unitRef</b>: %s <br />" +
							  "<b>metaIdRef</b>: %s";
			String portRef = noneHTML();
			String idRef = noneHTML();
			String unitRef = noneHTML();
			String metaIdRef = noneHTML();
			if (port.isSetPortRef()){
				portRef = port.getPortRef();
			}
			if (port.isSetIdRef()){
				idRef = port.getIdRef();
			}
			if (port.isSetUnitRef()){
				unitRef = port.getUnitRef();
			}
			if (port.isSetMetaIdRef()){
				metaIdRef = port.getMetaIdRef();
			}
			text = String.format(template, portRef, idRef, unitRef, metaIdRef); 
		}
		return text;
	}

	/////////////////////////////////////////////////////////////////////////////////////
    // Helper functions
    /////////////////////////////////////////////////////////////////////////////////////
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


	private String trueHTML(){
	    return "<img src=\"images/true2.png\" alt=\"true\" height=\"15\" width=\"15\"></img>";
	}
	private String falseHTML(){
	    return "<img src=\"images/false2.png\" alt=\"false\" height=\"15\" width=\"15\"></img>";
	}
	private String noneHTML(){
	    return "<img src=\"images/none2.png\" alt=\"none\" height=\"15\" width=\"15\"></img>";
	}
	
	private String booleanHTML(boolean b){
		if (b == true){
			return trueHTML();
		} else {
			return falseHTML();
		}	
	}

    /////////////////////////////////////////////////////////////////////////////////////
}
