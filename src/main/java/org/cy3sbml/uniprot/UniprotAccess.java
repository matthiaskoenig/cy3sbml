package org.cy3sbml.uniprot;

import uk.ac.ebi.kraken.interfaces.uniprot.Gene;
import uk.ac.ebi.kraken.interfaces.uniprot.Organism;
import uk.ac.ebi.kraken.interfaces.uniprot.ProteinDescription;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.interfaces.uniprot.comments.*;
import uk.ac.ebi.kraken.interfaces.uniprot.description.Field;
import uk.ac.ebi.kraken.interfaces.uniprot.description.Name;
import uk.ac.ebi.uniprot.dataservice.client.Client;
import uk.ac.ebi.uniprot.dataservice.client.QueryResult;
import uk.ac.ebi.uniprot.dataservice.client.ServiceFactory;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtQueryBuilder;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtService;
import uk.ac.ebi.uniprot.dataservice.query.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.List;

/**
 * Access UniProt information.
 */
public class UniprotAccess {
    private static final Logger logger = LoggerFactory.getLogger(UniprotAccess.class);
    public static final String UNIPROT_URL = "https://www.uniprot.org/uniprot";

    /**
     * Retrieve UniProt Entry by accession id.
     *
     * @param accession UniProt accession id, e.g. "P10415"
     * @return
     */
    public static UniProtEntry getUniProtEntry(String accession){
        UniProtEntry entry = null;
        ServiceFactory serviceFactoryInstance = Client.getServiceFactoryInstance();
        UniProtService uniProtService = serviceFactoryInstance.getUniProtQueryService();
        try {
            // start the service


            // fetch entry
            entry = uniProtService.getEntry(accession);

            if (entry == null){
                // is secondary accession, get first result
                logger.debug("Querying any accession: " + accession);
                Query query = UniProtQueryBuilder.anyAccession(accession);
                QueryResult<UniProtEntry> result = uniProtService.getEntries(query);
                entry = result.next();
            }
            if (entry == null) {
                logger.warn("UniProt Entry " + accession + " could not be retrieved");
            } else {
                logger.debug("Retrieved UniProtEntry " + accession);
            }
        } catch (Exception e) {
            logger.error("Problems retrieving uniprot entry.", e);
            e.printStackTrace();
        } finally {
            // always remember to stop service

        }
        return entry;
    }


    /**
     * Creates additional information for entry.
     * Identifier of the form "P29218"
     */
    public static String uniprotHTML(String accession){
        String text = "\t<br />\n";
        // UniProtEntry entry = UniprotAccess.getUniProtEntry(accession);
        UniProtEntry entry = UniprotCache.getUniProtEntry(accession);
        if (entry != null) {
            String uniProtId = entry.getUniProtId().toString();
            text += MessageFormat.format(
                    "\t<a href=\"" + UNIPROT_URL + "\"><img src=\"./images/logos/uniprot_icon.png\" title=\"Information from UniProt\"/></a>&nbsp;&nbsp;\n" +
                            "\t<a href=\"" + UNIPROT_URL + "/{0}\"><span class=\"identifier\">{1}</span></a> ({2})<br />\n",
                    accession,
                    accession,
                    uniProtId
            );
            // description
            ProteinDescription description = entry.getProteinDescription();

            // Names (Full, Short, EC, AltName)
            Name name = description.getRecommendedName();
            List<Field> fields = name.getFields();
            for (Field field: fields){

                if (field.getType().getValue().equals("Full")){
                    text += MessageFormat.format(
                            "\t<b>{0}</b><br />\n",
                            field.getValue()
                    );
                }else {
                    text += MessageFormat.format(
                            "\t<b>{0}</b>: {1}<br />\n",
                            field.getType().getValue(),
                            field.getValue()
                    );
                }
            }

            // organism
            Organism organism = entry.getOrganism();
            String organismStr = organism.getScientificName().toString();
            if (organism.hasCommonName()){
                organismStr += MessageFormat.format(" ({0})", organism.getCommonName());

            }
            text += MessageFormat.format(
                    "\t<b>Organism</b>: {0}<br />\n",
                    organismStr);

            // genes
            for (Gene gene : entry.getGenes()){
                String geneName = gene.getGeneName().getValue();
                text += MessageFormat.format("\t<b>Gene</b>: {0}<br />\n", geneName);

            }

            // alternative names
            text +="\t<span class=\"comment\">Synonyms</span>";
            for (Name n: description.getAlternativeNames()){
                text += MessageFormat.format(
                        "{0}; ",
                        n.getFields().get(0).getValue()
                );
            }
            text += "<br />\n";

            // comments
            for (Comment comment : entry.getComments()){
                CommentType ctype = comment.getCommentType();

                if (ctype.equals(CommentType.FUNCTION)){
                    FunctionComment fComment = (FunctionComment) comment;
                    for (CommentText commentText : fComment.getTexts()) {
                        text += MessageFormat.format(
                                "\t<span class=\"comment\">Function</span> <span class=\"text-success\">{0}</span><br />\n",
                                commentText.getValue()
                        );                    }
                }
                else if (ctype.equals(CommentType.CATALYTIC_ACTIVITY)) {
                    CatalyticActivityCommentStructured caComment = (CatalyticActivityCommentStructured) comment;
                    Reaction reaction = caComment.getReaction();
                    if (reaction != null){
                        text += MessageFormat.format(
                                "\t<span class=\"comment\">Catalytic Activity</span>{0}<br />\n",
                                reaction.getName()
                        );                    }
                }
                else if (ctype.equals(CommentType.PATHWAY)) {
                    PathwayComment pComment = (PathwayComment) comment;
                    for (CommentText commentText : pComment.getTexts()) {
                        text += MessageFormat.format(
                                "\t<span class=\"comment\">Pathway</span>{0}<br />\n",
                                commentText.getValue()
                        );
                    }
                }
            }
        }

        return text;
    }



}
