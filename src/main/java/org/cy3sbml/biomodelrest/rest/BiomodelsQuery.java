package org.cy3sbml.biomodelrest.rest;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

import org.cy3sbml.biomodelrest.BiomodelsQueryResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biomodels.ws.BioModelsWSClient;
import uk.ac.ebi.biomodels.ws.BioModelsWSException;
import uk.ac.ebi.biomodels.ws.SimpleModel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import org.json.*;


/*
Search fields:

query=*:*
modelformat (SBML, Original code, MATLAB (Octave))
    SBML
    Original code
    MATLAB (Octave)

curationstatus (Non-curated, Manually curated)
    Non-curated (7806)
    Manually curated (655)

modellingapproach
    Ordinary differential equation model (708)
    Constraint-based model (129)
    Logical model (17)
    Petri net (3)

modelflag
    Non Kinetic (450)
    Non Miriam (18)
    Sbml Extended

organisms
    Homo sapiens (7280)
    Saccharomyces cerevisiae (131)
    Mammalia (93)
    Mus musculus (92)
    cellular organisms (77)
    Escherichia coli (43)
    Rattus (36)
    Eukaryota (27)
    Xenopus laevis (23)
    Arabidopsis thaliana (22)
    Rattus norvegicus (22)
    Oryctolagus cuniculus (17)
    Viridiplantae (14)
    Cavia porcellus (12)
    Arabidopsis (12)
    Chordata (11)
    Drosophila melanogaster (11)
    Canis lupus familiaris (10)
    Murinae (10)
    Trypanosoma brucei (10)
    Schizosaccharomyces pombe (9)
    Neurospora crassa (7)
    Halobacterium salinarum (5)
    Metazoa (5)
    Escherichia coli K-12 (5)
    Mycobacterium tuberculosis (5)
    Methanosarcina barkeri (4)
    Helicobacter pylori (4)
    Bos taurus (4)
    Drosophila (4)
    Opisthokonta (4)
    Lactococcus lactis (4)
    Amphibia (4)
    Bacillus subtilis (4)
    Mus (3)
    Yarrowia lipolytica (3)
    Komagataella pastoris (3)
    Mus sp. (3)
    Plasmodium falciparum (3)
    Felis catus (3)
    Bacteria (3)
    Rattus rattus (3)
    Leishmania (3)
    Vertebrata (3)
    Saccharomyces cerevisiae S288C (3)
    Fusarium verticillioides (2)
    Aspergillus fischeri (2)
    Phanerochaete chrysosporium (2)
    Magnaporthe grisea (2)
    Clavispora lusitaniae (2)
    Histoplasma capsulatum (2)
    Postia placenta (2)
    Coccidioides immitis (2)
    Batrachochytrium dendrobatidis (2)
    Rhizopus oryzae (2)
    Candida tropicalis (2)
    Zymoseptoria tritici (2)
    Cryptococcus neoformans (2)
    Scheffersomyces stipitis (2)
    Balanus nubilus (2)
    Aspergillus terreus (2)
    Fusarium oxysporum (2)
    Equus caballus (2)
    [Nectria] haematococca (2)
    Rhodobacter sphaeroides (2)
    Trichoderma reesei (2)
    Sporobolomyces roseus (2)
    Aspergillus clavatus (2)
    Encephalitozoon cuniculi (2)
    Ustilago maydis (2)
    Debaryomyces hansenii (2)
    Parastagonospora nodorum (2)
    Kluyveromyces lactis (2)
    Candida albicans (2)
    Aspergillus fumigatus (2)
    Human immunodeficiency virus 1 (2)
    Puccinia graminis (2)
    Blattabacterium sp. (2)
    [Candida] glabrata (2)
    Chaetomium globosum (2)
    Coprinopsis cinerea (2)
    Sclerotinia sclerotiorum (2)
    Ovis aries (2)
    Tetronarce californica (2)
    Candidatus Sulcia muelleri (2)
    Homalodisca vitripennis (2)
    Candidatus Baumannia cicadellinicola (2)
    Aspergillus oryzae (2)
    Lodderomyces elongisporus (2)
    Macaca mulatta (2)
    Chlamydomonas reinhardtii (2)
    Fusarium graminearum (2)
    Armoracia rusticana (2)
    Aspergillus niger (2)
    Uncinocarpus reesii (2)
    Phycomyces blakesleeanus (2)
    Schizosaccharomyces japonicus (2)
    Clostridioides difficile (2)
    Oryctolagus (2)
    Ostreococcus tauri (2)


disease
go
uniprot
chebi
ensemble
*/




/**
 * UniRest based Sabio Queries.
 */
public class BiomodelsQuery {
	private static Logger logger = LoggerFactory.getLogger(BiomodelsQuery.class);
	public static final String BIOMODELS_RESTFUL_URL = "https://wwwdev.ebi.ac.uk/biomodels";
	public static final String CONNECTOR_AND = " AND ";

	/**
	 * Create URI from query String.
	 *
	 * Performs necessary replacements and sanitation of query.
	 */
	public static URI uriFromQuery(String query) throws URISyntaxException {
		URI uri = new URI(BIOMODELS_RESTFUL_URL + query);
		return uri;
	}

	public static BiomodelsQueryResult performQuery(String query){
		HttpResponse<InputStream> response = executeQuery(query);
		if (response != null){
			Integer status = response.getStatus();
			String json = null;
			if (status == 200){
				json = getStringBody(response);
			}
			return new BiomodelsQueryResult(query, status, json);
		}
		return null;
	}

	private static HttpResponse<InputStream> executeQuery(String query){
		try {
			URI uri = uriFromQuery(query);
			logger.info(uri.toString());
			HttpResponse<InputStream> ioResponse = Unirest.get(uri.toString())
														  .header("Accept", "text/json;charset=UTF-8")
														  .header("Content-Type", "text/json;charset=UTF-8")
														   .asBinary();
			return ioResponse;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static String getStringBody(HttpResponse<InputStream> ioResponse){
		InputStream inputStream = ioResponse.getRawBody();

		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		String content = bufferedReader.lines().collect(Collectors.joining("\n"));
		
		return content;
	}




//
//
//    /**
//     * Web service queries.
//     */
//    public List<String> getBioModelIdsByName(String name) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByName(name);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByPerson(String person) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByPerson(person);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByPublication(String publication) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByPublication(publication);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByTaxonomy(String taxonomy) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByTaxonomy(taxonomy);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByTaxonomyId(String taxonomyId) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByTaxonomyId(taxonomyId);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByChebi(String chebi) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByChEBI(chebi);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByChebiId(String chebiId) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByChEBIId(chebiId);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByUniprot(String uniprot) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByUniprot(uniprot);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public List<String> getBioModelIdsByUniprotId(String uniprotId) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] ids = null;
//        try {
//            ids = client.getModelsIdByUniprotId(uniprotId);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (ids == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(ids);
//    }
//
//    public String getBioModelNameById(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String name = "";
//        try {
//            name = client.getModelNameById(id);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        return name;
//    }
//
//    public String getDateLastModifiedByModelId(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String date = "";
//        try {
//            // Date expressed according to ISO 8601
//            date = client.getLastModifiedDateByModelId(id);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        return date;
//    }
//
//    public List<String> getAuthorsByModelId(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] authors = null;
//        try {
//            authors = client.getAuthorsByModelId(id);
//        } catch (BioModelsWSException e) {
//            e.printStackTrace();
//        }
//        if (authors == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(authors);
//    }
//
//    public List<String> getEncodersByModelId(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String[] encoders = null;
//        try {
//            encoders = client.getEncodersByModelId(id);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        if (encoders == null) {
//            return new LinkedList<String>();
//        }
//        return Arrays.asList(encoders);
//    }
//
//    public String getBioModelSBMLById(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        String sbml = "";
//        try {
//            sbml = client.getModelSBMLById(id);
//            if (sbml == null) {
//                sbml = "";
//            }
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        return sbml;
//    }
//
//    public SimpleModel getSimpleModelById(String id) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        SimpleModel model = null;
//        try {
//            model = client.getSimpleModelById(id);
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        return model;
//    }
//
//    public LinkedHashMap<String, SimpleModel> getSimpleModelsByIds(String[] ids) {
//        BioModelsWSClient client = createBioModelsWSClient();
//        LinkedHashMap<String, SimpleModel> simpleModels = null;
//        try {
//            List<SimpleModel> simpleModelsList = client.getSimpleModelsByIds(ids);
//            simpleModels = new LinkedHashMap<String, SimpleModel>();
//            for (int k = 0; k < simpleModelsList.size(); ++k) {
//                simpleModels.put(ids[k], simpleModelsList.get(k));
//            }
//        } catch (BioModelsWSException e) {
//            logger.error("BioModelsWSException", e);
//            e.printStackTrace();
//        }
//        return simpleModels;
//    }
//


    /* Test the Restful API. */
    public static void main(String[] args) throws URISyntaxException {

        BiomodelsQueryResult result = BiomodelsQuery.performQuery("/BIOMD0000000012?format=json");
        JSONObject json = result.getJSONObject();
        Biomodel model = new Biomodel(json);
        System.out.println(model);


        // Download the OMEX archive
        // https://www.ebi.ac.uk/biomodels/model/download/BIOMD0000000012

        // Download single model file
        // https://www.ebi.ac.uk/biomodels/model/download/BIOMD0000000012?filename=BIOMD0000000012_url.xml

        // Search for models
        // https://www.ebi.ac.uk/biomodels/search?query=repressilator&format=json


        // newQuery("searchKineticLaws/sbml?q=Tissue:spleen AND Organism:\"Homo sapiens\"");
        // newQuery("searchKineticLaws/sbml?q=Tissue:spleen%20AND%20Organism:%22homo%20sapiens%22");
    }


}
