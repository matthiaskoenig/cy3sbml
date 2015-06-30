package org.cy3sbml.miriam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.cy3sbml.SBML;
import org.cy3sbml.util.AttributeUtil;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sbml.jsbml.AbstractNamedSBase;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Parameter;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.UnitDefinition;
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;

import uk.ac.ebi.miriam.lib.MiriamLink;

/** 
 * Create the information for the selected NamedSBase.
 * Core information is parsed from the NamedSBase object,
 * with additional information like resources retrieved via
 * web services (MIRIAM).
 * Here the HTML information string is created which is displayed
 * on selection of SBML objects in the graph.
 */
public class NamedSBaseInfoFactory {
	private static final Logger logger = LoggerFactory.getLogger(NamedSBaseInfoFactory.class);
	
	private MiriamLink link;
	private AbstractNamedSBase sbmlObject;
	private String info = ""; 
	
	public NamedSBaseInfoFactory(Object obj){
		Class<? extends Object> objClass = obj.getClass();
		if (    objClass.equals(Model.class)  ||
				objClass.equals(Compartment.class)  || 
				objClass.equals(Parameter.class)  ||
				objClass.equals(Species.class) ||
				objClass.equals(Reaction.class) || 
				objClass.equals(QualitativeSpecies.class) ||
				objClass.equals(Transition.class) ||
				objClass.equals(GeneProduct.class)){
			sbmlObject = (AbstractNamedSBase) obj;
		}
		// WebService Link
		link = MiriamWebservice.getMiriamLink();
	}
	
	public String getInfo() {
		return info;
	}
	
	public void cacheMiriamInformation(){
		for (CVTerm term : sbmlObject.getCVTerms()){
			for (String rURI : term.getResources()){
				MiriamResourceInfo.getLocationsFromURI(link, rURI);
			}
		}
	}
	
	/** Parse and create information for the current sbmlObject. */
	public void createInfo() throws XMLStreamException {
		if (sbmlObject == null){
			// unsupported classes
			return;
		}
		// SBML information
		info = createHeader(sbmlObject);
		info += createNamedSBaseInfo(sbmlObject);
		info += createSBOInfo(sbmlObject);
  		
  		// CVterm annotations (MIRIAM action)
		List<CVTerm> terms = sbmlObject.getCVTerms();
  		info += getCVTermsString(terms);
  		
  		// notes and annotations if available
  		// !!! have to be set at the end due to the html content which
  		// breaks the rest of the html.
  		String notes = sbmlObject.getNotesString();
  		if (!notes.equals("") && notes != null ){
  			info += String.format("<p>%s</p>", notes);
  		}
	}
	
	private String createHeader(AbstractNamedSBase item){
		String template = "<h2><span color=\"gray\">%s</span> : %s</h2>";
		String name = item.getId();
		if (item.isSetName()){
			name =  String.format("%s (%s)", item.getId(), item.getName());
		}
		return String.format(template, getUnqualifiedClassName(item), name);
	}
	
	/** Returns the unqualified class name of a given object. */
	private static String getUnqualifiedClassName(Object obj){
		String name = obj.getClass().getName();
		if (name.lastIndexOf('.') > 0) {
		    name = name.substring(name.lastIndexOf('.')+1);
		}
		// The $ can be converted to a .
		name = name.replace('$', '.');  
		return name;
	}
	
	private String createSBOInfo(AbstractNamedSBase item){
		String text = "";
		if (item.isSetSBOTerm()){
			String sboTerm = item.getSBOTermID();
  			text = "<p>";
  			CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS, "urn:miriam:biomodels.sbo:" + sboTerm);
  			text += String.format("<b><span color=\"green\">%s</span></b><br>", sboTerm);
  			for (String rURI : term.getResources()){
  				text += MiriamResourceInfo.getInfoFromURI(link, rURI);
  			}
  			text += "</p>";
  		}
		return text;
	}
	
	
	/** Get string HTML representation of the CVTerm information. */
	private String getCVTermsString(List<CVTerm> cvterms){
		String text = "<hr>";
		if (cvterms.size() > 0){
			for (CVTerm term : cvterms){
				if (term.isModelQualifier()){
					text += String.format("<p><b>%s : %s</b><br>", term.getQualifierType(), term.getModelQualifierType());	
				} else if (term.isBiologicalQualifier()){
					text += String.format("<p><b>%s : %s</b><br>", term.getQualifierType(), term.getBiologicalQualifierType());
				}
				
				Map<String, String> map = null;
				for (String rURI : term.getResources()){
					map = getMapForURI(rURI);
					text += String.format("<span color=\"red\">%s</span> (%s)<br>", map.get("id"), map.get("key"));
					text += MiriamResourceInfo.getInfoFromURI(link, rURI);
				}
				text += "</p>";
			}
			text += "<hr>";
		}
  		return text;
	}
	
	/**
	 * Split the information in url, resource, id.
	 * Examples are:
	 * 		<rdf:li rdf:resource="http://identifiers.org/chebi/CHEBI:17234"/>
	 * 		<rdf:li rdf:resource="http://identifiers.org/kegg.compound/C00293"/>
	 * TODO: handle URNs
	 * TODO: shorten the urls
	 * 		"urn:miriam:kegg.compound:C00197" ()
	 */
	private Map<String, String> getMapForURI(final String rURI) {
		Map<String, String> map = new HashMap<String, String>();
		String[] items = rURI.split("/");
		map.put("id", items[items.length - 1]);
		map.put("key", StringUtils.join(ArrayUtils.subarray(items, 0, items.length-1), "/"));
		return map;
	}
	
	/** 
	 * The general NamedSBase information is created in the 
	 * header. Here the Class specific attribute information is generated.
	 */
	private String createNamedSBaseInfo(AbstractNamedSBase item){
		String text = "";
		// Model
		if (item instanceof Model){
			Model model = (Model) item;
			String template = "<b>L%sV%s</b>"; 
  			text = String.format(template, model.getLevel(), model.getVersion());
		}
		// Compartment
		else if (item instanceof Compartment){
			Compartment compartment = (Compartment) item;
			String template = "<b>spatialDimensions</b>: %s<br />" +
							  "<b>size</b>: %s [%s]<br />" +
							  "<b>constant</b>: %s";
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
				kineticLaw = reaction.getKineticLaw().getMath().toFormula();	
			}
			UnitDefinition udef = reaction.getDerivedUnitDefinition();
			if (udef != null){
				units = udef.toString();
			}
			text = String.format(template, compartment, reversible, fast, kineticLaw, units); 
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
		return text;
	}
	
	private String trueHTML(){
		return "<img src=\"images/true.gif\" alt=\"true\" height=\"15\" width=\"15\"></img>";
	}
	private String falseHTML(){
		return "<img src=\"images/false.gif\" alt=\"false\" height=\"15\" width=\"15\"></img>";
	}
	private String noneHTML(){
		return "<img src=\"images/none.gif\" alt=\"none\" height=\"15\" width=\"15\"></img>";
	}
	
	private String booleanHTML(boolean b){
		if (b == true){
			return trueHTML();
		} else {
			return falseHTML();
		}	
	}
	
	
	
	
	/** Get additional image information for the database and identifier.
	 * TODO: This has to be done offline and in the background (images have to be cashed) !
	 * TODO: Create background database of information.  
	 */
	/*
	@Deprecated
	private String getAdditionalInformation(String r){
		Map<String, String> map = getMapForURI(r);
		String text = "";
		String id = map.get("id");
		String key = map.get("key");
		String[] keyitems = key.split(":");
		String item = keyitems[keyitems.length-1];
		
		// Add chebi info
		if (item.equals("obo.chebi")){
			try{
				String[] tmps = id.split("%3A");
				String cid = tmps[tmps.length-1];
				text += "<img src=\"http://www.ebi.ac.uk/chebi/displayImage.do?defaultImage=true&imageIndex=0&chebiId="+cid+"&dimensions=160\"></img>";
			} catch (Exception e){
				//e.printStackTrace();
				logger.warn("obo.chebi image not available");
			}
		
		// Add kegg info
		}else if (item.equals("kegg.compound")){
			try{
				String imgsrc = 
					NamedSBaseInfoFactory.class.getClassLoader().getResource("http://www.genome.jp/Fig/compound/"+id+".gif").toString();
				text += "<img src=\""+imgsrc+"\"></img>";
			} catch (Exception e){
				//e.printStackTrace();
				logger.warn("kegg.compound image not available");
			}
		
		// TODO resize image and use KEGG
		// Uncomment for reactions, but problems with large images 
//		}else if (item.equals("kegg.reaction")){
//			try{
//				String imgsrc = 
//					BioModelText.class.getClassLoader().getResource("http://www.genome.jp/Fig/reaction/"+id+".gif").toString();
//				text += "<img src=\""+imgsrc+"\"></img>";
//			} catch (Exception e){
//				//e.printStackTrace();
//				System.out.println("CySBML -> kegg.reaction image not available");
//			}
//		}
			
			
		}
		return text;
	}
	*/
	
}
