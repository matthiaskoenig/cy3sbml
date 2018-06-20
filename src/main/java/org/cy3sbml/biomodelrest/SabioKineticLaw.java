package org.cy3sbml.biomodelrest;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import org.sbml.jsbml.*;
import org.sbml.jsbml.CVTerm.Qualifier;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Class for the representation of Kinetic Entries from SABIO-RK.
 *
 * Used in the results table.
 */
@SuppressWarnings("restriction")
public class SabioKineticLaw {
	private final SimpleIntegerProperty count;
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty organism;
    private final SimpleStringProperty tissue;
    private final SimpleStringProperty reaction;
    
 
    public SabioKineticLaw(Integer count, Integer id, String organism, String tissue, String reaction) {
    	this.count = new SimpleIntegerProperty(count);
    	this.id = new SimpleIntegerProperty(id);
        this.organism = new SimpleStringProperty(organism);
        this.tissue = new SimpleStringProperty(tissue);
        this.reaction = new SimpleStringProperty(reaction);
    }
 
    public Integer getCount(){
    	return count.get();
    }
    public Integer getId(){
    	return id.get();
    }
    public String getOrganism(){
    	return organism.get();
    }
    public String getTissue(){
    	return tissue.get();
    }
    public String getReaction(){
    	return reaction.get();
    }
    
    /**
	 * Parses Kinetic Law Ids from given text string.
	 *
	 * The ids can be separated by different separators, i.e.
	 * 	'\n', '\t', ' ', ';' or','
	 */
    public static HashSet<Integer> parseIds(String text){
    	HashSet<Integer> ids = new HashSet<Integer>();
		
    	// unify separators
    	text = text.replace("\n", ",");
    	text = text.replace("\t", ",");
    	text = text.replace(" ", ",");
    	text = text.replace(";", ",");
    	
    	String[] tokens = text.split(",");
    	for (String t : tokens){
        	// single entry parsing
    		if (t.length() == 0){
    			continue;
    		}
    		
        	try {
        		Integer kineticLaw = Integer.parseInt(t);
        		ids.add(kineticLaw);
        	} catch (NumberFormatException e) {
        		System.out.println("Kinetic Law Id could not be parsed from token: <" + t + ">");
        	}
    	}
    	return ids;
    }
    
	/** 
	 * Read SabioKineticLaws from given SBML. 
	 * 
	 * Information to populate the results panel is parsed here. 
	 * This uses directly the response string.
	 */
	public static ArrayList<SabioKineticLaw> parseKineticLaws(SBMLDocument doc){
		ArrayList<SabioKineticLaw> list = new ArrayList<SabioKineticLaw>();
		if (doc == null){
			return list;
		}

		// necessary to read information from annotations
		Model model = doc.getModel();
		Integer count = 1;
		for (Reaction r : model.getListOfReactions()){
			Integer kid = getKineticLawIdFromReaction(r);
			String organism = getOrganismFromReaction(r);
			String tissue = getTissueFromReaction(r);
			String reaction = getDescriptionFromReaction(r);

			list.add(new SabioKineticLaw(count, kid, organism, tissue, reaction));
			count++;
		}

		return list;
	}
	
	/** Parse SABIO-RK kinetic law id from reaction. */
	private static Integer getKineticLawIdFromReaction(Reaction r){
		KineticLaw law = r.getKineticLaw();
		// FIXME: bad hack to get the id, necessary to read the XML
		String metaId = law.getMetaId();
		String[] tokens = metaId.split("_");
		Integer kid = Integer.parseInt(tokens[tokens.length-1]);
		return kid;
	}

	/** Parse reaction description from reaction. */
	private static String getDescriptionFromReaction(Reaction r){
		String description = r.toString();
		for (ModifierSpeciesReference modifier: r.getListOfModifiers()){
			Species species = modifier.getSpeciesInstance();
			if (species.isSetName()){
				description = species.getName();
				break;
			}
		}
		return description;
	}
	
	/** Parse organism information from reaction annotation. */
	private static String getOrganismFromReaction(Reaction r){
		// BQB_HAS_TAXON in RDF
		String organism = "-";
		for (CVTerm cv: r.getCVTerms()){
			if (cv.isSetBiologicalQualifierType() && cv.getBiologicalQualifierType() == Qualifier.BQB_HAS_TAXON){
				if (cv.getResourceCount() > 0){
					String uri = cv.getResourceURI(0);
					String[] tokens = uri.split("/");
					organism = tokens[tokens.length-1];
					break;
				}
			}
		}
		
		return organism;
	}
	
	/** 
	 * Parse tissue information from reaction annotation.
	 *
	 * The information is currently not available in SABIO-RK SBML.
	 * see https://github.com/matthiaskoenig/cy3sabiork/issues/10
	 */
	private static String getTissueFromReaction(Reaction r){
		String tissue = "-";
		return tissue;
	}

}