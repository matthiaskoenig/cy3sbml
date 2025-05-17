package org.cy3sbml.oven;

import org.slf4j.LoggerFactory;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.comments.*;
import uk.ac.ebi.uniprot.dataservice.client.Client;
import uk.ac.ebi.uniprot.dataservice.client.QueryResult;
import uk.ac.ebi.uniprot.dataservice.client.ServiceFactory;
import uk.ac.ebi.uniprot.dataservice.client.alignment.blast.BlastService;
import uk.ac.ebi.uniprot.dataservice.client.alignment.blast.UniParcBlastService;
import uk.ac.ebi.uniprot.dataservice.client.alignment.blast.UniProtBlastService;
import uk.ac.ebi.uniprot.dataservice.client.alignment.blast.UniRefBlastService;
import uk.ac.ebi.uniprot.dataservice.client.exception.ServiceException;
import uk.ac.ebi.uniprot.dataservice.client.uniparc.UniParcService;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtQueryBuilder;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtService;
import uk.ac.ebi.uniprot.dataservice.client.uniref.UniRefService;
import uk.ac.ebi.uniprot.dataservice.query.Query;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class UniprotTest {
    
    public static void main(String[] args) throws ServiceException {

        ServiceFactory serviceFactoryInstance = Client.getServiceFactoryInstance();
        UniProtService uniProtService = serviceFactoryInstance.getUniProtQueryService();
        System.out.println(uniProtService.toString());
//        String url = "https://www.uniprot.org/uniprotkb/P04483.xml";
//        String result2 = null;
//        try {
//            result2 = sendRestRequest(url).toString();
//            System.out.println(result2);
//        } catch (Exception e) {
//            System.out.println(e);
//        }

        // fetch entry
        UniProtEntry entry = uniProtService.getEntry("P12345");
        if (entry == null) {
            // is secondary accession, get first result
            System.out.println("Querying any accession: ");
            Query query = UniProtQueryBuilder.anyAccession("q9uj37");
            QueryResult<UniProtEntry> result = uniProtService.getEntries(query);
            entry = result.next();
        }
        if (entry == null) {
            System.out.println("UniProt Entry could not be retrieved");
        } else {
            System.out.println("Retrieved UniProtEntry ");
        }


        // comments
        String text = "";
        for (Comment comment : entry.getComments()){
            CommentType ctype = comment.getCommentType();
            if (ctype.equals(CommentType.FUNCTION)){
                FunctionComment fComment = (FunctionComment) comment;
                for (CommentText commentText : fComment.getTexts()) {
                    text += String.format("\t<span class=\"comment\">Function</span> <span class=\"text-success\">%s</span><br />\n", commentText.getValue());
                }
            }
            else if (ctype.equals(CommentType.CATALYTIC_ACTIVITY)) {
                CatalyticActivityCommentStructured caComment = (CatalyticActivityCommentStructured) comment;
                Reaction reaction = caComment.getReaction();
                if (reaction != null){
                    text += String.format("\t<span class=\"comment\">Catalytic Activity</span>%s<br />\n", reaction.getName());
                }

            }
            else if (ctype.equals(CommentType.PATHWAY)) {
                PathwayComment pComment = (PathwayComment) comment;
                for (CommentText commentText : pComment.getTexts()) {
                    text += String.format("\t<span class=\"comment\">Pathway</span>%s<br />\n", commentText.getValue());
                }
            }
        }
        System.out.println(text);
    }
    public static StringBuffer sendRestRequest(String url) throws Exception {

        try {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");
            int responseCode = con.getResponseCode();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine + "\n");
            }
            in.close();

            //print result
            return response;
        } catch (IOException e) {
            throw new Exception("Error while sending request: " + e.getMessage());
        }


    }
}
