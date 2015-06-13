package org.cy3sbml.miriam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import javax.xml.stream.XMLStreamException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sbml.jsbml.AbstractNamedSBase;
import org.sbml.jsbml.CVTerm;
import org.sbml.jsbml.Compartment;
import org.sbml.jsbml.Model;
import org.sbml.jsbml.Reaction;
import org.sbml.jsbml.Species;
import org.sbml.jsbml.ext.qual.QualitativeSpecies;
import org.sbml.jsbml.ext.qual.Transition;

import uk.ac.ebi.miriam.lib.MiriamLink;

/** Gets information from the MIRIAM web resource for given 
 * NamedSBase object.
 * The created information objects should be cached in the background
 * to reduce the webservice overhead. I.e. every resource should be
 * retrieved maximally one time. 
 * 
 * FIXME: offline mode & better overview over the information in the SBML
 *  Holds the available link information for already read resources (Store & Read Cache)
 *   
 * TODO: bug if nodes are selected with no correspondence in the SBML.
 * TODO: get the additional information from KEGG, ...
 * TODO: use ontology lookup service to retrieve additional information
 */
public class NamedSBaseInfoFactory {
	private static final Logger logger = LoggerFactory.getLogger(NamedSBaseInfoFactory.class);
	
	private MiriamLink link;
	private AbstractNamedSBase sbmlObject;
	private String info = ""; 
	
	public NamedSBaseInfoFactory(Object obj){
		
		if (    obj.getClass().equals(Model.class)  ||
				obj.getClass().equals(Compartment.class)  || 
	  			obj.getClass().equals(Reaction.class) || 
	  			obj.getClass().equals(Species.class) ||
	  			obj.getClass().equals(QualitativeSpecies.class) ||
	  			obj.getClass().equals(Transition.class) ){
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
	
	/** Get information for the given Object 
	 * @throws XMLStreamException */
	public void createInfo() throws XMLStreamException {
		if (sbmlObject == null){
			return;
		}
		
		// SBML information
		info = createHeader(sbmlObject);
		info += createSBOInfo(sbmlObject);
  		// Add type specific additional SBML information
  		// TODO: read the const, boundary condition, ... kinetic law, assignment, ...
  		
  		// CVterm annotations (MIRIAM action)
  		info += getCVTermsString(sbmlObject.getCVTerms());
  		
  		// notes and annotations if available
  		// !!! have to be set at the end due to the html content which
  		// breaks the rest of the html.
  		String notes = sbmlObject.getNotesString();
  		if (!notes.equals("") && notes != null ){
  			info += String.format("<p>%s</p>", notes);
  		}
  		// TODO: read everything available according to standard
	   }
	
	
	private String createHeader(AbstractNamedSBase item){
		return String.format("<h2><span color=\"gray\">%s</span> : %s (%s)</h2>",
								getUnqualifiedClassName(item), item.getId(), item.getName());
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
	
	
	/** Returns the SBOTermId if available. */
	private String createSBOInfo(AbstractNamedSBase item){
		String info = "";
		if (item.isSetSBOTerm()){
  			info = getSBOTermString(item.getSBOTermID());
  		}
		return info;
	}
	
	/** Public get SBOTerm string */
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
	
	
	/** Get a String HTML representation of the CVTerm information */
	private String getCVTermsString(List<CVTerm> cvterms){
		String text = "<hr>";
		if (cvterms.size() > 0){
			for (CVTerm term : cvterms){
				text += String.format("<p><b>%s : %s</b><br>", term.getQualifierType(), term.getBiologicalQualifierType());
				Map<String, String> map = null;
				for (String rURI : term.getResources()){
					map = getKeyAndId(rURI);
					text += String.format("<span color=\"red\">%s</span> (%s)<br>", map.get("id"), map.get("key"));
					text += MiriamResourceInfo.getInfoFromURI(link, rURI);
				}
				text += "</p>";
			}
			text += "<hr>";
		}
  		return text;
	}
	
	/** Get additional image information for the database and identifier.
	 * TODO: This has to be done offline and in the background (images have to be cashed) !
	 * TODO: Create background database of information.  
	 */
	@Deprecated
	private String getAdditionalInformation(String r){
		Map<String, String> map = getKeyAndId(r);
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
		/* Uncomment for reactions, but problems with large images 
		}else if (item.equals("kegg.reaction")){
			try{
				String imgsrc = 
					BioModelText.class.getClassLoader().getResource("http://www.genome.jp/Fig/reaction/"+id+".gif").toString();
				text += "<img src=\""+imgsrc+"\"></img>";
			} catch (Exception e){
				//e.printStackTrace();
				System.out.println("CySBML -> kegg.reaction image not available");
			}
		}*/
			
			
		}
		return text;
	}
	
	@Deprecated
	private Map<String, String> getKeyAndId(final String rURI) {
		Map<String, String> map = new HashMap<String, String>();
		// split into key and identifier
		String[] items = rURI.split(":");
		String[] keyitems = new String[items.length - 1];
		for (int i = 0; i < keyitems.length; ++i) {
			keyitems[i] = items[i];
		}
		map.put("id", items[items.length - 1]);
		map.put("key", StringUtils.join(keyitems, ":"));
		return map;
	}
	
}
