package org.cy3sbml.biomodelrest.rest;

import com.mashape.unirest.http.Unirest;
import org.cy3sbml.ResourceExtractor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/** 
 * Manage biomodel query suggestions (facets).
 */

public class QuerySuggestions implements Serializable {

    private static String[] KEYWORDS = {
            "modelformat",
            "curationstatus",
            "modellingapproach",
            "modelflag",
            "disease",
            "GO",
            "UNIPROT",
            "CHEBI",
            "ensemble",
            "TAXONOMY"
    };

    private static String[] MODELFORMAT = {
            "SBML",
            "Original code",
            "MATLAB (Octave)"
    };
    private static String[] CURATIONSTATUS = {
            "Non-curated",
            "Manually curated",
    };
    private static String[] MODELLINGAPPROACH = {
            "Ordinary differential equation model",
            "Constraint-based model",
            "Logical model",
            "Petri net"
    };
    private static String[] MODELFLAG = {
            "Non Kinetic",
            "Non Miriam",
            "Sbml Extended",
    };


	private TreeSet<String> keywords;
	private HashMap<String, TreeSet<String>> suggestions;

	public QuerySuggestions(TreeSet<String> keywords, HashMap<String, TreeSet<String>> suggestions){
	    this.keywords = keywords;
	    this.suggestions = suggestions;
    }


    /**
     * Create QuerySuggestions from static data.
     * @return
     */
	public static QuerySuggestions fromStaticData(){

	    TreeSet<String> keywords = new TreeSet<>();
        HashMap<String, TreeSet<String>> suggestions = new HashMap<>();

	    for (String keyword: KEYWORDS) {
            keywords.add(keyword);
            TreeSet<String> fields = new TreeSet<String>();
            String[] values = null;
            switch (keyword) {
                case "modelformat":
                    values = MODELFORMAT;
                    break;
                case "curationstatus":
                    values = CURATIONSTATUS;
                    break;
                case "modellingapproach":
                    values = MODELLINGAPPROACH;
                    break;
                case "modelflag":
                    values = MODELFLAG;
                    break;
            }
            if (values != null) {
                for (String value : values) {
                    fields.add(value);
                }
            }
            suggestions.put(keyword, fields);
        }
        return new QuerySuggestions(keywords, suggestions);
    }

	/** Get keywords for which suggestions exist. */
	public TreeSet<String> getKeywords(){
	    return keywords;
	}
	
	/** Suggestions. */
	public TreeSet<String> getSuggestionsForKeyword(String key){
	    return suggestions.get(key);
	}

	/** Print suggestion information to console. */
	public void print(){
		System.out.println("-------------------------------------------------");
		for (String s: keywords){
			System.out.println(s);
		}
		System.out.println("-------------------------------------------------");
		
		for (String key: new TreeSet<String>(suggestions.keySet())){
			System.out.print(key + " : " + suggestions.get(key).size());
			System.out.println();
		}
	}
	

	public static void main(String[] args){
		QuerySuggestions suggestions = QuerySuggestions.fromStaticData();
		suggestions.print();
	}
	
}
