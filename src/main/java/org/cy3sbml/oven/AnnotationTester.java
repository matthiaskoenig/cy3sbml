package org.cy3sbml.oven;


import org.sbml.jsbml.*;
import org.sbml.jsbml.util.StringTools;
import org.sbml.jsbml.xml.XMLNode;
import org.sbml.jsbml.xml.parsers.SBMLRDFAnnotationParser;

import javax.xml.stream.XMLStreamException;
import java.util.List;

public class AnnotationTester {
    /**
     *
     */
    public static void testParseAnnotation() {

        String xmlAnnotationStr = "<annotation> "
                + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
                + "         xmlns:bqmodel=\"http://biomodels.net/model-qualifiers/\""
                + "         xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">"
                + "  <rdf:Description rdf:about=\"#glyph1\">"
                + "    <bqbiol:is> "
                + "      <rdf:Bag> "
                + "        <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P00561\" />"
                + "      </rdf:Bag> "
                + "    </bqbiol:is> "
                + "    <bqmodel:isDescribedBy>"
                + "       <rdf:Bag> "
                + "         <rdf:li rdf:resource=\"http://identifiers.org/pubmed/21988831\" />"
                + "       </rdf:Bag> "
                + "     </bqmodel:isDescribedBy>"
                + "   </rdf:Description> "
                + "</rdf:RDF> "
                + "</annotation>";

        try {
            XMLNode xmlNode = XMLNode.convertStringToXMLNode(StringTools.toXMLAnnotationString(xmlAnnotationStr));
            Model model = new Model(3, 1);
            model.setAnnotation(xmlNode);

            SBMLRDFAnnotationParser annotationParser = new SBMLRDFAnnotationParser();

            annotationParser.processAnnotation(model);

            // Now you have access to the CVTerm objects through the Model annotation object
            Annotation annotation = model.getAnnotation();

            System.out.println("Number of CVterm = " + annotation.getCVTermCount());
            System.out.println("First URI of the first CVTerm = " + annotation.getCVTerm(0).getResourceURI(0));

            // adding another URI to the first CVTerm
            annotation.getCVTerm(0).addResourceURI("http://identifiers.org/go/GO:1234567");

            // adding a new CVTerm with two URI
            annotation.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_PART_OF, "http://identifiers.org/uniprot/P00561", "http://identifiers.org/uniprot/P00562"));

            // Getting back the full XML String
            System.out.println(annotation.getFullAnnotationString());

        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }


    /**
     *
     */
    public static void testParseAnnotationSBGNBase() {

        String xmlAnnotationStr = "<annotation> "
                + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
                + "         xmlns:bqmodel=\"http://biomodels.net/model-qualifiers/\""
                + "         xmlns:bqbiol=\"http://biomodels.net/biology-qualifiers/\">"
                + "  <rdf:Description rdf:about=\"#glyph1\">"
                + "    <bqbiol:is> "
                + "      <rdf:Bag> "
                + "        <rdf:li rdf:resource=\"http://identifiers.org/uniprot/P00561\" />"
                + "      </rdf:Bag> "
                + "    </bqbiol:is> "
                + "    <bqmodel:isDescribedBy>"
                + "       <rdf:Bag> "
                + "         <rdf:li rdf:resource=\"http://identifiers.org/pubmed/21988831\" />"
                + "       </rdf:Bag> "
                + "     </bqmodel:isDescribedBy>"
                + "   </rdf:Description> "
                + "</rdf:RDF> "
                + "</annotation>";

        try {
            // XMLNode xmlNode = XMLNode.convertStringToXMLNode(StringTools.toXMLAnnotationString(xmlAnnotationStr));
            XMLNode xmlNode = XMLNode.convertStringToXMLNode(xmlAnnotationStr);

            SBMLRDFAnnotationParser annotationParser = new SBMLRDFAnnotationParser();

            SBGNBase sbgnBase = new SBGNBase();
            sbgnBase.setAnnotation(xmlNode);

            sbgnBase.setId("glyph1");
            sbgnBase.setName("glyph1 name");
            sbgnBase.setRole("yellow species");

            annotationParser.processAnnotation(sbgnBase);

            // non RDF annotation
            Annotation annotation = sbgnBase.getAnnotation();

            System.out.println("id = " + sbgnBase.getId());
            System.out.println("name = " + sbgnBase.getName());
            System.out.println("role = " + sbgnBase.getRole());

            System.out.println("Number of CVterm = " + annotation.getCVTermCount());
            System.out.println("First URI of the first CVTerm = " + annotation.getCVTerm(0).getResourceURI(0));

            // adding another URI to the first CVTerm
            annotation.getCVTerm(0).addResourceURI("http://identifiers.org/go/GO:1234567");

            // adding a new CVTerm with two URI
            annotation.addCVTerm(new CVTerm(CVTerm.Qualifier.BQB_IS_PART_OF, "http://identifiers.org/uniprot/P00561", "http://identifiers.org/uniprot/P00562"));

            // Getting back the full XML String
            System.out.println(annotation.getFullAnnotationString());

        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args){
        testParseAnnotation();
        System.out.println("-----------------------------");
        testParseAnnotationSBGNBase();
    }

}
