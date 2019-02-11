package org.cy3sbml.oven;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.comments.*;
import uk.ac.ebi.uniprot.dataservice.client.Client;
import uk.ac.ebi.uniprot.dataservice.client.ServiceFactory;
import uk.ac.ebi.uniprot.dataservice.client.exception.ServiceException;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtService;

public class UniprotTest {

    public static void main(String[] args) throws ServiceException {
        ServiceFactory serviceFactoryInstance = Client.getServiceFactoryInstance();
        UniProtService uniProtService = serviceFactoryInstance.getUniProtQueryService();
        UniProtEntry entry = null;

        // start the service
        uniProtService.start();

        // fetch entry
        entry = uniProtService.getEntry("P19367");

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
}
