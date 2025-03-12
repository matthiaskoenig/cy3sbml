package org.cy3sbml.oven;

import org.sbml.jsbml.*;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;


public class StringJSBMLReader {

    public static void main(String[] args) throws IOException, XMLStreamException {
        String sbmlString = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n" +
                "<sbml xmlns=\"http://www.sbml.org/sbml/level2\" level=\"2\" version=\"1\" \n" +
                "      xmlns:html=\"http://www.w3.org/1999/xhtml\">\n" +
                "    <model id=\"ehmn\">\n" +
                "        <listOfCompartments>\n" +
                "            <compartment id=\"Human\"/>\n" +
                "        </listOfCompartments>\n" +
                "        <listOfSpecies>\n" +
                "            <species id=\"C00001\" name=\"H2O\"/>\n" +
                "            <species id=\"C00002\" name=\"ATP\"/>\n" +
                "            <species id=\"C00003\" name=\"NAD+\"/>\n" +
                "            <species id=\"C00004\" name=\"NADH\"/>\n" +
                "        </listOfSpecies>\n" +
                "    </model>\n" +
                "</sbml>";
        SBMLDocument doc = JSBML.readSBMLFromString(sbmlString);
        System.out.println(doc);

        Model model = doc.getModel();
        for (Species s: model.getListOfSpecies()){
            System.out.println(s);
            System.out.println("id: " + s.getId() + ", name: " + s.getName());
        }
    }
}
