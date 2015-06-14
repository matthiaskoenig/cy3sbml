package biomodel;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/** Parsing of the form fields into usable SearchContent instance.
 * @author Matthias Koenig
 * @date 120530
 *
 */
public class SearchContent {
	
	public static final String CONTENT_NAME = "NAME";
	public static final String CONTENT_PERSON = "PERSON";
	public static final String CONTENT_PUBLICATION = "PUBLICATION";
	public static final String CONTENT_TAXONOMY = "TAXONOMY";
	public static final String CONTENT_CHEBI = "CHEBI";
	public static final String CONTENT_UNIPROT = "UNIPROT";
	
	public static final String CONTENT_MODE = "MODE";
	public static final String CONNECT_AND = "AND";
	public static final String CONNECT_OR = "OR";
	public static final String PARSED_IDS = "PARSED IDS";
	
	private List<String> names = new LinkedList<String>();
	private List<String> persons = new LinkedList<String>();
	private List<String> publications = new LinkedList<String>();
	private List<String> taxonomies = new LinkedList<String>();
	private List<String> chebis = new LinkedList<String>();
	private List<String> uniprots = new LinkedList<String>();
	
	private String searchMode;
	
	public SearchContent(HashMap<String, String> map){
		if (map.containsKey(CONTENT_NAME)){
			setNames(map.get(CONTENT_NAME));
		}
		if (map.containsKey(CONTENT_PERSON)){
			setPersons(map.get(CONTENT_PERSON));
		}
		if (map.containsKey(CONTENT_PUBLICATION)){
			setPublications(map.get(CONTENT_PUBLICATION));
		}
		if (map.containsKey(CONTENT_TAXONOMY)){
			setTaxonomies(map.get(CONTENT_TAXONOMY));
		}
		if (map.containsKey(CONTENT_CHEBI)){
			setChebis(map.get(CONTENT_CHEBI));
		}
		if (map.containsKey(CONTENT_UNIPROT)){
			setUniprots(map.get(CONTENT_UNIPROT));
		}
		if (map.containsKey(CONTENT_MODE)){
			searchMode = map.get(CONTENT_MODE);
		}
	}
	
	// Names
	private void setNames(String text){
		names = getTokensFromSearchText(text);
	}
	public List<String> getNames(){
		return names;
	}
	public String namesToString(String separator){
		return getListToString(names, separator);
	}	
	public boolean hasNames(){
		return (names.size() > 0);
	}
	
	// Persons
	private void setPersons(String text){
		persons = getTokensFromSearchText(text);
	}
	public List<String> getPersons(){
		return persons;
	}
	public boolean hasPersons(){
		return (persons.size() > 0);
	}
	public String personsToString(String separator){
		return getListToString(persons, separator);
	}	
	
	// Publications
	private void setPublications(String text){
		publications = getTokensFromSearchText(text);
	}
	public List<String> getPublications(){
		return publications;
	}
	public boolean hasPublications(){
		return (publications.size()>0);
	}
	public String publicationsToString(String separator){
		return getListToString(publications, separator);
	}	
	
	// Taxonomy
	private void setTaxonomies(String text){
		taxonomies = getTokensFromSearchText(text);
	}
	public List<String> getTaxonomies(){
		return taxonomies;
	}
	public boolean hasTaxonomies(){
		return (taxonomies.size()>0);
	}
	public String taxonomiesToString(String separator){
		return getListToString(taxonomies, separator);
	}	
	
	// Chebis
	private void setChebis(String text){
		chebis = getTokensFromSearchText(text);
	}
	public List<String> getChebis(){
		return chebis;
	}
	public boolean hasChebis(){
		return (chebis.size()>0);
	}
	public String chebisToString(String separator){
		return getListToString(chebis, separator);
	}
	
	// UniProt
	private void setUniprots(String text){
		uniprots = getTokensFromSearchText(text);
	}
	public List<String> getUniprots(){
		return uniprots;
	}
	public boolean hasUniprots(){
		return (uniprots.size()>0);
	}
	public String uniprotsToString(String separator){
		return getListToString(uniprots, separator);
	}	

	
	// Search Mode
	public String getSearchMode(){
		return searchMode; 
	}
	
	// Parsing form fields
	public List<String> getTokensFromSearchText(String text){
		String separator = " ";
		String pattern = "([\\s\\.,;:])+";
		text = text.replaceAll(pattern, separator);
		List<String> tokens = new LinkedList<String>();
		String[] tokenArray = text.split(separator);
		if (tokenArray != null){
			for (String token : tokenArray){
				token = token.replaceAll("[\\s]", "");
				if (token.length() > 0){
					tokens.add(token);
				}
			}
		}
		return tokens;
	}
	
	// Printing
	public String toHTML(){
		String sep = " ";
		String info = String.format("<table bgcolor=\"#C0C0C0\">"
				   + createHTMLTableRow("Name")
				   + createHTMLTableRow("Person")
				   + createHTMLTableRow("Publication")
				   + createHTMLTableRow("ChEBI")
				   + createHTMLTableRow("UniProt")
				   + createHTMLTableRow("Search Mode")
				   + "</table>", 
					namesToString(sep), 
					personsToString(sep), 
					publicationsToString(sep),
					chebisToString(sep),
					uniprotsToString(sep),
					searchMode.toString());
		return info;
	}
	private String createHTMLTableRow(String att){
		return "<tr><td><font size=\"-1\"><b>" + 
				att +
				"</b></font></td></td><font size=\"-1\">%s</font></td></tr>";
	}
	
	public String toString(){
		String sep = " ";
		String info = String.format("Name : %s\n"
								   +"Person : %s\n"
								   +"Publication : %s\n"
								   +"ChEBI : %s\n"
								   +"UniProt : %s\n"
								   +"Mode : %s\n", 
									namesToString(sep), 
									personsToString(sep), 
									publicationsToString(sep),
									chebisToString(sep),
									uniprotsToString(sep),
									searchMode.toString());
		return info;
	}
	
	public String getArrayToString(String[] tokens, String separator){
		String info = "";
		for (String token: tokens){
			info += token + separator;
		}
		return info;
	}
	
	public String getListToString(List<String> tokens, String separator){
		String info = "";
		for (String token: tokens){
			info += token + separator;
		}
		return info;
	}
	
	///// TESTING ////////////////////
	public static void test(){
		String name = "ABC asdf  test, ";
		String person = "König, Bölling;; ,";
		String publication = "PMID:12345";
		
		HashMap<String, String> map = new HashMap<String, String>();
		map.put(SearchContent.CONTENT_NAME, name);
		map.put(SearchContent.CONTENT_PERSON, person);
		map.put(SearchContent.CONTENT_PUBLICATION, publication);
		map.put(SearchContent.CONTENT_MODE, SearchContent.CONNECT_AND);
		
		SearchContent content = new SearchContent(map);
		String info = content.toString();
		System.out.println(info);
		
	}
	
	public static void main(String[] args){
		test();
	}
}
