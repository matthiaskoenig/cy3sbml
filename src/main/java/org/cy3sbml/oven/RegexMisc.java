package org.cy3sbml.oven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by mkoenig on 06.09.16.
 */
public class RegexMisc {


    public static void main(String[] args){
        String test = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:dcterms=\"http://purl.org/dc/terms/\" xmlns:jerm=\"http://www.mygrid.org.uk/ontology/JERMOntology#\" xmlns:sioc=\"http://rdfs.org/sioc/ns#\"> <jerm:Investigation rdf:about=\"http://fairdomhub.org/investigations/96\"> <dcterms:title>MM-PLF: Multiscale modeling for personalized liver function</dcterms:title> <dcterms:created rdf:datatype=\"http://www.w3.org/2001/XMLSchema#dateTime\">2016-08-16T14:07:01Z</dcterms:created> <dcterms:description/> <dcterms:modified rdf:datatype=\"http://www.w3.org/2001/XMLSchema#dateTime\">2016-08-16T14:07:01Z</dcterms:modified> <sioc:has_creator rdf:resource=\"http://fairdomhub.org/people/678\"/> <sioc:has_owner rdf:resource=\"http://fairdomhub.org/people/678\"/> <jerm:hasContributor rdf:resource=\"http://fairdomhub.org/people/678\"/> <jerm:hasCreator rdf:resource=\"http://fairdomhub.org/people/678\"/> <jerm:hasPart rdf:resource=\"http://fairdomhub.org/studies/170\"/> <jerm:itemProducedBy rdf:resource=\"http://fairdomhub.org/projects/46\"/> </jerm:Investigation> </rdf:RDF>\n" +
                "\n" +
                "{ \"id\": 96, \"uri\": \"http://fairdomhub.org/investigations/96\", \"title\": \"MM-PLF: Multiscale modeling for personalized liver function\", \"description\": \"\", \"contributor\": { \"name\": \"Matthias König\", \"uri\": \"http://fairdomhub.org/people/678\", \"orcid\": \"http://orcid.org/0000-0003-1725-179X\" }, \"creators\": [ { \"name\": \"Matthias König\", \"uri\": \"http://fairdomhub.org/people/678\", \"orcid\": \"http://orcid.org/0000-0003-1725-179X\" } ], \"studies\": [ \"studies/170-galactose-metabolism/\" ] }";

        //test = test.replaceAll("\"(http://.*?)\"", "<a href=\"$1\">$1</a>");
        test = test.replace("\"(http://.*?)\"", "");
        System.out.println(test);

        Pattern pattern = Pattern.compile("\"(http://.*?)\"");
        Matcher m = pattern.matcher(test);

    }
}
