package org.cy3sbml.miriam;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for looking up terms via the Ontology Lookup Service.
 *      http://www.ebi.ac.uk/ols/index
 * with REST API documented here
 *      http://www.ebi.ac.uk/ols/docs/api
 *      http://stateless.co/hal_specification.html
 *
 * Use UniRest for REST queries.
 */
public class OntologyLookupTest {
    private static final Map<String, String> ONTOLOGY_MAP;

    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("biomodels.sbo", "sbo");
        map.put("Substrate", "Compound");
        map.put("Product", "Compound");
        map.put("Inhibitor", "Compound");
        map.put("Catalyst", "Compound");
        map.put("Cofactor", "Compound");
        map.put("Activator", "Compound");
        map.put("OtherModifier", "Compound");
        map.put("AnyRole", "Compound");
        map.put("Enzymename", "Enzyme");
        map.put("PubMedID", "PubmedID");
        map.put("KeggCompoundID", "KEGGCompoundID");
        map.put("KeggReactionID", "KEGGReactionID");
        map.put("SabioCompoundID", "SABIOCompoundID");
        map.put("SabioReactionID", "SABIOReactionID");
        map.put("ChebiID", "CHEBICompoundID");
        map.put("PubChemID", "PUBCHEMCompoundID");
        ONTOLOGY_MAP = Collections.unmodifiableMap(map);
    }


    /**
     * Create client and perform query.
     * @throws UnirestException
     */
    public static JSONObject olsQuery(String uri) {
        JSONObject jsonObject = null;
        try {
            HttpResponse<JsonNode> response = Unirest.get(uri).asJson();
            Integer status = response.getStatus();
            if (status != 200){
                System.out.println("Query unsuccessful, status <" + status +">");
                return null;
            }

            JsonNode node = response.getBody();
            if (false) {
                System.out.println("--------------------------------------------");
                System.out.println(node.toString());
                System.out.println("--------------------------------------------");
            }
            jsonObject = node.getObject();
        } catch (UnirestException e){
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * Get all available ontologies from OLS.
     * Uses the ontology mapping.
     */
    public static HashMap<String, Ontology> getOntologies(){

    }


	/*
	 * Test the Restful API.
	 *
	 * For Json parsing see
	 *  http://stackoverflow.com/questions/2591098/how-to-parse-json-in-java
	 *
	 *      <rdf:li rdf:resource="http://identifiers.org/biomodels.sbo/SBO:0000247" />
     *      <rdf:li rdf:resource="http://identifiers.org/chebi/CHEBI:25858" />
     *      <rdf:li rdf:resource="http://identifiers.org/kegg.compound/C13747" />
     */
    public static void main(String[] args){
        JSONObject jsonNode = olsQuery("http://www.ebi.ac.uk/ols/api/ontologies?size=500");

        JSONArray ontologies = jsonResponse.getJSONObject("_embedded").getJSONArray("ontologies");

        Gson g = new Gson();
        for (int i=0; i<ontologies.length(); i++){
            JSONObject jsonOntology = ontologies.getJSONObject(i);
            Ontology ontology = g.fromJson(jsonOntology.toString(), Ontology.class);
            System.out.println(" --> " + ontology.ontologyId + "\t" + ontology.loaded);


        }

    }
}
