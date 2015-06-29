package org.cy3sbml.miriam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

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
import org.sbml.jsbml.ext.fbc.GeneProduct;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;

import uk.ac.ebi.miriam.lib.MiriamLink;

/** 
 * Get information from the MIRIAM web resource for given NamedSBase object.
 * The created information objects are cached to reduce load on the 
 * web services.
 * 
 * TODO: move MIRIAM requests to file version.
 * TODO: refactor information when moving to full HTML5 support 
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
				objClass.equals(Reaction.class) || 
				objClass.equals(Species.class) ||
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
	
	/** 
	 * Get information for the given Object.
	 * TODO: read interesting information for given NamedSBase and present. 
	 */
	public void createInfo() throws XMLStreamException {
		if (sbmlObject == null){
			return;
		}
		// SBML information
		info = createHeader(sbmlObject);
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
		if (item.isSetName()){
			return String.format("<h2><span color=\"gray\">%s</span> : %s (%s)</h2>",
					getUnqualifiedClassName(item), item.getId(), item.getName());	
		} else {
			return String.format("<h2><span color=\"gray\">%s</span> : %s</h2>",
					getUnqualifiedClassName(item), item.getId());
		}
		
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
		String info = "";
		if (item.isSetSBOTerm()){
  			info = getSBOTermString(item.getSBOTermID());
  		}
		return info;
	}
	
	private String getSBOTermString(String sboTerm){
		String text = "<p>";
		CVTerm term = new CVTerm(CVTerm.Qualifier.BQB_IS, "urn:miriam:biomodels.sbo:" + sboTerm);
		text += String.format("<b><span color=\"green\">%s</span></b><br>", sboTerm);
		for (String rURI : term.getResources()){
			text += MiriamResourceInfo.getInfoFromURI(link, rURI);
		}
		text += "</p>";
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
	 */
	private Map<String, String> getMapForURI(final String rURI) {
		Map<String, String> map = new HashMap<String, String>();
		String[] items = rURI.split("/");
		map.put("id", items[items.length - 1]);
		map.put("key", StringUtils.join(ArrayUtils.subarray(items, 0, items.length-1), "/"));
		return map;
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
